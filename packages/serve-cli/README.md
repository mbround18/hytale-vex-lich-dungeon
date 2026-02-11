# Hytale Dev Server Web App

A modern web-based dashboard for managing the Hytale development environment. This web app orchestrates the game server, Docker services, and frontend dev tools with real-time log streaming via WebSocket.

## Features

✨ **Unified Process Management**

- Gradle build system
- Docker Compose server (`docker-compose up`)
- pnpm dev environment (`pnpm i && pnpm dev`)
- **Full restart support** - Processes can be restarted without losing log connectivity

🌐 **Web-Based Dashboard**

- Browser-based real-time log viewing (http://localhost:8080)
- WebSocket streaming for each process
- Multiple concurrent connections supported
- Clean, dark-themed GitHub-style UI

📡 **Real-time Log Streaming**

- Zero-latency WebSocket connections per process
- Auto-scroll with manual control
- Clear logs or jump to bottom with one click
- Process status indicators
- **Persistent log streaming** - Logs reconnect automatically after restart

🧹 **Automatic PID Cleanup**

- Stores process IDs in `./tmp/pids/`
- Automatically kills orphaned processes on startup
- Cleans up all PIDs on exit
- **Docker logs tailer management** - Properly stops and restarts log followers

📊 **Easy Customization**

- Single `index.html` file you can extend
- Responsive design suitable for small and large displays
- Add custom controls, filtering, or export features easily
- Pure HTML/CSS/JS frontend, no build step required

## Recent Fixes (Feb 2026)

🔧 **Restart Functionality Now Works**:

- Fixed child process ownership issue preventing restarts
- Docker logs tailer now properly stops and restarts with container
- Log buffers automatically clear on restart for clean slate
- Container names dynamically detected (no hardcoded names)
- Race conditions in restart flow eliminated

🐛 **Resolved Issues**:

- ✅ Restarts now work properly (were broken)
- ✅ Logs pickup correctly after restart
- ✅ Docker logs tailer lifecycle properly managed
- ✅ Exit watcher no longer steals process ownership

## Installation

Build the web app:

```bash
cd packages/serve-cli
cargo build --release
```

The binary will be at `target/release/serve-cli`.

## Usage

Start the dev server:

```bash
./packages/serve-cli/target/release/serve-cli --dir . --port 8080
```

Then open your browser to **http://localhost:8080** and you'll see the log dashboard.

### Command Line Options

```bash
serve-cli --dir /path/to/project --port 8080
```

- `--dir` - Path to the hytale project (default: current directory)
- `--port` - Port to listen on (default: 8080)

## Dashboard Usage

### Viewing Logs

- Click the tabs (🐘 Gradle, 🐳 Docker, 📦 Pnpm) to switch between process logs
- Logs update in real-time as processes generate output
- New log lines are color-coded by type (error, warn, success, info)

### Log Controls

- **↓ Bottom** - Jump to latest logs
- **🗑️ Clear** - Remove all logs for that process
- **🔄 Restart** - Restart the process (clears logs and reconnects)
- **Scroll** - Manually scroll to view historical logs

### Connection Status

Each process shows a connection indicator (green = connected, red = disconnected)

### Process Control

Use the `/api/control` endpoint to manage processes programmatically:

```bash
# Restart Docker (automatically rebuilds via Gradle)
curl -X POST http://localhost:8080/api/control \
  -H "Content-Type: application/json" \
  -d '{"process": "docker", "action": "restart"}'

# Stop a process
curl -X POST http://localhost:8080/api/control \
  -H "Content-Type: application/json" \
  -d '{"process": "pnpm", "action": "stop"}'

# Start a process
curl -X POST http://localhost:8080/api/control \
  -H "Content-Type: application/json" \
  -d '{"process": "gradle", "action": "start"}'
```

## How It Works

### Core Architecture

serve-cli orchestrates your entire Hytale development environment with:

1. **Dependency-aware process sequencing** - Gradle builds before Docker starts
2. **Real-time WebSocket log streaming** - Each process has a dedicated stream
3. **Graceful lifecycle management** - PIDs tracked, processes cleanly stopped
4. **Dynamic container detection** - No hardcoded container names

### On Startup

1. Checks for orphaned processes from previous runs in `./tmp/pids/`
2. Kills any found orphaned PIDs
3. Spawns three main processes:
   - `./gradlew build` - Gradle build system
   - `docker compose up --no-log-prefix` - Server infrastructure
   - `pnpm i && pnpm dev` - Frontend development environment

### Log Streaming

- Each process log stream is independent WebSocket connection
- Logs are buffered in memory (last 1000 lines per process)
- WebSocket clients receive new lines every 500ms
- Multiple clients can connect to the same app simultaneously

### PID Management

All spawned process IDs are stored in `./tmp/pids/`:

- `Gradle.pid` - Gradle process ID
- `DockerCompose.pid` - Docker Compose process ID
- `Pnpm.pid` - pnpm development process ID

When the app starts, it automatically kills any processes referenced in these files before starting new ones.

## Process Details

### Gradle (🐘)

- Builds the plugin system and shared libraries
- Initial build takes time - wait for "BUILD SUCCESS" message
- Required for server code changes

### Docker (🐳)

- Runs the Hytale server infrastructure via docker-compose
- Logs show container initialization and server startup
- Monitor here to check server health

### pnpm (📦)

- Installs dependencies with `pnpm install`
- Starts dev server with `pnpm dev`
- Hot-reloads on file changes
- Runs the web dashboard and UI components

## Customizing the Dashboard

The dashboard is a single HTML file with embedded CSS and JavaScript. You can easily customize it:

### File Location

- `static/index.html` - The complete web app

### Easy Customizations

1. **Add more tabs** - Duplicate a tab button and log pane
2. **Change colors** - Modify the color values in the `<style>` block
3. **Add buttons** - Create new actions that call API endpoints
4. **Custom log filtering** - Add JavaScript to filter logs in real-time
5. **Log export** - Add button to download logs as text file

### Available APIs

```javascript
// Get all logs for a process:
fetch("/api/logs/gradle")
  .then((r) => r.json())
  .then((data) => console.log(data.logs));

// Get process status:
fetch("/api/status")
  .then((r) => r.json())
  .then((data) => console.log(data.processes.gradle.running));

// Connect to WebSocket:
const ws = new WebSocket("ws://localhost:8080/ws/gradle");
ws.onmessage = (event) => {
  const msg = JSON.parse(event.data);
  console.log(msg.line); // Log line
};
```

### Example Enhancement

Add a button to the dashboard to clear all logs:

```html
<button class="secondary" onclick="clearAllLogs()">🧹 Clear All</button>
```

```javascript
function clearAllLogs() {
  ["gradle", "docker", "pnpm"].forEach(clearLogs);
}
```

## Troubleshooting

### Can't connect to the web app

- Make sure port 8080 is not in use: `lsof -i :8080`
- Try a different port: `serve-cli --port 3000`
- Check firewall settings

### Orphaned Processes

If you see processes still running after exiting:

```bash
# The app should handle this automatically on restart
# But if needed, manually kill by name:
pkill -f "docker compose"
pkill -f "pnpm"
pkill -f "gradle"
```

### Docker Not Starting

Check Docker logs:

- Click the Docker tab
- Look for error messages
- Common issue: Docker daemon not running (`docker ps`)

### pnpm Installation Fails

- Check for network connectivity
- Try manually running `pnpm install` in project root
- Check `pnpm-lock.yaml` is not corrupted

## Architecture

The web app is built with:

- **Actix-web** - Web framework for HTTP server
- **Actix web-actors** - WebSocket support via Actor model
- **Tokio** - Async Rust runtime for process management
- **Serde JSON** - JSON serialization

### Request Flow

```
Browser connects to http://localhost:8080/
  ↓
Actix-web serves index.html
  ↓
JavaScript connects to /ws/gradle, /ws/docker, /ws/pnpm
  ↓
LogStreamActor sends new log lines every 500ms
  ↓
Dashboard receives JSON messages and renders logs
```

## Development

To modify the app:

```bash
# Check for errors without building
cargo check

# Build in debug mode
cargo build

# Build optimized release
cargo build --release

# Run with your changes
cargo run -- --dir /path/to/project --port 8080
```

### Key Files

- `src/main.rs` - Entry point and CLI argument parsing
- `src/server.rs` - Actix-web app setup and HTTP routes
- `src/ws.rs` - WebSocket Actor implementation
- `src/processes.rs` - Process management and PID cleanup
- `static/index.html` - Frontend dashboard
