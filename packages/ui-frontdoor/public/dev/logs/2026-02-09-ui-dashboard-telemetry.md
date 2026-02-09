---
createdAt: 2026-02-09
updatedAt: 2026-02-09
title: "UI Dashboard Telemetry Visualization"
tags: [ui, dashboard, telemetry, visualization]
commit: 7f61be9
---
## UI Dashboard Telemetry Visualization

Enhanced the web dashboard with real-time dungeon instance statistics, map visualization, and telemetry event tracking.

## Highlights

- **MapView Enhancements**: Added room statistics display showing cleared/total enemies per room, room coordinates, and event room indicators.
- **StatsView Components**: New instance statistics panel tracking total instances, active players, room generation counts.
- **Telemetry Integration**: Connected UI to DungeonMaster SSE endpoints for live event streaming.
- **Instance Handlers**: Dedicated handlers for processing room, elimination, and instance lifecycle events.
- **Home Page Updates**: Added video tutorials section with categories (Features, Development, Tutorials).
- **Navigation Improvements**: Enhanced site navigation with better structure and dev log links.

## Notes

The dashboard now provides real-time visibility into dungeon generation, player progression, and enemy eliminations. The MapView displays a live grid of generated rooms with color-coded status indicators (cleared/active/event rooms). Stats update in real-time as events stream from the server.
