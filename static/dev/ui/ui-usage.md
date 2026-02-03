# UI Usage (Authoring Guide)

This page focuses on how we structure and author `.ui` files in this repo. Use it alongside the syntax and validator pages.

## File Layout (This Repo)

- `shared/interfaces/src/main/resources/Common/UI/Custom/Demos/Pages/`
- `shared/interfaces/src/main/resources/Common/UI/Custom/Demos/Huds/`

UI docs are loaded via Java using paths relative to `Common/UI/Custom/`.

## Imports

```ui
$C = "../Common.ui";
$Shared = "../Shared/Widgets.ui";
```

## Templates + Composition

```ui
@TitleLabel = Label {
  Style: (FontSize: 20, RenderBold: true);
  Text: %client.ui.title;
};

Group {
  $C.@Panel {
    $C.@SectionHeader {
      $C.@TitleLabel { }
    }
  }
}
```

## Style Merge (Tuple Spread)

```ui
Label {
  Style: (...$C.@DefaultLabelStyle, FontSize: 18, RenderBold: true);
}
```

## Layout Patterns

```ui
Group {
  LayoutMode: Top;
  Padding: (Horizontal: 12, Vertical: 12);
}
```

```ui
Group {
  LayoutMode: Middle;
  Anchor: (Width: 680);
}
```

## Backgrounds and Images

```ui
Background: (TexturePath: "Common/Panel.png", Border: 12);
```

```ui
BackgroundImage {
  Image: "Common/Textures/SomeTexture.png";
}
```

> Do not use `Image` as a node type. Use `BackgroundImage` or a `Group` with `Background`.

## Runtime Rules

- Label updates must target a property: `#Title.Text`.
- Run UI commands on the world thread.
- Client UI paths must strip everything through `Custom/`.

## Usage Examples in This Repo

Page docs:
- `Common/UI/Custom/Demos/Pages/DemoTabs.ui`
- `Common/UI/Custom/Demos/Pages/DemoToast.ui`

HUD docs:
- `Common/UI/Custom/Demos/Huds/DemoHudWidgetStrip.ui`
- `Common/UI/Custom/Demos/Huds/DemoHudStats.ui`

## Learning Notes

- Small, single-purpose UI docs are easier to debug.
- Imports and templates keep files manageable.
- If a UI crashes, it is usually a path or missing asset issue.
