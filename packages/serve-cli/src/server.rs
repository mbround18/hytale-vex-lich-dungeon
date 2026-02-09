use crate::processes::{ProcessManager, ProcessType};
use crate::ws::ws_handler;
use actix_files::Files;
use actix_web::{web, App, HttpResponse, HttpServer, Responder};
use serde::Deserialize;
use serde_json::json;
use std::sync::Arc;

pub async fn run(process_manager: Arc<ProcessManager>, port: u16) -> anyhow::Result<()> {
    // web::Data wraps the Arc, allowing it to be cloned cheaply into Actix workers
    let pm_data = web::Data::new(process_manager);

    println!("🚀 Server starting on http://0.0.0.0:{}", port);

    HttpServer::new(move || {
        App::new()
            .app_data(pm_data.clone())
            // API Endpoints
            .service(
                web::scope("/api")
                    .route("/logs/{process}", web::get().to(get_logs))
                    .route("/status", web::get().to(get_status))
                    .route("/control", web::post().to(control_process)),
            )
            // WebSocket Endpoint
            .route("/ws/{process}", web::get().to(ws_handler))
            // Static/Docs
            .route("/openapi.json", web::get().to(openapi_spec))
            .route("/", web::get().to(index))
            .service(Files::new("/static", "./static").show_files_listing())
    })
    .bind(("0.0.0.0", port))?
    .run()
    .await?;

    Ok(())
}

async fn index() -> impl Responder {
    // Embeds the HTML in the binary for single-file deployment convenience
    const DASHBOARD_HTML: &str = include_str!("../static/index.html");

    HttpResponse::Ok()
        .content_type("text/html; charset=utf-8")
        .body(DASHBOARD_HTML)
}

async fn get_logs(path: web::Path<String>, pm: web::Data<Arc<ProcessManager>>) -> impl Responder {
    let process_name = path.into_inner();

    let process_type = match process_name.as_str() {
        "gradle" => ProcessType::Gradle,
        "docker" => ProcessType::DockerCompose,
        "pnpm" => ProcessType::Pnpm,
        _ => return HttpResponse::BadRequest().json(json!({"error": "Unknown process type"})),
    };

    // Await the async lock retrieval in ProcessManager
    let logs = pm.get_logs(process_type).await;

    HttpResponse::Ok().json(json!({
        "process": process_name,
        "logs": logs
    }))
}

async fn get_status(pm: web::Data<Arc<ProcessManager>>) -> impl Responder {
    // Await the async status generation
    let status = pm.get_status().await;
    HttpResponse::Ok().json(status)
}

#[derive(Deserialize)]
struct ControlRequest {
    process: String,
    action: String,
}

async fn control_process(
    payload: web::Json<ControlRequest>,
    pm: web::Data<Arc<ProcessManager>>,
) -> impl Responder {
    let process_type = match payload.process.as_str() {
        "gradle" => ProcessType::Gradle,
        "docker" => ProcessType::DockerCompose,
        "pnpm" => ProcessType::Pnpm,
        _ => return HttpResponse::BadRequest().json(json!({"error": "Unknown process type"})),
    };

    let result = match payload.action.as_str() {
        "start" => pm.start_process(process_type).await,
        "stop" => pm.stop_process(process_type).await,
        "restart" => {
            // Use optimized restart for Docker (parallel stop + rebuild)
            if process_type == ProcessType::DockerCompose {
                pm.restart_docker_optimized().await
            } else {
                pm.restart_process(process_type).await
            }
        }
        _ => return HttpResponse::BadRequest().json(json!({"error": "Unknown action"})),
    };

    match result {
        Ok(_) => HttpResponse::Ok().json(json!({
            "status": "ok",
            "process": payload.process,
            "action": payload.action
        })),
        Err(e) => HttpResponse::InternalServerError().json(json!({
            "error": e.to_string()
        })),
    }
}

async fn openapi_spec() -> impl Responder {
    let spec = json!({
        "openapi": "3.0.0",
        "info": {
            "title": "Hytale Dev Server API",
            "description": "Real-time process and log management for Hytale development environment",
            "version": "1.0.0",
            "contact": { "name": "Hytale Mods" }
        },
        "servers": [
            { "url": "http://localhost:8080", "description": "Local development server" }
        ],
        "tags": [
            { "name": "Dashboard", "description": "Web interface endpoints" },
            { "name": "Status", "description": "Process status and health" },
            { "name": "Logs", "description": "Log retrieval and streaming" }
        ],
        "paths": {
            "/": {
                "get": {
                    "summary": "Get the dashboard",
                    "operationId": "getDashboard",
                    "responses": {
                        "200": { "description": "Dashboard HTML", "content": { "text/html": {} } }
                    }
                }
            },
            "/api/status": {
                "get": {
                    "summary": "Get process status",
                    "operationId": "getStatus",
                    "responses": {
                        "200": { "description": "Process status info", "content": { "application/json": {} } }
                    }
                }
            },
            "/api/logs/{process}": {
                "get": {
                    "summary": "Get logs for a process",
                    "operationId": "getLogs",
                    "parameters": [
                        { "name": "process", "in": "path", "required": true, "schema": { "type": "string", "enum": ["gradle", "docker", "pnpm"] } }
                    ],
                    "responses": {
                        "200": { "description": "Log lines", "content": { "application/json": {} } },
                        "400": { "description": "Invalid process name" }
                    }
                }
            },
            "/api/control": {
                "post": {
                    "summary": "Control a process",
                    "operationId": "controlProcess",
                    "requestBody": {
                        "required": true,
                        "content": {
                            "application/json": {
                                "schema": {
                                    "type": "object",
                                    "required": ["process", "action"],
                                    "properties": {
                                        "process": { "type": "string", "enum": ["gradle", "docker", "pnpm"] },
                                        "action": { "type": "string", "enum": ["start", "stop", "restart"] }
                                    }
                                }
                            }
                        }
                    },
                    "responses": {
                        "200": { "description": "Action accepted", "content": { "application/json": {} } },
                        "400": { "description": "Invalid process name or action" }
                    }
                }
            },
            "/ws/{process}": {
                "get": {
                    "summary": "WebSocket stream",
                    "description": "Upgrade to WebSocket",
                    "parameters": [
                        { "name": "process", "in": "path", "required": true, "schema": { "type": "string", "enum": ["gradle", "docker", "pnpm"] } }
                    ],
                    "responses": {
                        "101": { "description": "Switching Protocols" }
                    }
                }
            }
        }
    });

    HttpResponse::Ok().json(spec)
}
