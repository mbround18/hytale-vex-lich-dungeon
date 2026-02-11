use anyhow::Result;
use clap::Parser;
use std::path::PathBuf;
use std::sync::Arc;
use tracing::{info, warn};
use tracing_subscriber::EnvFilter;

mod processes;
mod server;
mod ws;

use processes::{ProcessManager, ProcessType};

#[derive(Parser, Debug)]
#[command(name = "hytale-dev")]
#[command(about = "Hytale Development Environment Manager")]
struct Args {
    /// Working directory for the project
    #[arg(short, long, alias = "root-dir", env = "ROOT_DIR", default_value = ".")]
    dir: PathBuf,

    /// Port to serve the dashboard on
    #[arg(short, long, default_value = "8080")]
    port: u16,

    /// Path to the node binary used to derive npm/pnpm/corepack paths
    #[arg(long, env = "NODE_PATH")]
    node_path: Option<PathBuf>,
}

#[tokio::main]
async fn main() -> Result<()> {
    init_tracing();
    let args = Args::parse();

    // 1. Initialize Process Manager (Async)
    // We verify the directory and create PID folders immediately
    let process_manager = ProcessManager::new(args.dir, args.node_path).await?;
    let process_manager = Arc::new(process_manager);
    
    
    info!("🧹 Cleaning up previous sessions...");
    process_manager.cleanup_previous_pids().await?;

    info!("⚡ Booting background services...");

    // 2. Start Processes in dependency order
    // Gradle build must complete before Docker starts
    info!("🔨 Starting Gradle build...");
    process_manager.start_process(ProcessType::Gradle).await?;
    process_manager
        .wait_for_completion(ProcessType::Gradle)
        .await;
    info!("✓ Gradle build completed");

    info!("🐳 Starting Docker container...");
    process_manager
        .start_process(ProcessType::DockerCompose)
        .await?;

    info!("📦 Starting package manager...");
    process_manager.start_process(ProcessType::Pnpm).await?;

    info!("🚀 Dashboard available at http://127.0.0.1:{}", args.port);
    info!("👉 Press Ctrl+C to stop server and kill processes");

    // 3. Run Web Server
    // This function awaits (blocks) until the server is stopped (e.g. via Ctrl+C)
    server::run(process_manager.clone(), args.port).await?;

    // 4. Graceful Shutdown
    // This code runs only after Actix receives a stop signal
    info!("🛑 Shutting down services...");

    // Stop in specific order if necessary (e.g., stop app before db)
    let _ = process_manager.stop_process(ProcessType::Pnpm).await;
    let _ = process_manager
        .stop_process(ProcessType::DockerCompose)
        .await;
    let _ = process_manager.stop_process(ProcessType::Gradle).await;

    info!("✓ Shutdown complete. Goodbye!");

    Ok(())
}

fn init_tracing() {
    let env_filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new("info"));

    if tracing_subscriber::fmt()
        .with_env_filter(env_filter)
        .with_target(false)
        .try_init()
        .is_err()
    {
        warn!("tracing already initialized");
    }
}
