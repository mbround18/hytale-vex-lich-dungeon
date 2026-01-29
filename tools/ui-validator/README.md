# UI Validator

Validates Hytale `.ui` file syntax to catch errors before runtime.

## Usage

```bash
# Validate all UI files in plugins/
./gradlew validateUi

# Run with full test suite
./gradlew test

# Auto-runs on build
./gradlew build
```

## Checks

- ✅ **Balanced Braces** - Ensures `{` and `}` match
- ✅ **Balanced Parentheses** - Ensures `(` and `)` match
- ✅ **Balanced Brackets** - Ensures `[` and `]` match
- ✅ **Import Paths** - Verifies imported `.ui` files exist
- ✅ **Import Aliases** - Detects `$Alias.` usage without a matching `$Alias = "..."` import
- ✅ **Color Syntax** - Validates `#RGB`, `#RGBA`, `#RRGGBB`, `#RRGGBBAA` (+ optional `(alpha)`)
- ✅ **Element IDs** - Distinguishes `#ElementName` from `#ColorCode`
- ✅ **Statement Boundaries** - Detects missing semicolons and merged statements
- ✅ **Standalone Spreads** - Flags `...@Template;` as invalid outside tuples
- ✅ **Unsupported Nodes** - Flags `Image {}` nodes (use `BackgroundImage` or `Group` + `Background`)
- ✅ **Core Rules (optional)** - When provided, flags node/property usage not observed in core UI files
- ✅ **Assignment/Property Names** - Guards against invalid `@Name =` or `Property:` targets
- ✅ **Common Typos** - Detects Hieght, Widht, Backgroun, Paddig
- ✅ **Texture Paths** - Verifies `TexturePath: "..."` and `Background: "..."` assets exist

## Observed UI Rules (from `data/assets/*.ui`)

- Statements are terminated by `;` unless they are element blocks like `Group { ... }` or `$C.@Template { ... }`
- Imports use `$Alias = "Relative/Path.ui";`
- Assignments use `@Name = ...;` (or `$Alias = ...;` for imports)
- Property assignments inside blocks use `Property: Value;`
- Spread syntax uses `...@Template` or `...$Alias.@Template` inside tuples or as `...$Alias.@Template;`
- IDs use `#ElementId` and are distinct from hex color tokens
- Block comments use `/* ... */` and line comments use `// ...`

## Example Output

```
✅ All UI files are valid!
```

Or on error:

```
❌ Found 2 validation error(s):

  VexDungeonSummary.ui:15 - Unbalanced braces: 1 unclosed
  VexCommon.ui:42 - Malformed color code: #12 (must be #RGB or #RRGGBB)
```

## Adding Custom Checks

Edit `tools/ui-validator/src/main/java/MBRound18/hytale/tools/UiValidator.java` and add validation methods. Tests go in `src/test/java/`.

## Import/Asset Roots

By default, imports and textures are resolved relative to the scan directory. You can pass extra roots:

```bash
./gradlew validateUi --args='--import-root /path/to/assets --asset-root /path/to/textures'
```

## Core UI Rules

You can optionally point the validator at a core UI root to enforce only node types and properties that appear in base game UI files:

```bash
./gradlew validateUi --args='--core-ui-root /mnt/c/Users/micha/AppData/Roaming/Hytale/install/release/package/game/latest/Client/Data'
```

## Notes

- Template instantiation `$V.@Property { }` is **valid** syntax
- Element IDs like `#VexTitle` are not confused with color codes
- Import paths are resolved relative to the UI file location
