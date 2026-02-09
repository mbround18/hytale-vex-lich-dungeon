use anyhow::{Context, Result};
use serde_json::{json, Value};
use std::collections::VecDeque;
use std::fmt;
use std::path::{Path, PathBuf};
use std::process::Stdio;
use std::sync::Arc;
use tokio::fs;
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::process::{Child, Command as TokioCommand};
use tokio::sync::Mutex;

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
                        // Use libc or a crate like `sysinfo` for cross-platform,
                        // but sticking to your logic:
                        let _ = std::process::Command::new("kill")
                            .arg("-9")
                            .arg(pid.to_string())
                            .output();
                        println!("  ✓ Cleaned up stale PID {}", pid);
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
            fs::remove_file(pid_file).await?;
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
}

impl ProcessManager {
    pub async fn new(working_dir: PathBuf) -> Result<Self> {
        let pid_manager = PidManager::new(&working_dir).await?;

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
        })
    }

    pub async fn start_process(&self, process_type: ProcessType) -> Result<()> {
        let mut processes = self.processes.lock().await;
        let proc = processes
            .iter_mut()
            .find(|p| p.process_type == process_type)
            .context("Process not found in manager")?;

        if proc.is_running {
            return Ok(());
        }

        // Prepare commands
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
            ProcessType::Pnpm => {
                let mut c = TokioCommand::new("sh");
                c.args(["-c", "pnpm i && pnpm dev"]);
                c
            }
        };

        cmd.current_dir(&self.working_dir)
            .stdout(Stdio::piped())
            .stderr(Stdio::piped());

        let mut child = cmd
            .spawn()
            .with_context(|| format!("Failed to spawn {}", process_type))?;
        let pid = child.id().context("Failed to get child PID")?;

        // Persist PID
        self.pid_manager
            .store_pid(&process_type.to_string(), pid)
            .await?;

        // Handle I/O
        let stdout = child.stdout.take().unwrap();
        let stderr = child.stderr.take().unwrap();

        proc.child = Some(child);
        proc.is_running = true;
        proc.logs.clear();
        proc.logs
            .push_back(format!("🚀 Started {} (PID: {})", process_type, pid));

        // Spawn log readers and exit watcher
        self.spawn_log_reader(process_type, stdout).await;
        self.spawn_log_reader(process_type, stderr).await;

        // Special handling for DockerCompose: after it starts, spawn logs tailer
        if process_type == ProcessType::DockerCompose {
            self.spawn_docker_logs_tailer().await;
        }

        self.spawn_exit_watcher(process_type).await;

        Ok(())
    }

    async fn spawn_log_reader<R: tokio::io::AsyncRead + Unpin + Send + 'static>(
        &self,
        process_type: ProcessType,
        reader: R,
    ) {
        let processes = self.processes.clone();
        tokio::spawn(async move {
            let mut lines = BufReader::new(reader).lines();
            while let Ok(Some(line)) = lines.next_line().await {
                let mut procs = processes.lock().await;
                if let Some(p) = procs.iter_mut().find(|p| p.process_type == process_type) {
                    p.logs.push_back(line);
                    if p.logs.len() > 1000 {
                        p.logs.pop_front();
                    }
                }
            }
        });
    }

    async fn spawn_docker_logs_tailer(&self) {
        let processes = self.processes.clone();
        let working_dir = self.working_dir.clone();

        tokio::spawn(async move {
            // Wait a bit for container to be ready
            tokio::time::sleep(tokio::time::Duration::from_millis(500)).await;

            // Dynamically get the container name
            let container_name = match TokioCommand::new("docker")
                .args(["compose", "ps", "-q"])
                .current_dir(&working_dir)
                .output()
                .await
            {
                Ok(output) if !output.stdout.is_empty() => {
                    String::from_utf8_lossy(&output.stdout).trim().to_string()
                }
                _ => {
                    let mut procs = processes.lock().await;
                    if let Some(p) = procs
                        .iter_mut()
                        .find(|p| p.process_type == ProcessType::DockerCompose)
                    {
                        p.logs.push_back("⚠️ Failed to detect container ID for logs".to_string());
                    }
                    return;
                }
            };

            // Start docker logs -f on the detected container
            let mut cmd = TokioCommand::new("docker");
            cmd.args(["logs", "-f", &container_name])
                .current_dir(&working_dir)
                .stdout(Stdio::piped())
                .stderr(Stdio::piped());

            match cmd.spawn() {
                Ok(mut child) => {
                    // Store the tailer PID so we can kill it on restart
                    if let Some(pid) = child.id() {
                        let mut procs = processes.lock().await;
                        if let Some(p) = procs
                            .iter_mut()
                            .find(|p| p.process_type == ProcessType::DockerCompose)
                        {
                            p.docker_logs_tailer_pid = Some(pid);
                            p.logs.push_back(format!("📡 Docker logs tailer started (PID: {})", pid));
                        }
                    }

                    let stdout = child.stdout.take();
                    let stderr = child.stderr.take();

                    if let Some(reader) = stdout {
                        let procs = processes.clone();
                        tokio::spawn(async move {
                            let mut lines = BufReader::new(reader).lines();
                            while let Ok(Some(line)) = lines.next_line().await {
                                let mut procs = procs.lock().await;
                                if let Some(p) = procs
                                    .iter_mut()
                                    .find(|p| p.process_type == ProcessType::DockerCompose)
                                {
                                    p.logs.push_back(line);
                                    if p.logs.len() > 1000 {
                                        p.logs.pop_front();
                                    }
                                }
                            }
                        });
                    }

                    if let Some(reader) = stderr {
                        let procs = processes.clone();
                        tokio::spawn(async move {
                            let mut lines = BufReader::new(reader).lines();
                            while let Ok(Some(line)) = lines.next_line().await {
                                let mut procs = procs.lock().await;
                                if let Some(p) = procs
                                    .iter_mut()
                                    .find(|p| p.process_type == ProcessType::DockerCompose)
                                {
                                    p.logs.push_back(format!("[STDERR] {}", line));
                                    if p.logs.len() > 1000 {
                                        p.logs.pop_front();
                                    }
                                }
                            }
                        });
                    }

                    // Spawn an exit watcher just for logs (optional, for cleanup)
                    tokio::spawn(async move {
                        let _ = child.wait().await;
                    });
                }
                Err(e) => {
                    let mut procs = processes.lock().await;
                    if let Some(p) = procs
                        .iter_mut()
                        .find(|p| p.process_type == ProcessType::DockerCompose)
                    {
                        p.logs
                            .push_back(format!("⚠️ Failed to start logs tailer: {}", e));
                    }
                }
            }
        });
    }

    async fn spawn_exit_watcher(&self, process_type: ProcessType) {
        let processes = self.processes.clone();
        let pid_manager_dir = self.pid_manager.pids_dir.clone();

        tokio::spawn(async move {
            // Extract the child from the process struct so we can wait on it
            let mut child_opt = {
                let mut procs = processes.lock().await;
                procs
                    .iter_mut()
                    .find(|p| p.process_type == process_type)
                    .and_then(|p| p.child.take())
            };

            if let Some(child) = child_opt.as_mut() {
                let pid = child.id();
                
                // Wait for the child process to actually exit
                match child.wait().await {
                    Ok(status) => {
                        let mut procs = processes.lock().await;
                        if let Some(p) = procs.iter_mut().find(|p| p.process_type == process_type) {
                            p.is_running = false;
                            if status.success() {
                                p.logs.push_back(format!("✅ Process {} completed successfully", process_type));
                            } else {
                                p.logs.push_back(format!(
                                    "❌ Process {} exited with status: {}",
                                    process_type,
                                    status.code().map(|c| c.to_string()).unwrap_or_else(|| "unknown".to_string())
                                ));
                            }
                        }
                    }
                    Err(e) => {
                        let mut procs = processes.lock().await;
                        if let Some(p) = procs.iter_mut().find(|p| p.process_type == process_type) {
                            p.is_running = false;
                            p.logs.push_back(format!("⚠️ Error waiting for {} to exit: {}", process_type, e));
                        }
                    }
                }
                
                // Cleanup PID file
                if let Some(_pid) = pid {
                    let _ = fs::remove_file(pid_manager_dir.join(format!("{}.pid", process_type))).await;
                }
            }
        });
    }

    pub async fn stop_process(&self, process_type: ProcessType) -> Result<()> {
        let mut processes = self.processes.lock().await;
        if let Some(proc) = processes
            .iter_mut()
            .find(|p| p.process_type == process_type)
        {
            // Kill the main process
            if let Some(mut child) = proc.child.take() {
                let _ = child.kill().await;
            }
            
            // Kill the docker logs tailer if it exists
            if process_type == ProcessType::DockerCompose {
                if let Some(tailer_pid) = proc.docker_logs_tailer_pid.take() {
                    let _ = std::process::Command::new("kill")
                        .args(["-9", &tailer_pid.to_string()])
                        .output();
                    proc.logs.push_back(format!("🛑 Stopped docker logs tailer (PID: {})", tailer_pid));
                }
            }
            
            proc.is_running = false;
            self.pid_manager
                .remove_pid(&process_type.to_string())
                .await?;
        }
        Ok(())
    }

    pub async fn restart_process(&self, process_type: ProcessType) -> Result<()> {
        // Clear logs on restart
        {
            let mut processes = self.processes.lock().await;
            if let Some(proc) = processes.iter_mut().find(|p| p.process_type == process_type) {
                proc.logs.clear();
                proc.logs.push_back(format!("🔄 Restarting {}...", process_type));
            }
        }
        
        self.stop_process(process_type).await?;
        // Give the process time to fully stop
        tokio::time::sleep(tokio::time::Duration::from_millis(500)).await;
        self.start_process(process_type).await?;
        Ok(())
    }

    /// Optimized restart for Docker: stops Docker and rebuilds in parallel, then restarts Docker
    /// This is much faster than sequential stop -> rebuild -> start
    pub async fn restart_docker_optimized(&self) -> Result<()> {
        // 1. Stop Docker Compose
        self.stop_process(ProcessType::DockerCompose).await?;

        // 2. Start Gradle build (runs in parallel with Docker shutdown)
        self.start_process(ProcessType::Gradle).await?;

        // 3. Wait for both Docker stop and Gradle build to complete
        self.wait_for_completion(ProcessType::Gradle).await?;

        // 4. Start Docker again with fresh artifacts
        self.start_process(ProcessType::DockerCompose).await?;

        Ok(())
    }

    pub async fn get_status(&self) -> Value {
        let processes = self.processes.lock().await;
        let mut status_map = serde_json::Map::new();

        for proc in processes.iter() {
            status_map.insert(
                proc.process_type.to_string(),
                json!({
                    "running": proc.is_running,
                    "log_count": proc.logs.len()
                }),
            );
        }

        json!({ "processes": status_map })
    }

    pub async fn get_logs(&self, process_type: ProcessType) -> Vec<String> {
        let processes = self.processes.lock().await;
        processes
            .iter()
            .find(|p| p.process_type == process_type)
            .map(|p| p.logs.iter().cloned().collect())
            .unwrap_or_default()
    }

    pub async fn cleanup_previous_pids(&self) -> Result<()> {
        self.pid_manager.cleanup_previous_pids().await
    }

    /// Wait for a process to complete (is_running becomes false).
    /// Polls every 100ms to check status.
    pub async fn wait_for_completion(&self, process_type: ProcessType) -> Result<()> {
        loop {
            let is_running = {
                let processes = self.processes.lock().await;
                processes
                    .iter()
                    .find(|p| p.process_type == process_type)
                    .map(|p| p.is_running)
                    .unwrap_or(false)
            };

            if !is_running {
                return Ok(());
            }

            // Poll every 100ms
            tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;
        }
    }
}
