use crate::processes::{ProcessManager, ProcessType};
use crate::ws::ws_handler;
use actix_files::Files;
use actix_web::{middleware, web, App, HttpResponse, HttpServer, Responder};
use serde::Deserialize;
use serde_json::{json, Value};
use std::sync::Arc;
use tracing::{error, info};

/// Helper to parse process strings into ProcessType
fn parse_process_type(name: &str) -> Option<ProcessType> {
    match name.to_lowercase().as_str() {
        "gradle" => Some(ProcessType::Gradle),
        "docker" | "docker-compose" => Some(ProcessType::DockerCompose),
        "pnpm" => Some(ProcessType::Pnpm),
        _ => None,
    }
}

pub async fn run(process_manager: Arc<ProcessManager>, port: u16) -> anyhow::Result<()> {
    // Store the Arc itself so handlers can extract Arc<ProcessManager>
    let pm_data = web::Data::new(process_manager.clone());

    info!("🚀 Server starting on http://0.0.0.0:{}", port);

    HttpServer::new(move || {
        App::new()
            .app_data(pm_data.clone())
            // Middleware for clean request logging
            .wrap(middleware::Logger::default())
            // API Endpoints
            .service(
                web::scope("/api")
                    .route("/status", web::get().to(get_status))
                    .route("/logs/{process}", web::get().to(get_logs))
                    .route("/control", web::post().to(control_process)),
            )
            // WebSocket Stream
            .route("/ws/{process}", web::get().to(ws_handler))
            // Static Assets & Docs
            .route("/openapi.json", web::get().to(openapi_spec))
            .route("/", web::get().to(index))
            .service(Files::new("/dist", "./dist").show_files_listing())
    })
    .bind(("0.0.0.0", port))?
    .run()
    .await?;

    Ok(())
}

async fn index() -> impl Responder {
    // Embedded HTML for zero-dependency deployment
    const DASHBOARD_HTML: &str = include_str!("../static/index.html");

    HttpResponse::Ok()
        .content_type("text/html; charset=utf-8")
        .body(DASHBOARD_HTML)
}

async fn get_logs(path: web::Path<String>, pm: web::Data<Arc<ProcessManager>>) -> impl Responder {
    let process_name = path.into_inner();

    match parse_process_type(&process_name) {
        Some(pt) => {
            let logs = pm.get_ref().get_logs(pt).await;
            HttpResponse::Ok().json(json!({
                "process": process_name,
                "logs": logs
            }))
        }
        None => HttpResponse::BadRequest()
            .json(json!({"error": format!("Unknown process: {}", process_name)})),
    }
}

async fn get_status(pm: web::Data<Arc<ProcessManager>>) -> impl Responder {
    let status = pm.get_ref().get_status().await;
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
    let pt = match parse_process_type(&payload.process) {
        Some(t) => t,
        None => return HttpResponse::BadRequest().json(json!({"error": "Unknown process type"})),
    };

    info!("Control request: {} -> {}", payload.process, payload.action);

    let result = match payload.action.as_str() {
        "start" => pm.get_ref().start_process(pt).await,
        "stop" => pm.get_ref().stop_process(pt).await,
        "restart" => {
            if pt == ProcessType::DockerCompose {
                // Check if specialized optimized method exists
                pm.get_ref().restart_process(pt).await
            } else {
                pm.get_ref().restart_process(pt).await
            }
        }
        _ => return HttpResponse::BadRequest().json(json!({"error": "Unknown action"})),
    };

    match result {
        Ok(_) => HttpResponse::Ok().json(json!({
            "status": "success",
            "process": payload.process,
            "action": payload.action
        })),
        Err(e) => {
            error!("Failed to {} {}: {}", payload.action, payload.process, e);
            HttpResponse::InternalServerError().json(json!({
                "error": e.to_string()
            }))
        }
    }
}

async fn openapi_spec() -> impl Responder {
    let spec: Value = json!({
        "openapi": "3.0.0",
        "info": {
            "title": "Hytale Dev Server API",
            "description": "Real-time process management for Hytale development",
            "version": "1.1.0"
        },
        "paths": {
            "/api/status": {
                "get": { "summary": "Get all process statuses", "responses": { "200": { "description": "OK" } } }
            },
            "/api/control": {
                "post": {
                    "summary": "Control a process",
                    "requestBody": {
                        "content": {
                            "application/json": {
                                "schema": {
                                    "type": "object",
                                    "properties": {
                                        "process": { "type": "string", "enum": ["gradle", "docker", "pnpm"] },
                                        "action": { "type": "string", "enum": ["start", "stop", "restart"] }
                                    }
                                }
                            }
                        }
                    },
                    "responses": { "200": { "description": "OK" } }
                }
            }
        }
    });

    HttpResponse::Ok().json(spec)
}
