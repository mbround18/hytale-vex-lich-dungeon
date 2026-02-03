use crate::java::{class_name_from_rel_path, package_to_path};
use crate::util::normalize_path;
use anyhow::{Context, Result};
use std::collections::BTreeMap;
use std::fs;
use std::path::Path;
use walkdir::WalkDir;

pub fn generate_lang_classes(
    lang_root: &Path,
    java_out: &Path,
    java_package: &str,
    override_file: Option<&Path>,
    override_class_name: Option<&str>,
) -> Result<usize> {
    let lang_root = normalize_path(lang_root)?;
    let override_file = override_file.map(|path| {
        let candidate = if path.is_absolute() {
            path.to_path_buf()
        } else {
            lang_root.join(path)
        };
        normalize_path(&candidate).unwrap_or(candidate)
    });
    let mut generated = 0usize;

    for entry in WalkDir::new(&lang_root).into_iter().filter_map(Result::ok) {
        if !entry.file_type().is_file() {
            continue;
        }
        if entry.path().extension().and_then(|s| s.to_str()) != Some("lang") {
            continue;
        }
        let rel = entry
            .path()
            .strip_prefix(&lang_root)
            .with_context(|| format!("{} not under lang root {}", entry.path().display(), lang_root.display()))?;
        let keys = parse_lang_file(entry.path())?;
        let class_name = if override_file
            .as_ref()
            .map(|path| path == entry.path())
            .unwrap_or(false)
        {
            override_class_name.unwrap_or("ServerLang").to_string()
        } else if matches!(
            rel.file_name().and_then(|s| s.to_str()),
            Some("server.lang") | Some("mbround18_custom.lang")
        ) {
            "ServerLang".to_string()
        } else {
            let mut base = class_name_from_rel_path(rel);
            if base.ends_with("Ui") {
                base.truncate(base.len() - 2);
            }
            format!("{}Lang", base)
        };
        let java_source = generate_lang_class(java_package, &class_name, &keys);
        let out_path = java_out
            .join(package_to_path(java_package))
            .join(format!("{}.java", class_name));
        if let Some(parent) = out_path.parent() {
            fs::create_dir_all(parent)?;
        }
        fs::write(&out_path, java_source)
            .with_context(|| format!("write {}", out_path.display()))?;
        generated += 1;
    }

    Ok(generated)
}

fn parse_lang_file(path: &Path) -> Result<Vec<(String, String)>> {
    let content = fs::read_to_string(path).with_context(|| format!("read {}", path.display()))?;
    let mut entries = Vec::new();
    for line in content.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('#') || trimmed.starts_with("//") {
            continue;
        }
        let Some((key, _value)) = trimmed.split_once('=') else {
            continue;
        };
        let mut key = key.trim().to_string();
        if !key.is_empty() {
            if !key.starts_with("server.") {
                key = format!("server.{}", key);
            }
            entries.push((key.clone(), key));
        }
    }
    Ok(entries)
}

fn generate_lang_class(package: &str, class_name: &str, keys: &[(String, String)]) -> String {
    let mut fields = BTreeMap::new();
    for (key, _) in keys {
        let mut name = field_name_from_key(key);
        if fields.contains_key(&name) {
            let mut i = 2;
            while fields.contains_key(&format!("{}{}", name, i)) {
                i += 1;
            }
            name = format!("{}{}", name, i);
        }
        fields.insert(name, key.clone());
    }

    let mut out = String::new();
    out.push_str("package ");
    out.push_str(package);
    out.push_str(";\n\n");
    out.push_str("import com.hypixel.hytale.server.core.Message;\n\n");
    out.push_str("public final class ");
    out.push_str(class_name);
    out.push_str(" {\n");
    for (name, key) in fields {
        out.push_str("  public static final Message ");
        out.push_str(&name);
        out.push_str(" = Message.translation(\"");
        out.push_str(&key);
        out.push_str("\");\n");
    }
    out.push_str("}\n");
    out
}

fn field_name_from_key(key: &str) -> String {
    let key = key.strip_prefix("server.").unwrap_or(key);
    let mut parts = Vec::new();
    let mut current = String::new();
    for c in key.chars() {
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
        return "key".to_string();
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
        name = format!("key{}", name);
    }
    name
}
