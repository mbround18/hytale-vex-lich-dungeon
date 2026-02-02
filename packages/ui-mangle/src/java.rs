use std::collections::BTreeMap;
use std::path::{Path, PathBuf};

pub fn class_name_from_rel_path(rel: &Path) -> String {
    let mut parts = Vec::new();
    for component in rel.iter() {
        let s = component.to_string_lossy();
        let s = s.trim_end_matches(".ui");
        for chunk in s.split(|c: char| !c.is_alphanumeric()) {
            if chunk.is_empty() {
                continue;
            }
            let mut chars = chunk.chars();
            if let Some(first) = chars.next() {
                let mut part = String::new();
                part.push(first.to_ascii_uppercase());
                part.extend(chars.map(|c| c.to_ascii_lowercase()));
                parts.push(part);
            }
        }
    }
    if parts.is_empty() {
        return "Ui".to_string();
    }
    let mut name = parts.join("");
    if name.chars().next().map(|c| c.is_ascii_digit()).unwrap_or(false) {
        name = format!("Ui{}", name);
    }
    name.push_str("Ui");
    name
}

pub fn path_to_slash_string(path: &Path) -> String {
    let mut out = String::new();
    for (idx, component) in path.iter().enumerate() {
        if idx > 0 {
            out.push('/');
        }
        out.push_str(&component.to_string_lossy());
    }
    out
}

pub fn package_to_path(pkg: &str) -> PathBuf {
    pkg.split('.').collect()
}

pub fn generate_java_class(
    package: &str,
    class_name: &str,
    ui_path: &str,
    ids: &[(String, String)],
) -> String {
    let mut fields = BTreeMap::new();
    for (id, ty) in ids {
        let mut value = format!("#{}", id);
        if ty == "Label" {
            value.push_str(".TextSpans");
        }
        let mut name = field_name_from_id(id);
        if fields.contains_key(&name) {
            let mut i = 2;
            while fields.contains_key(&format!("{}{}", name, i)) {
                i += 1;
            }
            name = format!("{}{}", name, i);
        }
        fields.insert(name, value);
    }

    let mut out = String::new();
    out.push_str("package ");
    out.push_str(package);
    out.push_str(";\n\n");
    out.push_str("public final class ");
    out.push_str(class_name);
    out.push_str(" {\n");
    out.push_str("  public static final String UI_PATH = \"");
    out.push_str(ui_path);
    out.push_str("\";\n\n");
    for (name, value) in fields {
        out.push_str("  public final String ");
        out.push_str(&name);
        out.push_str(" = \"");
        out.push_str(&value);
        out.push_str("\";\n");
    }
    out.push_str("}\n");
    out
}

fn field_name_from_id(id: &str) -> String {
    let mut parts = Vec::new();
    let mut current = String::new();
    for c in id.chars() {
        if c.is_alphanumeric() {
            current.push(c);
        } else if !current.is_empty() {
            parts.push(current.clone());
            current.clear();
        }
    }
    if !current.is_empty() {
        parts.push(current);
    }
    if parts.is_empty() {
        return "id".to_string();
    }
    let mut name = String::new();
    for (i, part) in parts.iter().enumerate() {
        if i == 0 {
            let mut chars = part.chars();
            if let Some(first) = chars.next() {
                name.push(first.to_ascii_lowercase());
                name.extend(chars);
            }
        } else {
            let mut chars = part.chars();
            if let Some(first) = chars.next() {
                name.push(first.to_ascii_uppercase());
                name.extend(chars);
            }
        }
    }
    if name.chars().next().map(|c| c.is_ascii_digit()).unwrap_or(false) {
        name = format!("id{}", name);
    }
    if is_java_keyword(&name) {
        name.push('_');
    }
    name
}

fn is_java_keyword(name: &str) -> bool {
    matches!(
        name,
        "abstract" | "assert" | "boolean" | "break" | "byte" | "case" | "catch" | "char" |
        "class" | "const" | "continue" | "default" | "do" | "double" | "else" | "enum" |
        "extends" | "final" | "finally" | "float" | "for" | "goto" | "if" | "implements" |
        "import" | "instanceof" | "int" | "interface" | "long" | "native" | "new" |
        "package" | "private" | "protected" | "public" | "return" | "short" | "static" |
        "strictfp" | "super" | "switch" | "synchronized" | "this" | "throw" | "throws" |
        "transient" | "try" | "void" | "volatile" | "while"
    )
}

pub fn common_alias_path(rel_to_ui_root: &Path) -> String {
    let depth = rel_to_ui_root.parent().map(|p| p.components().count()).unwrap_or(0);
    let mut out = String::new();
    if depth == 0 {
        out.push_str("Common.ui");
        return out;
    }
    for _ in 0..depth {
        out.push_str("../");
    }
    out.push_str("Common.ui");
    out
}
