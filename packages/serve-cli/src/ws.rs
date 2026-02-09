use crate::processes::{ProcessManager, ProcessType};
use actix::prelude::*;
use actix_web::{web, HttpRequest, HttpResponse};
use actix_web_actors::ws;
use serde_json::json;
use std::sync::Arc;
use std::time::Duration;

pub struct LogStreamActor {
    process_type: ProcessType,
    pm: Arc<ProcessManager>,
    last_log_count: usize,
}

impl LogStreamActor {
    pub fn new(process_type: ProcessType, pm: Arc<ProcessManager>) -> Self {
        Self {
            process_type,
            pm,
            last_log_count: 0,
        }
    }
}

impl Actor for LogStreamActor {
    type Context = ws::WebsocketContext<Self>;

    fn started(&mut self, ctx: &mut Self::Context) {
        // Poll for new logs every 500ms
        ctx.run_interval(Duration::from_millis(500), |act, ctx| {
            let pm = act.pm.clone();
            let pt = act.process_type;

            // 1. Create an async future to fetch the logs
            let fut = async move { pm.get_logs(pt).await }
                // 2. Convert it into an ActorFuture so it interacts with the actor context
                .into_actor(act)
                // 3. Handle the result when the future resolves
                .map(|logs, act, ctx| {
                    // Detect buffer resets (e.g. process restart or clear)
                    if logs.len() < act.last_log_count {
                        act.last_log_count = 0;
                    }

                    // If we have new data, send it down the pipe
                    if logs.len() > act.last_log_count {
                        for log in &logs[act.last_log_count..] {
                            let msg = json!({
                                "type": "log",
                                "line": log,
                                "process": act.process_type.to_string()
                            });
                            ctx.text(msg.to_string());
                        }
                        act.last_log_count = logs.len();
                    }
                });

            // 4. Spawn the future into the actor's execution context
            ctx.spawn(fut);
        });
    }
}

/// Standard Actix Stream Handler for incoming WebSocket messages
impl StreamHandler<Result<ws::Message, ws::ProtocolError>> for LogStreamActor {
    fn handle(&mut self, msg: Result<ws::Message, ws::ProtocolError>, ctx: &mut Self::Context) {
        match msg {
            Ok(ws::Message::Ping(msg)) => ctx.pong(&msg),
            Ok(ws::Message::Text(_)) => { /* We ignore incoming text for now */ }
            Ok(ws::Message::Close(reason)) => {
                ctx.close(reason);
                ctx.stop();
            }
            _ => (),
        }
    }
}

/// The HTTP entry point that upgrades the connection
pub async fn ws_handler(
    req: HttpRequest,
    path: web::Path<String>,
    pm: web::Data<Arc<ProcessManager>>,
    stream: web::Payload,
) -> actix_web::Result<HttpResponse> {
    let process_name = path.into_inner();

    // Validate process name before starting the actor
    let process_type = match process_name.as_str() {
        "gradle" => ProcessType::Gradle,
        "docker" => ProcessType::DockerCompose,
        "pnpm" => ProcessType::Pnpm,
        _ => {
            return Err(actix_web::error::ErrorBadRequest(
                "Unknown process identifier",
            ))
        }
    };

    let actor = LogStreamActor::new(process_type, pm.get_ref().clone());

    ws::start(actor, &req, stream)
}
