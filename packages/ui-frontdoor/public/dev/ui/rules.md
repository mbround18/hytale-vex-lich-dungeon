# Hytale UI Rules (Observed)

These rules are inferred from the core client UI files in a current Hytale install. They are treated as the golden reference for syntax and structure.

## Project UI Layout (This Repo)

- UI docs live in `shared/interfaces/src/main/resources/Common/UI/Custom/`.
- Demo pages: `Common/UI/Custom/Demos/Pages/`.
- Demo HUDs: `Common/UI/Custom/Demos/Huds/`.
- Render previews (for docs/marketing): `static/dev/ui/*.rendered.png`.

## Java Wiring Patterns (Shared Interfaces)

Reference these for clean, working examples:

- `shared/interfaces/src/main/java/MBRound18/hytale/shared/interfaces/pages/demo/AbstractDemoPage.java`
- `shared/interfaces/src/main/java/MBRound18/hytale/shared/interfaces/abstracts/AbstractCustomUIHud.java`
- `shared/interfaces/src/main/java/MBRound18/hytale/shared/interfaces/commands/DemoPageCommand.java`
- `shared/interfaces/src/main/java/MBRound18/hytale/shared/interfaces/commands/DemoHudCommand.java`

Minimal pattern (page):

```java
@Override
public void build(UICommandBuilder builder) {
  builder.append("Demos/Pages/DemoInputs.ui");
}
```

Minimal pattern (HUD):

```java
@Override
protected void build(UICommandBuilder builder) {
  builder.append("Demos/Huds/DemoHudWidgetStrip.ui");
}
```

## Testing Commands

- `/demo <page>` opens a demo page.
- `/dlist` lists demo pages.
- `/dhud <name>` shows a HUD.
- `/dhud list` lists HUDs.
- `/dhud reset` clears + resets the HUD state.

## Writing UI Docs (Grimoire)

- Docs live in `static/dev/ui/*.md` and are rendered by `/dev/ui/`.
- Navigation is controlled by `static/js/ui.js` (`pages` array).
- Keep file names stable so links don’t break.
- Add or update `.rendered.png` previews for visual reference.

## Lessons Learned (From the Code)

- Client crashes usually mean a bad UI path or missing asset.
- Normalize UI paths to the client format (strip everything through `Custom/`).
- Keep demo pages tiny and focused for faster iteration.
- For HUDs, clear state before reset to avoid stale UI.
- Run UI commands on the world thread.

## File Structure

- **Imports**: `$Alias = "Relative/Path.ui";`
- **Assignments**: `@Name = ...;` (styles, numbers, tuples, templates)
- **Elements**: `Group { ... }`, `Label #Id { ... }`, `TextButton { ... }`
- **Template definitions**: `@Template = Group { ... };`
- **Template instantiation**: `$Alias.@Template { ... }`

## Statements

- **Every statement ends with `;`** unless it is an element block (`Group { ... }`) or a template instantiation block (`$Alias.@Template { ... }`).
- **Assignment blocks** must end with `;` after the closing brace: `@Template = Group { ... };`

## Comments

- Line comments: `// like this`
- Block comments: `/* like this */`

## Lists

- List assignment uses `[...]`:
  `@Hints = [ "A", "B" ];`

## Strings & Localization

- Strings use `"..."`
- Localization tokens are used directly: `Text: %client.some.key;` or `%server.some.key;`
- Server translations must live in `shared/interfaces/src/main/resources/Server/Languages/en-US/server.lang`.

## Colors

Valid forms observed in core UI:

- `#RGB`
- `#RGBA`
- `#RRGGBB`
- `#RRGGBBAA`
- Optional alpha suffix: `#000(0.5)`

## Backgrounds

Observed patterns include:

- `Background: #000000;`
- `Background: (TexturePath: "Common/Panel.png", Border: 12);`
- `Background: "SomeTexture.png";`
- Full-screen images use `BackgroundImage { Image: "..."; }` (not `Image { ... }`)

## IDs

- Element IDs use `#Id` and appear after element type: `Label #Title { ... }`

## Spread / Merge

- Spread is valid **inside tuples only**:
  - `Style: (...@DefaultLabelStyle, FontSize: 18);`
  - `Padding: (...@DefaultPadding, Vertical: 6);`
- **Standalone spread statements** like `...@Template;` are **not supported**.

## Node & Property Validity

- Node types and node-specific properties are validated against core UI usage when core rules are available.
- **`Image` is not a node type**; use `BackgroundImage` or a `Group` with `Background`.
- **`Border` is not a `Group` property**; use `Background` / `PatchStyle` tuples instead.

## UI Command Rules (Runtime)

- **Set label text with a property:** `#FriendsListBody.Text`, not `#FriendsListBody`.
- **Always run UI commands on the world thread.**
- **Client UI paths must not include `Common/UI/Custom/` or `UI/Custom/`.** Strip to after `Custom/` (use `UiPath.normalizeForClient(...)`).

## Most Common Elements

- `Group`, `Label`, `Button`, `TextButton`, `ProgressBar`, `TextField`, `CheckBox`, `DropdownBox`

## Most Common Properties

- `Anchor`, `Background`, `LayoutMode`, `Style`, `Text`, `Padding`, `FlexWeight`, `Visible`
