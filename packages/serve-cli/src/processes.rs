use anyhow::{Context, Result};
use serde_json::{json, Value};
use std::collections::VecDeque;
use std::fmt;
use std::path::{Path, PathBuf};
use std::process::Stdio;
use std::sync::Arc;
use std::{env, ffi::OsStr};
use tokio::fs;
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::process::{Child, Command as TokioCommand};
use tokio::sync::Mutex;
use tracing::info;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum ProcessType {
    Gradle,
    DockerCompose,
    Pnpm,
}

impl fmt::Display for ProcessType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ProcessType::Gradle => write!(f, "gradle"),
            ProcessType::DockerCompose => write!(f, "docker"),
            ProcessType::Pnpm => write!(f, "pnpm"),
        }
    }
}

/// Manages PID files for process cleanup
struct PidManager {
    pids_dir: PathBuf,
}

impl PidManager {
    async fn new(working_dir: &Path) -> Result<Self> {
        let pids_dir = working_dir.join("tmp").join("pids");
        fs::create_dir_all(&pids_dir).await?;
        Ok(Self { pids_dir })
    }

    async fn store_pid(&self, name: &str, pid: u32) -> Result<()> {
        let pid_file = self.pids_dir.join(format!("{}.pid", name));
        fs::write(pid_file, pid.to_string()).await?;
        Ok(())
    }

    async fn cleanup_previous_pids(&self) -> Result<()> {
        if !fs::metadata(&self.pids_dir).await.is_ok() {
            return Ok(());
        }

        let mut entries = fs::read_dir(&self.pids_dir).await?;
        while let Some(entry) = entries.next_entry().await? {
            let path = entry.path();
            if path.extension().map_or(false, |ext| ext == "pid") {
                if let Ok(content) = fs::read_to_string(&path).await {
                    if let Ok(pid) = content.trim().parse::<i32>() {
                        // Kill process group if possible, or just the PID
                        #[cfg(unix)]
                        let _ = std::process::Command::new("kill")
                            .args(["-9", &pid.to_string()])
                            .output();
                        info!("✓ Cleaned up stale PID {}", pid);
                    }
                }
                let _ = fs::remove_file(&path).await;
            }
        }
        Ok(())
    }

    async fn remove_pid(&self, name: &str) -> Result<()> {
        let pid_file = self.pids_dir.join(format!("{}.pid", name));
        if fs::metadata(&pid_file).await.is_ok() {
            let _ = fs::remove_file(pid_file).await;
        }
        Ok(())
    }
}

pub struct Process {
    pub process_type: ProcessType,
    pub child: Option<Child>,
    pub logs: VecDeque<String>,
    pub is_running: bool,
    pub docker_logs_tailer_pid: Option<u32>,
}

pub struct ProcessManager {
    pub working_dir: PathBuf,
    pub processes: Arc<Mutex<Vec<Process>>>,
    pid_manager: PidManager,
    toolchain: Toolchain,
}

#[derive(Clone, Debug)]
struct Toolchain {
    env_path: Option<String>,
    pnpm_cmd: PathBuf,
}

impl Toolchain {
    fn new(node_path: Option<PathBuf>) -> Self {
        let node_dir = node_path.as_ref().and_then(|p| p.parent()).map(Path::to_path_buf);
        let pnpm_cmd = Self::resolve_tool(&node_dir, "pnpm");

        let env_path = node_dir.as_ref().map(|dir| {
            let existing = env::var_os("PATH").unwrap_or_default();
            let mut new_path = dir.as_os_str().to_os_string();
            if !existing.is_empty() {
                let sep = if cfg!(windows) { ";" } else { ":" };
                new_path.push(OsStr::new(sep));
                new_path.push(existing);
            }
            new_path.to_string_lossy().to_string()
        });

        Self { env_path, pnpm_cmd }
    }

    fn resolve_tool(node_dir: &Option<PathBuf>, name: &str) -> PathBuf {
        if let Some(dir) = node_dir {
            let candidate = dir.join(name);
            if candidate.exists() { return candidate; }
        }
        PathBuf::from(name)
    }

    fn apply_env(&self, cmd: &mut TokioCommand) {
        if let Some(env_path) = &self.env_path {
            cmd.env("PATH", env_path);
        }
    }
}

impl ProcessManager {
    pub async fn new(working_dir: PathBuf, node_path: Option<PathBuf>) -> Result<Self> {
        let pid_manager = PidManager::new(&working_dir).await?;
        let toolchain = Toolchain::new(node_path);

        let processes = vec![
            ProcessType::Gradle,
            ProcessType::DockerCompose,
            ProcessType::Pnpm,
        ]
            .into_iter()
            .map(|pt| Process {
                process_type: pt,
                child: None,
                logs: VecDeque::with_capacity(1001),
                is_running: false,
                docker_logs_tailer_pid: None,
            })
            .collect();

        Ok(Self {
            working_dir,
            processes: Arc::new(Mutex::new(processes)),
            pid_manager,
            toolchain,
        })
    }

    pub async fn start_process(&self, process_type: ProcessType) -> Result<()> {
        let mut processes = self.processes.lock().await;
        let proc = processes
            .iter_mut()
            .find(|p| p.process_type == process_type)
            .context("Process not found")?;

        if proc.is_running {
            return Ok(());
        }

        // Pnpm has a unique "Install then Dev" workflow
        if process_type == ProcessType::Pnpm {
            return self.start_pnpm_workflow(proc).await;
        }

        // Standard commands
        let mut cmd = match process_type {
            ProcessType::Gradle => {
                let mut c = TokioCommand::new("./gradlew");
                c.args(["build", "--console=plain"]);
                c
            }
            ProcessType::DockerCompose => {
                let mut c = TokioCommand::new("docker");
                c.args(["compose", "up", "--detach"]);
                c
            }
            _ => unreachable!(),
        };

        self.setup_and_spawn(proc, &mut cmd, process_type).await
    }

    async fn start_pnpm_workflow(&self, proc: &mut Process) -> Result<()> {
        proc.logs.clear();
        proc.logs.push_back("📦 Initializing pnpm workflow...".to_string());
        proc.is_running = true;

        let processes = self.processes.clone();
        let working_dir = self.working_dir.clone();
        let toolchain = self.toolchain.clone();
        let pid_manager_dir = self.pid_manager.pids_dir.clone();

        tokio::spawn(async move {
            // 1. Install
            let mut install_cmd = TokioCommand::new(&toolchain.pnpm_cmd);
            install_cmd.arg("i").current_dir(&working_dir);
            toolchain.apply_env(&mut install_cmd);

            if let Err(e) = Self::run_to_completion(processes.clone(), ProcessType::Pnpm, install_cmd, "📦 pnpm install").await {
                Self::log_to_proc(&processes, ProcessType::Pnpm, format!("❌ Install failed: {}", e)).await;
                Self::set_running_state(&processes, ProcessType::Pnpm, false).await;
                return;
            }

            // 2. Dev
            let mut dev_cmd = TokioCommand::new(&toolchain.pnpm_cmd);
            dev_cmd.arg("dev").current_dir(&working_dir);
            toolchain.apply_env(&mut dev_cmd);

            match Self::spawn_and_watch(processes.clone(), ProcessType::Pnpm, dev_cmd, pid_manager_dir).await {
                Ok(_) => info!("pnpm dev started successfully"),
                Err(e) => {
                    Self::log_to_proc(&processes, ProcessType::Pnpm, format!("❌ Dev failed: {}", e)).await;
                    Self::set_running_state(&processes, ProcessType::Pnpm, false).await;
                }
            }
        });

        Ok(())
    }

    async fn setup_and_spawn(&self, proc: &mut Process, cmd: &mut TokioCommand, pt: ProcessType) -> Result<()> {
        cmd.current_dir(&self.working_dir)
            .stdout(Stdio::piped())
            .stderr(Stdio::piped());
        self.toolchain.apply_env(cmd);

        let mut child = cmd.spawn().with_context(|| format!("Failed to spawn {}", pt))?;
        let pid = child.id().context("No PID")?;

        self.pid_manager.store_pid(&pt.to_string(), pid).await?;

        let stdout = child.stdout.take().unwrap();
        let stderr = child.stderr.take().unwrap();

        proc.child = Some(child);
        proc.is_running = true;
        proc.logs.clear();
        proc.logs.push_back(format!("🚀 Started {} (PID: {})", pt, pid));

        Self::spawn_log_reader_for(self.processes.clone(), pt, stdout, None);
        Self::spawn_log_reader_for(self.processes.clone(), pt, stderr, Some("[STDERR] ".to_string()));

        if pt == ProcessType::DockerCompose {
            self.spawn_docker_logs_tailer().await;
        }

        self.spawn_exit_watcher(pt);
        Ok(())
    }

    /// Runs a command fully and returns result (used for pnpm install)
    async fn run_to_completion(processes: Arc<Mutex<Vec<Process>>>, pt: ProcessType, mut cmd: TokioCommand, label: &str) -> Result<()> {
        cmd.stdout(Stdio::piped()).stderr(Stdio::piped());
        let mut child = cmd.spawn()?;
        let pid = child.id().unwrap_or(0);

        Self::log_to_proc(&processes, pt, format!("{} (PID: {})", label, pid)).await;

        if let Some(out) = child.stdout.take() { Self::spawn_log_reader_for(processes.clone(), pt, out, None); }
        if let Some(err) = child.stderr.take() { Self::spawn_log_reader_for(processes.clone(), pt, err, None); }

        let status = child.wait().await?;
        if status.success() {
            Self::log_to_proc(&processes, pt, format!("✅ {} completed", label)).await;
            Ok(())
        } else {
            Err(anyhow::anyhow!("Process exited with status {}", status))
        }
    }

    /// Spawns a long-running command and sets it in the manager (used for pnpm dev)
    async fn spawn_and_watch(processes: Arc<Mutex<Vec<Process>>>, pt: ProcessType, mut cmd: TokioCommand, pids_dir: PathBuf) -> Result<()> {
        cmd.stdout(Stdio::piped()).stderr(Stdio::piped());
        let mut child = cmd.spawn()?;
        let pid = child.id().context("No PID")?;

        let _ = fs::write(pids_dir.join(format!("{}.pid", pt)), pid.to_string()).await;

        let stdout = child.stdout.take().unwrap();
        let stderr = child.stderr.take().unwrap();

        {
            let mut procs = processes.lock().await;
            if let Some(p) = procs.iter_mut().find(|p| p.process_type == pt) {
                p.child = Some(child);
                p.logs.push_back(format!("🚀 Started dev server (PID: {})", pid));
            }
        }

        Self::spawn_log_reader_for(processes.clone(), pt, stdout, None);
        Self::spawn_log_reader_for(processes.clone(), pt, stderr, Some("[STDERR] ".to_string()));

        Self::spawn_exit_watcher_static(processes, pids_dir, pt);
        Ok(())
    }

    fn spawn_log_reader_for<R: tokio::io::AsyncRead + Unpin + Send + 'static>(
        processes: Arc<Mutex<Vec<Process>>>,
        pt: ProcessType,
        reader: R,
        prefix: Option<String>,
    ) {
        tokio::spawn(async move {
            let mut lines = BufReader::new(reader).lines();
            while let Ok(Some(line)) = lines.next_line().await {
                let mut procs = processes.lock().await;
                if let Some(p) = procs.iter_mut().find(|p| p.process_type == pt) {
                    let formatted = prefix.as_ref().map(|pre| format!("{}{}", pre, line)).unwrap_or(line);
                    p.logs.push_back(formatted);
                    if p.logs.len() > 1000 { p.logs.pop_front(); }
                }
            }
        });
    }

    async fn spawn_docker_logs_tailer(&self) {
        let processes = self.processes.clone();
        let working_dir = self.working_dir.clone();

        tokio::spawn(async move {
            tokio::time::sleep(tokio::time::Duration::from_millis(800)).await;

            let mut cmd = TokioCommand::new("docker");
            cmd.args(["compose", "logs", "-f", "--no-color", "--tail", "50"])
                .current_dir(&working_dir)
                .stdout(Stdio::piped())
                .stderr(Stdio::piped());

            if let Ok(mut child) = cmd.spawn() {
                if let Some(pid) = child.id() {
                    let mut procs = processes.lock().await;
                    if let Some(p) = procs.iter_mut().find(|p| p.process_type == ProcessType::DockerCompose) {
                        p.docker_logs_tailer_pid = Some(pid);
                        p.logs.push_back(format!("📡 Tailing docker logs (PID: {})", pid));
                    }
                }

                if let Some(out) = child.stdout.take() {
                    Self::spawn_log_reader_for(processes.clone(), ProcessType::DockerCompose, out, Some("[DOCKER] ".to_string()));
                }
                let _ = child.wait().await;
            }
        });
    }

    fn spawn_exit_watcher(&self, pt: ProcessType) {
        Self::spawn_exit_watcher_static(self.processes.clone(), self.pid_manager.pids_dir.clone(), pt);
    }

    fn spawn_exit_watcher_static(processes: Arc<Mutex<Vec<Process>>>, pids_dir: PathBuf, pt: ProcessType) {
        tokio::spawn(async move {
            let mut child = loop {
                tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;
                let mut procs = processes.lock().await;
                if let Some(p) = procs.iter_mut().find(|p| p.process_type == pt) {
                    if let Some(child) = p.child.take() {
                        break child;
                    }
                    if !p.is_running { return; } // Stopped externally
                } else { return; }
            };

            let status = child.wait().await;
            let mut procs = processes.lock().await;
            if let Some(p) = procs.iter_mut().find(|p| p.process_type == pt) {
                p.is_running = false;
                let msg = match status {
                    Ok(s) => format!("✅ Process {} exited with status {}", pt, s),
                    Err(e) => format!("⚠️ Process {} errored: {}", pt, e),
                };
                p.logs.push_back(msg);
            }
            let _ = fs::remove_file(pids_dir.join(format!("{}.pid", pt))).await;
        });
    }

    // Helper: Log to a specific process entry
    async fn log_to_proc(processes: &Arc<Mutex<Vec<Process>>>, pt: ProcessType, msg: String) {
        let mut procs = processes.lock().await;
        if let Some(p) = procs.iter_mut().find(|p| p.process_type == pt) {
            p.logs.push_back(msg);
        }
    }

    // Helper: Set running state
    async fn set_running_state(processes: &Arc<Mutex<Vec<Process>>>, pt: ProcessType, state: bool) {
        let mut procs = processes.lock().await;
        if let Some(p) = procs.iter_mut().find(|p| p.process_type == pt) {
            p.is_running = state;
        }
    }

    pub async fn stop_process(&self, pt: ProcessType) -> Result<()> {
        let mut processes = self.processes.lock().await;
        if let Some(proc) = processes.iter_mut().find(|p| p.process_type == pt) {
            if let Some(mut child) = proc.child.take() {
                let _ = child.kill().await;
            }

            if pt == ProcessType::DockerCompose {
                if let Some(pid) = proc.docker_logs_tailer_pid.take() {
                    #[cfg(unix)]
                    let _ = std::process::Command::new("kill").args(["-9", &pid.to_string()]).output();
                }
                // Also run docker compose down if needed
                let _ = TokioCommand::new("docker")
                    .args(["compose", "stop"])
                    .current_dir(&self.working_dir)
                    .output()
                    .await;
            }

            proc.is_running = false;
            self.pid_manager.remove_pid(&pt.to_string()).await?;
            proc.logs.push_back(format!("🛑 Stopped {}", pt));
        }
        Ok(())
    }

    pub async fn restart_process(&self, pt: ProcessType) -> Result<()> {
        self.stop_process(pt).await?;
        tokio::time::sleep(tokio::time::Duration::from_millis(300)).await;
        self.start_process(pt).await?;
        Ok(())
    }

    pub async fn wait_for_completion(&self, pt: ProcessType) {
        loop {
            let is_running = {
                let processes = self.processes.lock().await;
                processes
                    .iter()
                    .find(|p| p.process_type == pt)
                    .map(|p| p.is_running)
                    .unwrap_or(false)
            };

            if !is_running {
                break;
            }

            tokio::time::sleep(tokio::time::Duration::from_millis(200)).await;
        }
    }

    pub async fn get_status(&self) -> Value {
        let processes = self.processes.lock().await;
        let status_map: serde_json::Map<String, Value> = processes.iter().map(|p| {
            (p.process_type.to_string(), json!({
                "running": p.is_running,
                "log_count": p.logs.len()
            }))
        }).collect();

        json!({ "processes": status_map })
    }

    pub async fn get_logs(&self, pt: ProcessType) -> Vec<String> {
        let processes = self.processes.lock().await;
        processes
            .iter()
            .find(|p| p.process_type == pt)
            .map(|p| p.logs.iter().cloned().collect())
            .unwrap_or_default()
    }

    pub async fn cleanup_previous_pids(&self) -> Result<()> {
        self.pid_manager.cleanup_previous_pids().await
    }
}
