mod args;
mod expand;
mod java;
mod lang;
mod model;
mod parser;
mod render;
mod util;

use anyhow::{Context, Result, anyhow};
pub use args::Args;
use expand::{expand_items, mangle_ids};
use java::{
    class_name_from_rel_path, common_alias_path, generate_java_class, package_to_path,
    path_to_slash_string,
};
use lang::generate_lang_classes;
use model::{BodyItem, MacroRegistry};
use parser::parse_ui_file;
use render::render_items;
use std::collections::BTreeSet;
use std::path::Path;
use util::{collect_ui_files, normalize_path, parse_ident_at, relative_path};

pub fn run(args: Args) -> Result<()> {
    let resources_root = normalize_path(&args.resources_root)?;
    let ui_root = normalize_path(&args.ui_root)?;
    let ui_out = normalize_path(&args.ui_out)?;
    let java_out = normalize_path(&args.java_out)?;

    if ui_out.exists() {
        std::fs::remove_dir_all(&ui_out).with_context(|| format!("remove {}", ui_out.display()))?;
    }
    std::fs::create_dir_all(&ui_out).with_context(|| format!("create {}", ui_out.display()))?;

    if java_out.exists() {
        std::fs::remove_dir_all(&java_out)
            .with_context(|| format!("remove {}", java_out.display()))?;
    }
    std::fs::create_dir_all(&java_out).with_context(|| format!("create {}", java_out.display()))?;

    let ui_files = collect_ui_files(&args, &ui_root)?;
    if ui_files.is_empty() {
        return Err(anyhow!("No .ui files found under {}", ui_root.display()));
    }

    let mut registry = MacroRegistry::default();
    let mut generated = 0usize;
    let mut root_params = std::collections::HashMap::new();
    if args.warn_duplicate_properties {
        root_params.insert("__warn_duplicates".to_string(), "true".to_string());
    }

    for ui_file in ui_files {
        let raw_content = std::fs::read_to_string(&ui_file)
            .with_context(|| format!("read {}", ui_file.display()))?;
        let ast = parse_ui_file(&ui_file, &mut registry)?;
        let mut file_params = ast.constants.clone();
        if !root_params.is_empty() {
            file_params.extend(root_params.clone());
        }
        let mut expanded_items = expand_items(
            ast.items,
            &ast.imports,
            &mut registry,
            if file_params.is_empty() {
                None
            } else {
                Some(&file_params)
            },
        )?;

        let has_entry_nodes = expanded_items
            .iter()
            .any(|item| matches!(item, BodyItem::Child(_)));
        if !has_entry_nodes {
            let rel_to_resources = ui_file.strip_prefix(&resources_root).with_context(|| {
                format!(
                    "{} not under resources root {}",
                    ui_file.display(),
                    resources_root.display()
                )
            })?;
            let ui_out_path = ui_out.join(rel_to_resources);
            if let Some(parent) = ui_out_path.parent() {
                std::fs::create_dir_all(parent)?;
            }
            std::fs::write(&ui_out_path, raw_content)
                .with_context(|| format!("write {}", ui_out_path.display()))?;
            if args.verbose {
                eprintln!("write macro-only file {}", ui_file.display());
            }
            continue;
        }

        let mut ids = Vec::new();
        for item in &mut expanded_items {
            if let BodyItem::Child(node) = item {
                mangle_ids(node, None, &mut ids)?;
            }
        }

        let rel_to_resources = ui_file.strip_prefix(&resources_root).with_context(|| {
            format!(
                "{} not under resources root {}",
                ui_file.display(),
                resources_root.display()
            )
        })?;
        let rel_to_ui_root = ui_file.strip_prefix(&ui_root).with_context(|| {
            format!(
                "{} not under ui root {}",
                ui_file.display(),
                ui_root.display()
            )
        })?;
        let ui_out_path = ui_out.join(rel_to_resources);
        if let Some(parent) = ui_out_path.parent() {
            std::fs::create_dir_all(parent)?;
        }
        let mut rendered = render_items(&expanded_items, 0);
        let aliases = collect_aliases(&rendered);
        let mut header_lines = Vec::new();
        for alias in aliases {
            if alias == "C" {
                let rel_common = common_alias_path(rel_to_ui_root);
                header_lines.push(format!("$C = \"{}\";", rel_common));
                continue;
            }
            if let Some(import_path) = ast.imports.get(&alias) {
                let base_dir = ui_file.parent().unwrap_or_else(|| Path::new("."));
                let rel = relative_path(base_dir, import_path);
                header_lines.push(format!("${} = \"{}\";", alias, rel));
            }
        }
        if !header_lines.is_empty() {
            header_lines.sort();
            let header = format!("{}\n\n", header_lines.join("\n"));
            rendered = format!("{}{}", header, rendered);
        }
        std::fs::write(&ui_out_path, rendered)
            .with_context(|| format!("write {}", ui_out_path.display()))?;

        if has_entry_nodes {
            let class_name = class_name_from_rel_path(rel_to_ui_root);
            let ui_path_string = path_to_slash_string(rel_to_ui_root);
            let java_source =
                generate_java_class(&args.java_package, &class_name, &ui_path_string, &ids);
            let java_out_path = java_out
                .join(package_to_path(&args.java_package))
                .join(format!("{}.java", class_name));
            if let Some(parent) = java_out_path.parent() {
                std::fs::create_dir_all(parent)?;
            }
            std::fs::write(&java_out_path, java_source)
                .with_context(|| format!("write {}", java_out_path.display()))?;
        }
        generated += 1;
    }

    if args.verbose {
        eprintln!("generated {} UI files", generated);
    }

    if let Some(lang_root) = &args.lang_root {
        let lang_root = normalize_path(lang_root)?;
        let count = generate_lang_classes(
            &lang_root,
            &java_out,
            &args.java_package,
            args.lang_class_file.as_deref(),
            args.lang_class_name.as_deref(),
        )?;
        if args.verbose {
            eprintln!("generated {} lang classes", count);
        }
    }

    Ok(())
}

fn collect_aliases(text: &str) -> BTreeSet<String> {
    let mut aliases = BTreeSet::new();
    let bytes = text.as_bytes();
    let mut i = 0usize;
    while i < bytes.len() {
        if bytes[i] == b'$'
            && let Some((alias, end)) = parse_ident_at(bytes, i + 1)
        {
            if end < bytes.len() && bytes[end] == b'.' {
                aliases.insert(alias.to_string());
            }
            i = end;
            continue;
        }
        i += 1;
    }
    aliases
}

#[cfg(test)]
mod tests;
