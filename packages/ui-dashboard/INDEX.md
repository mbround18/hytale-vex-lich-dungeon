# ui-dashboard index

Overview of the key files and directories for `packages/ui-dashboard`.

**Entrypoints**
- `index.html` Base HTML shell for Vite.
- `src/main.tsx` React bootstrap and root mount.
- `src/App.tsx` Dashboard composition, routing, state ingestion, replay + archives.

**API**
- `src/api/index.ts` Axios client + API base resolution.

**State**
- `src/state/dashboardBus.ts` BroadcastChannel event bus for multi-tab sync.
- `src/state/eventArchive.ts` IndexedDB archive and event log storage helpers.

**UI Components**
- `src/components/Header.tsx` Top navigation and status indicator.
- `src/components/StatsView.tsx` Command metrics + tables.
- `src/components/MapView.tsx` Dungeon map visualization.
- `src/components/TelemetryView.tsx` Live event stream + replay controls.
- `src/components/ArchivesView.tsx` Archived runs and replay timeline controls.
- `src/components/map/instanceHandlers.ts` Map event handlers and instance helpers.

**Styles**
- `src/styles/app.scss` Global dashboard styles.

**Types**
- `src/types.ts` Shared dashboard types.
