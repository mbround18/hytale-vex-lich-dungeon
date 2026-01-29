# Hytale UI Rules (Observed)

These rules are inferred from the core client UI files in a current Hytale install. They are treated as the golden reference for syntax and structure.

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

## Most Common Elements
- `Group`, `Label`, `Button`, `TextButton`, `ProgressBar`, `TextField`, `CheckBox`, `DropdownBox`

## Most Common Properties
- `Anchor`, `Background`, `LayoutMode`, `Style`, `Text`, `Padding`, `FlexWeight`, `Visible`
