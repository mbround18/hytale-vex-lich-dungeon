# Syntax Cheats

## Core Statement Shapes

```ui
$Alias = "Path/To/File.ui";
@Value = 12;
@Style = (FontSize: 16, TextColor: #fff);
@Hints = [
  "One",
  "Two",
];

Group #Root {
  LayoutMode: Top;
}

@Template = Group {
  Padding: (Full: 8);
};

$C.@Template {
  Label { Text: "Hello"; }
}
```

## Properties

```ui
Property: Value;
```

Common property value shapes:

- **Tuple**: `(Left: 10, Top: 6, Width: 120)`
- **Style tuple**: `Style: (...@DefaultLabelStyle, FontSize: 18);`
- **Texture tuple**: `Background: (TexturePath: "Common/Panel.png", Border: 12);`

## Elements

```ui
ElementType #OptionalId {
  Property: Value;
}
```

Examples:

```ui
Group {
  LayoutMode: Left;
}

Label #Title {
  Text: %client.title.label;
}
```

## Template Instantiation

```ui
$C.@Panel {
  #Header {
    $C.@Title { @Text = "Hello"; }
  }
}
```

## Spread / Merge

```ui
Style: (...@DefaultLabelStyle, FontSize: 20);
Padding: (...@DefaultPadding, Horizontal: 8);
```

## Comments

```ui
// Line comment
/* Block comment */
```
