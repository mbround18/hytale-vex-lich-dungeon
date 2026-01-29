# UI Loading Tracking (Vex Lich Dungeon)

## Summary

We are debugging client crashes when opening Custom UI pages. The client reports:

- `Crash - Could not find document Custom/Vex/Pages/VexDungeonSummary.ui for Custom UI Append command.`

## Current Status

- **Asset packs loaded (server logs):**
  - `Hytale:Hytale`
  - `MBRound18:VexLichDungeon` (appears twice; note)
- **Pack manifest version:** strict semver (`0.1.0`) to satisfy AssetModule.
- **Assets zip contains UI docs:**
  - `Common/UI/Custom/Vex/Pages/VexDungeonSummary.ui`
  - `Custom/Vex/Pages/VexDungeonSummary.ui` (fallback copy)
- **UI commands:** using `append()` with resolved path. Inline mode disabled for UI docs with imports.
- **Template source priority:** data `ui-templates.json` overrides bundled defaults (now synced on startup).
- **Latest test path:** `UI/Custom/Vex/Pages/VexDungeonSummary.ui` (no leading `/`).

## Findings (New)

- **Client paths must strip everything through `Custom/`.**
  - `Common/UI/Custom/Friends/Pages/FriendsList.ui` -> `Friends/Pages/FriendsList.ui`
  - `UI/Custom/Friends/Pages/FriendsList.ui` -> `Friends/Pages/FriendsList.ui`
  - `Custom/Friends/Pages/FriendsList.ui` -> `Friends/Pages/FriendsList.ui`
- **Label updates must target properties** (ex: `#FriendsListBody.Text`, not `#FriendsListBody`).
- **UI commands must run on the world thread** (scheduler/ForkJoin will assert).

## Timeline / Changes

- Added UI template registries in engine and JSON config (`ui-templates.json`).
- Added preflight check to validate UI docs exist in assets zip.
- Packaged both `Common/UI/Custom/...` and `Custom/...` copies in assets zip.
- Ensured assets zip is versioned, then reverted manifest version to strict semver.
- Added inline UI fallback; disabled for docs importing other `.ui` files.
- Added asset pack logging at plugin start.
- Switched summary UI path to `Common/UI/Custom/Pages/VexDungeonSummary.ui` (full path).
- Normalized resolver paths (strip leading `/`) to avoid accidental mismatches.
- Synced data `ui-templates.json` from bundled defaults to avoid stale paths.
- Tested leading `/Common/UI/Custom/Pages/...` path (still missing).
- Tested leading `/UI/Custom/Vex/Pages/...` path.
- Now testing `UI/Custom/Vex/Pages/VexDungeonSummary.ui` (no leading `/`).

## Hypotheses

- Client is not actually loading the mod asset pack despite server listing it.
- Client cache or load order issue prevents the UI doc from being available.
- Duplicate pack registration might cause one pack to be ignored or overridden.

## Next Checks (Shortlist)

- [ ] Confirm client asset cache cleared / pack re-downloaded after install.
- [ ] Verify pack list on client side (if accessible via logs).
- [ ] Inspect duplicate `MBRound18:VexLichDungeon` pack registration source.
- [ ] Add server-side trace for UI command path + pack presence at command time.
- [ ] Rebuild/install to refresh data `ui-templates.json`, then retest `/vex ui show summary`.
- [ ] If `/UI/Custom/...` fails, test `Custom/Vex/Pages/...` without leading `/` again.
- [ ] Test a minimal UI doc with no imports to isolate document lookup.

## Notes

- Server does not resolve `.ui` documents; it only sends UI command packets.
- `AppendInline` fails for UI documents with imports (e.g., `$V = "../VexCommon.ui"`).
- Stale data templates were still pointing to `Custom/Vex/Pages/...` even after code changes.
