---
createdAt: 2026-01-31
updatedAt: 2026-01-31
title: "TroubleDev Alignment Check"
tags: [alignment, docs, commands, threading, ui]
commit: uncommitted
---

## Alignment Check: TroubleDev Articles

We reviewed our implementation against the TroubleDev guidance on commands, ECS access, threading, and Custom UI paths.

## Alignment

- Commands + threading: `AbstractCommand` extends `AbstractPlayerCommand`, so handlers inherit the safe threading + ECS access model and Store/Ref availability.
- Threading model: UI flows execute Store access on the world thread via `UiThread.runOnPlayerWorld(...)` (through `AbstractCustomUIController` and openers), matching the world-thread safety guidance.
- ECS access: `AbstractCustomUIController` pulls a `Ref<EntityStore>` from the `PlayerRef`, validates it, and then reads components from the Store, which follows the Universe → World → EntityStore access pattern.
- Custom UI paths: `AbstractCustomUIHud` and `AbstractCustomUIPage` normalize UI paths for client usage and append to the UI command builder, keeping paths relative to Custom UI root and aligning with TroubleDev’s path conventions.

## Notes

- If we want to enforce relative-only paths at load time, we can add validation in `UiTemplateLoader` or `UiPath`.
