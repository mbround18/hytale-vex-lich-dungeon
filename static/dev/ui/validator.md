# Validator Notes

## What It Checks
- Balanced braces `{}` and parentheses `()`
- Unterminated strings and block comments
- Missing semicolons / merged statements
- Invalid assignment targets (`@Name =` or `$Alias =`)
- Invalid property names (`Property:`)
- Import existence + unknown import aliases
- Malformed color hex (supports `#RGB/#RGBA/#RRGGBB/#RRGGBBAA`)
- Standalone spread statements (disallowed)
- Unsupported node types (ex: `Image`)
- Node-specific property validation when core rules are provided
- TexturePath / Background image resolution against asset roots

## Why This Is "Robust"
The validator rules are aligned to patterns seen in core Hytale UI files from a current client install.

## Runtime Gotchas (Not Syntax Errors)
- **Selectors must target properties** when setting label text: `#FriendsListBody.Text`.
- **Commands must run on the world thread** (not scheduler/ForkJoin).
- **Client UI paths must not include `Common/UI/Custom/` or `UI/Custom/`.** Strip to after `Custom/` (use `UiPath.normalizeForClient(...)`).

## Known Limitations
- **Import resolution** is relative to the file path. If your UI relies on runtime asset packs outside your plugin folder, the validator may flag missing imports.
- **TexturePath resolution** is validated only against provided asset roots, so missing/extra packs can still produce false positives.
- **Semantic checks** (e.g., layout rules, element-specific required props) are not enforced.

## Running
```bash
./gradlew validateUi
```
