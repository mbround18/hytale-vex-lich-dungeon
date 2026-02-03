#!/bin/bash

set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

WATCH_DIR="shared/interfaces/src"
BUILD_TASK="build"
SERVICE_NAME="hytale-server"
DEBOUNCE_SECONDS=5

last_build_time=0
compose_pid=""

build_and_copy() {
  ./gradlew "$BUILD_TASK"
}

hard_reload() {
  build_and_copy
  # NOTE: UI assets live in shared interfaces resources now. Rebuild + restart to refresh them.
  docker compose restart
  echo "Hard reload complete. UI assets still require a manual reload if changed."
}

cleanup() {
  if [[ -n "$compose_pid" ]]; then
    docker compose down
    kill "$compose_pid" 2>/dev/null
  fi
}

trap cleanup EXIT INT TERM

docker compose up &
compose_pid=$!

echo "Ready. Press 'r' to reload, 'h' for hard reload (UI changes), 'e' to exit (Ctrl+C also exits)."
while true; do
  printf "[r=reload, h=hard, e=exit] > "
  IFS= read -r -n1 key
  echo
  case "$key" in
    r|R)
      if ! docker compose ps --status running --services | grep -qx "$SERVICE_NAME"; then
        echo "$SERVICE_NAME is not running."
        continue
      fi

      now=$(date +%s)
      if (( now - last_build_time < DEBOUNCE_SECONDS )); then
        echo "Debounced. Try again in a few seconds."
        continue
      fi

      last_build_time=$now
      echo "Rebuilding jars (excluding upnp forwarder)..."
      build_and_copy
      ;;
    h|H)
      echo "Hard reload requested..."
      hard_reload
      ;;
    e|E)
      echo "Exiting."
      break
      ;;
    *)
      echo "Unknown command. Press 'r' to rebuild, 'e' to exit."
      ;;
  esac
done
