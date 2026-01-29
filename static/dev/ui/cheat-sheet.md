# UI Cheat Sheet

## Common Elements
| Element | Notes |
| --- | --- |
| `Group` | Primary container for layout |
| `Label` | Text display |
| `TextButton` | Button with label |
| `Button` | Icon or square button |
| `ProgressBar` | Status/progress display |
| `TextField` | Input field |
| `CheckBox` | Toggle input |
| `DropdownBox` | Dropdown input |
| `BackgroundImage` | Full-screen or panel images |

## Common Properties
| Property | Example |
| --- | --- |
| `Anchor` | `Anchor: (Top: 10, Left: 10, Width: 200);` |
| `LayoutMode` | `LayoutMode: Top;` |
| `Background` | `Background: (TexturePath: "Common/Panel.png", Border: 12);` |
| `Padding` | `Padding: (Horizontal: 8, Vertical: 6);` |
| `Style` | `Style: (...@DefaultLabelStyle, FontSize: 18);` |
| `Text` | `Text: %client.ui.someLabel;` |
| `FlexWeight` | `FlexWeight: 1;` |
| `Visible` | `Visible: false;` |

## Color Forms
- `#RGB`, `#RGBA`, `#RRGGBB`, `#RRGGBBAA`
- Optional alpha suffix: `#000(0.5)`

## Quick Templates

```ui
@Title = Label {
  @Text = "";
  Style: (FontSize: 20, RenderBold: true);
  Text: @Text;
};
```

```ui
$C.@Title {
  @Text = "Header";
}
```

## Runtime Commands
- **Set label text via property:** `#FriendsListBody.Text`.
- **Run UI commands on the world thread** (not scheduler/ForkJoin).
- **Normalize client paths** by stripping everything through `Custom/`:
  - `Common/UI/Custom/Friends/Pages/FriendsList.ui` -> `Friends/Pages/FriendsList.ui`
  - `UI/Custom/Friends/Pages/FriendsList.ui` -> `Friends/Pages/FriendsList.ui`
  - `Custom/Friends/Pages/FriendsList.ui` -> `Friends/Pages/FriendsList.ui`

## Do / Avoid
- **Do** merge styles inside tuples: `Style: (...@DefaultLabelStyle, FontSize: 18);`
- **Avoid** standalone spread statements: `...@Template;`
- **Do** use `BackgroundImage` for images, not `Image`
- **Avoid** `Border` on `Group`; use `Background`/`PatchStyle` tuples
