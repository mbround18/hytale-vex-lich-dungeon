---
createdAt: 2026-02-02
updatedAt: 2026-02-02
title: "Dev Feed + Forwarder Hardening"
tags: [tooling, build, forwarder]
commit: d3c6365
---
## Dev Feed + Forwarder Hardening

We tightened release tooling and hardened URL handling in the forwarder.

## Highlights

- Added tasks to update and validate the dev feed and manifest processing.
- Switched UPnP forwarder URL creation to `URI.create` for safer parsing.

## Notes

- These changes keep the release pipeline predictable and reduce edge-case crashes.
