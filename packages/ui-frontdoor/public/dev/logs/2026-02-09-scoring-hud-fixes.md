---
createdAt: 2026-02-09
updatedAt: 2026-02-09
title: "Enemy Scoring & HUD Fixes"
tags: [bugfix, scoring, hud, threading]
commit: f011a2e
---
## Enemy Scoring & HUD Fixes

Fixed critical bugs preventing enemy kills from awarding points and updating the score HUD. Resolved entity lifecycle crashes, killer attribution failures, and thread safety issues.

## Highlights

- **Fixed entity elimination crash**: Changed `findNearestPlayerInRoom()` to use tracked position instead of removed entity's transform component, preventing `IllegalStateException`.
- **Assigned default points to prefab enemies**: Added point values (3-12) based on enemy type (Bunny=3, Skeleton=7, Archer=8, Alchemist=10, Ghoul=12).
- **Fixed killer detection**: Changed player room tracking from object identity (`Map<Player, RoomKey>`) to UUID-based (`Map<UUID, RoomKey>`), ensuring reliable killer attribution across entity lifecycle events.
- **Fixed HUD instance label**: Changed "Instance" field from displaying total score to showing derived instance ID number (e.g., "Instance: 847").
- **Thread-safe HUD events**: Created `VexHudEventHandler` with `resolvePlayerRef()` to refresh PlayerRef binding from Universe, preventing "Assert not in thread" errors.
- **Single-player fallback**: Added killer attribution fallback for single-player scenarios when room tracking fails.

## Notes

Previously, enemies awarded 0 points when killed due to missing point values in prefab spawns. The HUD displayed confusing labels (both "Instance" and "Player" showed the same score), and thread safety issues caused crashes when updating the UI. All three critical issues are now resolved.
