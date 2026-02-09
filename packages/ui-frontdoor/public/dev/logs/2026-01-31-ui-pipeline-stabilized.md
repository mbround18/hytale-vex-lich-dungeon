---
createdAt: 2026-01-31
updatedAt: 2026-01-31
title: "UI Pipeline Stabilized"
tags: [ui, hud, tooling, devlog]
commit: 1cbb1c5
---

# UI Pipeline Stabilized (After a Long Week)

This has been a long, hard stretch. I spent a week staying up until 6 a.m. most nights, and my wife has been incredibly supportive through all of it. Her encouragement kept me grounded while I worked through a mountain of UI and server issues.

## What finally came together

- A working UI pipeline for pages and HUDs.
- Stable command entry points for demos.
- A template project so other modders can ramp up faster.
- Tooling to make UI authoring bearable.

## The hard road (recent)

The toughest part was getting Custom UI to load consistently:

- Server errors when loading UI documents.
- Client crashes from missing or mismatched UI paths.
- HUD state not clearing or resetting predictably.

The fixes were small but crucial:

- Normalize UI paths to the client format.
- Build tiny, single-purpose UI demos first, then scale.
- Add explicit reset and clear flows for HUDs.
- Keep a visual snapshot trail of UI renders to compare.

## Community work

- Modding template: https://github.com/mbround18/hytale-modding-template
- VS Code extension: UI intellisense, validation, and highlighting for Hytale UI files.

## What’s next (teaser)

I am still pushing hard on Vex Dungeon. Coming up:

- A beautiful party and friends plugin.
- Discord rich integration.
- More UI polish and workflow improvements.

Thanks for following the journey. This is only the start.
