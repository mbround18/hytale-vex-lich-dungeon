# Core UI Rule Snapshot

This snapshot comes from scanning the base game UI files from a current client install.

## Observed Node Types (core)

- `Group`
- `Label`
- `Button`
- `ProgressBar`
- `TextButton`
- `TextField`
- `ActionButton`
- `CheckBox`
- `DropdownBox`
- `ItemGrid`
- `TabNavigation`
- `TabButton`
- `SliderNumberField`
- `NumberField`
- `ToggleButton`
- `DynamicPane`
- `ItemPreviewComponent`
- `DynamicPaneContainer`
- `SceneBlur`
- `Panel`
- `BackgroundImage`

## High-Frequency Properties (core)

- `Anchor`
- `Background`
- `LayoutMode`
- `Style`
- `Text`
- `Padding`
- `FlexWeight`
- `Visible`
- `Default` / `Hovered` / `Pressed` (style blocks)
- `LabelStyle`
- `TextColor`
- `RenderBold`
- `FontSize`

## Important Observations

- **No standalone spread statements** like `...@Template;` appear in core UI.
- **`Image` is not a node type.** Core UI uses `BackgroundImage { Image: "..." }`.
- **`Border` is not a `Group` property.** Borders appear inside `Background` or `PatchStyle` tuples.
- **List assignments are valid**, e.g. `@Hints = [ ... ];`

## How the Validator Uses This

When a core UI rule snapshot is provided, the validator rejects:

- Node types not seen in core UI
- Property names on node types that were never observed in core UI

Use this to catch runtime parser errors like:

- `Unknown node type: Image`
- `Unknown property Border on node type Group`
