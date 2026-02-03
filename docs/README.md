# VexLichDungeon

🧙‍♂️ **Vex the Lich invites you to try the trials of their dungeon!** 🧙‍♂️

_To the victor goes the spoils... but beware, dear adventurer, for only those who complete the dungeon's treacherous challenges will become the new Lich. Do you dare accept Vex's invitation?_

---

This mod splits work into two parts:

- **plugins/**: Java plugins (commands, gameplay tweaks, etc.).
- **plugins/roguelike/src/main/resources/**: Packaged data such as `manifest.json`, `Server/`, and `Common/` content that ships with the Vex roguelike plugin.

## Prerequisites

- Docker and Docker Compose (v2 syntax).
- Git with access to `git@github.com:Ranork/Hytale-Server-Unpacked.git`.
- Java 25 installed (for Gradle wrapper).
- `unzip` available on PATH.

## Setup

Run the setup task from the repo root (wrapper downloads Gradle automatically):

```sh
./gradlew setup
```

What `setup` does:

- Ensures `data/server`, `data/assets`, and `data/unpacked` exist and are owned by `1000:1000`.
- If `data/server/Server/HytaleServer.jar` is missing, starts `docker compose -f compose.yml up` and waits until the jar is produced, then tears the stack down.
- Unzips `data/server/Assets.zip` into `data/assets` (overwrites existing files).
- Clones `git@github.com:Ranork/Hytale-Server-Unpacked.git` into `data/unpacked` (skips if a repo already exists there).

## Working in the template

- Place your Java plugin sources under `plugins/roguelike/`. Configure your Gradle build there to produce the plugin jar.
- Place Hytale data that should be bundled (e.g., `manifest.json`, `Server/`, `Common/`) under `plugins/roguelike/src/main/resources/`.
- The `data/` directory is for local provisioning only and is ignored from version control; it is recreated by `setup`.

## Build and distribution

- Run `./gradlew build` to produce distributables and install them into the local server mods directory.
- Outputs land in `dist/`: `VexLichDungeon-<version>.jar` (includes bundled resources like `manifest.json`, `Server/`, and `Common/`).
- The same jar is copied into `data/server/Server/mods/` to replace any existing copy for quick local testing.

## Local testing

- Run `./gradlew start` to build everything and launch `docker compose up` in the foreground so you can watch logs and interact.
- In-game, use Direct Connect to `127.0.0.1` to hit the locally running server.
- Stop with `Ctrl+C` once you're done testing; rerun `./gradlew start` after code or asset changes.

## Plugin structure

- Main plugin lives at `plugins/roguelike/src/main/java/MBRound18/hytale/vexlichdungeon/VexLichDungeonPlugin.java` and logs messages on startup and shutdown.
- Plugin metadata is in `plugins/roguelike/src/main/resources/plugin.properties` and jar manifest entries are filled via Gradle.
- Build upon this foundation to implement Vex's dungeon trials, rewards, and the path to lichdom.

## UI docs

- `docs/UI_LOADING_TRACKING.md` - crash investigation notes and path troubleshooting.

## Common tasks

- Refresh server files: delete `data/server/Server/HytaleServer.jar` then rerun `gradle setup`.
- Refresh assets: delete `data/assets` and rerun `gradle setup` to re-unzip from `Assets.zip`.
- Update API docs: remove `data/unpacked` and rerun `gradle setup` to reclone.

## Acknowledgments

Special thanks to:

- **Ranork** - [Hytale-Server-Unpacked](https://github.com/Ranork/Hytale-Server-Unpacked) for decompiled server assets and API documentation
- **mbround18** - [mbround18/hytale](https://hub.docker.com/r/mbround18/hytale) Docker image for streamlined local development and testing
