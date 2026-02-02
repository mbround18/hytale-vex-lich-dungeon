use crate::expand::expand_and_render_for_tests;
use crate::java::common_alias_path;
use crate::model::{BodyItem, MacroRegistry};
use crate::parser::parse_ui_file;
use crate::render::render_items;
use crate::expand::mangle_ids;
use std::path::Path;
use std::path::PathBuf;
use std::time::{SystemTime, UNIX_EPOCH};
use std::fs;

fn temp_dir() -> PathBuf {
    let nanos = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_nanos();
    let dir = std::env::temp_dir().join(format!("ui_mangle_test_{}_{}", std::process::id(), nanos));
    fs::create_dir_all(&dir).unwrap();
    dir
}

fn write_file(path: &Path, content: &str) {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent).unwrap();
    }
    fs::write(path, content).unwrap();
}

#[test]
fn expands_macro_and_mangles_ids() {
    let dir = temp_dir();
    let macros = dir.join("Macros.ui");
    let ui = dir.join("Demo.ui");
    write_file(
        &macros,
        r#"
@Card = Group {
  @Value = "0";
  Label #Value { Text: @Value; }
};
"#,
    );
    write_file(
        &ui,
        r#"
$M = "Macros.ui";
Group #Root {
  $M.@Card #Health { @Value = "100"; }
}
"#,
    );
    let mut registry = MacroRegistry::default();
    let rendered = expand_and_render_for_tests(&ui, &mut registry);
    assert!(rendered.contains("Label #RootHealthValue"));
    assert!(rendered.contains("Text: \"100\";"));
}

#[test]
fn resolves_import_constants() {
    let dir = temp_dir();
    let friends = dir.join("Friends.ui");
    let ui = dir.join("Page.ui");
    write_file(
        &friends,
        r#"
@PanelBackground = #111111;
@Accent = @PanelBackground;
@Panel = Group {
  Background: @PanelBackground;
};
"#,
    );
    write_file(
        &ui,
        r#"
$F = "Friends.ui";
Group #Root {
  Background: $F.@Accent;
  $F.@Panel #Panel { }
}
"#,
    );
    let mut registry = MacroRegistry::default();
    let rendered = expand_and_render_for_tests(&ui, &mut registry);
    assert!(rendered.contains("Background: #111111;"));
    assert!(!rendered.contains("$F.@Accent"));
}

#[test]
fn does_not_replace_param_assignments() {
    let dir = temp_dir();
    let macros = dir.join("Macros.ui");
    let ui = dir.join("Demo.ui");
    write_file(
        &macros,
        r#"
@Spacer = Group {
  @Height = 8;
  Anchor: (Height: @Height);
};
"#,
    );
    write_file(
        &ui,
        r#"
$M = "Macros.ui";
Group #Root {
  $M.@Spacer #Space { @Height = 240; }
}
"#,
    );
    let mut registry = MacroRegistry::default();
    let rendered = expand_and_render_for_tests(&ui, &mut registry);
    assert!(rendered.contains("Anchor: (Height: 240);"));
    assert!(!rendered.contains("240 = 8"));
}

#[test]
fn injects_common_alias_when_used() {
    let dir = temp_dir();
    let ui = dir.join("Nested").join("Demo.ui");
    write_file(
        &ui,
        r#"
Group #Root {
  $C.@TextField #Field { }
}
"#,
    );
    let mut registry = MacroRegistry::default();
    let ast = parse_ui_file(&ui, &mut registry).unwrap();
    let mut expanded_items = crate::expand::expand_items(ast.items, &ast.imports, &mut registry, None).unwrap();
    let mut ids = Vec::new();
    for item in &mut expanded_items {
        if let BodyItem::Child(node) = item {
            mangle_ids(node, None, &mut ids).unwrap();
        }
    }
    let mut rendered = render_items(&expanded_items, 0);
    if rendered.contains("$C.") && !rendered.contains("$C =") {
        let rel_common = common_alias_path(Path::new("Nested/Demo.ui"));
        rendered = format!("$C = \"{}\";\n\n{}", rel_common, rendered);
    }
    assert!(rendered.starts_with("$C = \"../Common.ui\";"));
}
