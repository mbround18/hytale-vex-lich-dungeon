---
createdAt: 2026-02-02
updatedAt: 2026-02-02
title: "Friends UI + Party HUD Refactors"
tags: [ui, friends, hud, cleanup]
commit: acacc0d
---
## Friends UI + Party HUD Refactors

We cleaned up the friends UI stack and tightened the party HUD flow.

## Highlights

- Refactored Friends UI components for clearer layout and safer updates.
- Improved party HUD update plumbing to reduce drift and stale renders.
- Hardened shared social models by enforcing non-null IDs and fields.
- Removed unused UI classes/templates tied to the old friend flow.

## Notes

- This pass focused on stability and maintainability rather than new features.
