# Common Patterns

## Container with Title

```ui
$C = "../Common.ui";

$C.@Panel {
  #Title {
    $C.@Title { @Text = "My Panel"; }
  }

  #Content {
    LayoutMode: Top;
  }
}
```

## Overlay + Centered Modal

```ui
$C = "../Common.ui";

Group {
  $C.@PageOverlay { }
  LayoutMode: Middle;

  $C.@DecoratedContainer {
    Anchor: (Width: 680);
  }
}
```

## Style Merge

```ui
Label {
  Style: (...$C.@DefaultLabelStyle, FontSize: 18, RenderBold: true);
}
```

## Common Gotchas
- **Missing semicolons** after properties or assignments are the #1 crash cause.
- **Standalone spread statements** like `...@Template;` are not supported. Spread is for tuples only.
- **Template instantiation** should use `$Alias.@Template { ... }` for element bodies.
- **Image is not a node type**. Use `BackgroundImage { Image: "..." }` or a `Group` with `Background`.
- **Block comments** use `/* ... */`. Line comments use `// ...`.
- **When setting label text, use `#Id.Text`** (not `#Id`).
- **Client UI paths should not include `Common/UI/Custom/` or `UI/Custom/`.** Strip to after `Custom/`.
