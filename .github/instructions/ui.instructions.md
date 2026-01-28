---
name: Hytale UI Instructions (Custom UI Only)
applyTo: "**/assets/Common/UI/Custom/**,**/assets/Server/Languages/en-US/server.lang"
---

# Hytale UI Instructions (Custom UI Only)

1. Never edit data/assets

- Treat data/assets as read-only extracted assets.
- All custom UI must live under assets/.

1. Do NOT override core UI

- Never ship Common/UI/Custom/Common.ui or Common/UI/Custom/Common/\*\* in our asset pack.
  - Use a dedicated namespace:
    - UI pages: assets/Common/UI/Custom/Vex/Pages/
    - Shared Vex styles: assets/Common/UI/Custom/Vex/
    - Vex textures: assets/Common/UI/Custom/Vex/\*.png

1. Pathing

- UI documents loaded via: commands.append("Root", "Custom/Vex/Pages/YourPage.ui")
- Shared styles import: $V = "../Vex/VexCommon.ui";
- TexturePath is relative to Custom root:
  - Correct: "Vex/ContainerHeader.png"
  - Wrong: "Common/UI/Custom/Vex/ContainerHeader.png"

1. Namespacing

- Avoid core IDs (#Title, #Content, #ContainerDecorationBottom).
- Use Vex-specific IDs: #VexTitle, #VexContent, #VexDecorationBottom.

1. Localization

- All text must use server.lang keys in assets/Server/Languages/en-US/server.lang
- Keys must be in customUI.vex\* namespace.

1. Images

- Provide both 1x and @2x when possible.
- Stock sizes:
  - DecorationBottom: 236x11 (1x), 472x22 (@2x)
  - Header: 714x38 (1x), 1428x76 (@2x)
  - Patch: 66x66 (1x), 132x132 (@2x)

1. Build and install

- After UI changes, run: ./gradlew assetsZip installToServerMods

1. Debug

- /vex ui list
- /vex ui show summary --var-SummaryStats=Test --var-SummaryBody=Hello
