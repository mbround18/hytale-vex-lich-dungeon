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
- When sending UI paths to the client, strip everything through Custom/:
  - Common/UI/Custom/Friends/Pages/FriendsList.ui -> Friends/Pages/FriendsList.ui
  - UI/Custom/Friends/Pages/FriendsList.ui -> Friends/Pages/FriendsList.ui
  - Custom/Friends/Pages/FriendsList.ui -> Friends/Pages/FriendsList.ui
  - Use UiPath.normalizeForClient(...) from ImmortalEngine.

1. Namespacing

- Avoid core IDs (#Title, #Content, #ContainerDecorationBottom).
- Use Vex-specific IDs: #VexTitle, #VexContent, #VexDecorationBottom.

1. Localization

- All text must use server.lang keys in assets/Server/Languages/en-US/server.lang
- Keys must be in customUI.vex\* namespace.
- When calling translations in Java, use Message.translation("server.ID_OF_TRANSLATION").
- When swapping a stat label (e.g., Mana -> Stamina), update both:
  - The UI label key in the .ui file.
  - The matching server.lang key/value.

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

1. UI command safety

- Custom UI commands must run on the world thread (not scheduler/ForkJoin).
  - Use UiThread.runOnPlayerWorld(playerRef, () -> ...) from ImmortalEngine.
- All UI updates must be executed inside UiThread.runOnPlayerWorld(...).
- When setting label text, target the property:
  - Correct: #FriendsListBody.Text
  - Wrong: #FriendsListBody
  - Use UiVars.textId("FriendsListBody") from ImmortalEngine.

1. HUD text safety

- Never send fractions or slash-delimited ratios in HUD/UI updates (e.g., "0/0", "12/100").
  - Even in plain strings, these can trigger client-side parsing issues.
  - Prefer whole numbers or words instead (e.g., "0 Members", "HP: 0").

1. HUD lifecycle and cleanup

- Always ensure HUD cleanup only targets active HUD instances; never create new HUDs during cleanup operations.
- Use defensive checks (null/active state validation) in cleanup methods before accessing or modifying HUD instances.
- When implementing event-driven cleanup for HUDs, validate that event handlers do not instantiate new objects during teardown.
- Test repeated spawn/removal cycles to catch edge cases like client crashes or lingering UI elements.
- Prefer AbstractCustomUIHud.closeHud() or similar methods that safely close only active instances.

1. Map markers and component cleanup

- Explicitly clear or reset map markers and related components when removing or restoring entities.
- Block components (e.g., BlockMapMarker) must be manually reset during snapshot restore or entity removal.
- Do not rely solely on entity despawn to clean up world markers; explicitly reset blocks to their original state.
- Test marker cleanup across multiple spawn/despawn cycles to ensure no lingering markers remain.
