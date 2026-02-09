---
createdAt: 2026-02-09
updatedAt: 2026-02-09
title: "DungeonMaster Plugin & Event System"
tags: [feature, telemetry, events, debugging]
commit: ebfd580
---

## DungeonMaster Plugin & Event System

Implemented comprehensive event-driven telemetry system with DungeonMaster plugin providing real-time debugging and monitoring capabilities via HTTP API.

## Highlights

- **DungeonMaster Plugin**: New debugging plugin with SSE (Server-Sent Events) for real-time event streaming to web dashboard.
- **Event Architecture**: Created 30+ event classes covering dungeon lifecycle (RoomGenerated, EntityEliminated, PortalCreated, etc.).
- **WorldEventQueue**: Centralized event dispatcher ensuring events fire on correct world thread context.
- **HTTP API Endpoints**: `/api/events/poll`, `/api/events/sse`, `/api/stats`, `/api/worlds`, `/api/players` for live monitoring.
- **Prefab Inspection**: Added PrefabMetadataHandler exposing prefab structure and entity metadata via API.
- **Event Handlers**: Implemented handlers for room tiles, enemies spawn, asset packs, lifecycle events.
- **OpenAPI Documentation**: Auto-generated API docs at `/api/openapi.json`.

## Notes

The event system enables telemetry-driven debugging and provides foundation for future analytics dashboard. Events are serialized to JSON and dispatched via SSE for real-time monitoring in the UI dashboard. All dungeon operations now emit structured events for tracking progression, eliminations, loot rolls, and player actions.
