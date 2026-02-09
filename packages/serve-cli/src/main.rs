use anyhow::Result;
use clap::Parser;
use std::path::PathBuf;
use std::sync::Arc;

mod processes;
mod server;
mod ws;

use processes::{ProcessManager, ProcessType};

#[derive(Parser, Debug)]
#[command(name = "hytale-dev")]
#[command(about = "Hytale Development Environment Manager")]
struct Args {
    /// Working directory for the project
    #[arg(short, long, default_value = ".")]
    dir: PathBuf,

    /// Port to serve the dashboard on
    #[arg(short, long, default_value = "8080")]
    port: u16,
}

#[tokio::main]
async fn main() -> Result<()> {
    let args = Args::parse();

    // 1. Initialize Process Manager (Async)
    // We verify the directory and create PID folders immediately
    let process_manager = ProcessManager::new(args.dir).await?;
    let process_manager = Arc::new(process_manager);

    println!("🧹 Cleaning up previous sessions...");
    process_manager.cleanup_previous_pids().await?;

    println!("⚡ Booting background services...");

    // 2. Start Processes in dependency order
    // Gradle build must complete before Docker starts
    println!("🔨 Starting Gradle build...");
    process_manager.start_process(ProcessType::Gradle).await?;
    process_manager.wait_for_completion(ProcessType::Gradle).await?;
    println!("✓ Gradle build completed");

    println!("🐳 Starting Docker container...");
    process_manager
        .start_process(ProcessType::DockerCompose)
        .await?;
    
    println!("📦 Starting package manager...");
    process_manager.start_process(ProcessType::Pnpm).await?;

    println!("🚀 Dashboard available at http://127.0.0.1:{}", args.port);
    println!("👉 Press Ctrl+C to stop server and kill processes");

    // 3. Run Web Server
    // This function awaits (blocks) until the server is stopped (e.g. via Ctrl+C)
    server::run(process_manager.clone(), args.port).await?;

    // 4. Graceful Shutdown
    // This code runs only after Actix receives a stop signal
    println!("\n🛑 Shutting down services...");

    // Stop in specific order if necessary (e.g., stop app before db)
    let _ = process_manager.stop_process(ProcessType::Pnpm).await;
    let _ = process_manager
        .stop_process(ProcessType::DockerCompose)
        .await;
    let _ = process_manager.stop_process(ProcessType::Gradle).await;

    println!("✓ Shutdown complete. Goodbye!");

    Ok(())
}
