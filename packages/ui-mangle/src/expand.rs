use crate::model::{BodyItem, MacroDef, MacroRegistry, Node, NodeKind};
use crate::parser::parse_param_assignment;
use crate::util::{is_ident_char, parse_ident_at};
use anyhow::Result;
use std::collections::{HashMap, HashSet};
use std::path::Path;

pub fn expand_items(
    items: Vec<BodyItem>,
    imports: &HashMap<String, std::path::PathBuf>,
    registry: &mut MacroRegistry,
    params: Option<&HashMap<String, String>>,
) -> Result<Vec<BodyItem>> {
    let empty_params = HashMap::new();
    let params_ref = params.unwrap_or(&empty_params);
    let mut out = Vec::new();
    for item in items {
        match item {
            BodyItem::Property(text) => {
                if parse_param_assignment(&text).is_some() {
                    out.push(BodyItem::Property(text));
                } else {
                    out.push(BodyItem::Property(replace_params_with_imports(
                        &text, params_ref, imports, registry,
                    )));
                }
            }
            BodyItem::Child(node) => {
                let expanded = expand_node(node, imports, registry, Some(params_ref))?;
                out.push(BodyItem::Child(expanded));
            }
        }
    }
    Ok(out)
}

fn expand_node(
    mut node: Node,
    imports: &HashMap<String, std::path::PathBuf>,
    registry: &mut MacroRegistry,
    params: Option<&HashMap<String, String>>,
) -> Result<Node> {
    match node.kind.clone() {
        NodeKind::MacroCall { prefix, name } => {
            let Some(import_path) = imports.get(&prefix) else {
                let mut unresolved = node.clone();
                if let Some(params) = params {
                    apply_params_to_items(&mut unresolved.items, params);
                }
                return Ok(unresolved);
            };

            let macro_def = resolve_macro_def(import_path, &name, registry)?;
            let Some(macro_def) = macro_def else {
                let mut unresolved = node.clone();
                if let Some(params) = params {
                    apply_params_to_items(&mut unresolved.items, params);
                }
                return Ok(unresolved);
            };

            let mut param_map = get_file_constants(import_path, registry);
            let constant_keys: HashSet<String> = param_map.keys().cloned().collect();
            param_map.insert("__alias".to_string(), prefix.clone());
            param_map.extend(macro_def.defaults.clone());
            if let Some(parent_params) = params
                && parent_params
                    .get("__warn_duplicates")
                    .map(|v| v == "true")
                    .unwrap_or(false)
            {
                param_map.insert("__warn_duplicates".to_string(), "true".to_string());
            }
            let mut call_items = node.items;
            if let Some(parent_params) = params {
                apply_params_to_items(&mut call_items, parent_params);
            }
            extract_param_overrides(&mut call_items, &mut param_map);

            let mut template_items = macro_def.items.clone();
            template_items = expand_items(template_items, imports, registry, Some(&param_map))?;
            let mut appended_items = expand_items(call_items, imports, registry, Some(&param_map))?;

            template_items.append(&mut appended_items);

            node.kind = NodeKind::Normal;
            node.type_name = macro_def.type_name;
            node.id = node.id.or(macro_def.id);
            node.items = template_items;
            qualify_spread_alias(&mut node.items, &prefix, &constant_keys);
            dedupe_node_properties(&mut node, params);
            reorder_node_items(&mut node);
            Ok(node)
        }
        NodeKind::Normal => {
            node.items = expand_items(node.items, imports, registry, params)?;
            dedupe_node_properties(&mut node, params);
            reorder_node_items(&mut node);
            Ok(node)
        }
    }
}

fn resolve_macro_def(
    path: &Path,
    name: &str,
    registry: &mut MacroRegistry,
) -> Result<Option<MacroDef>> {
    if !registry.files.contains_key(path) {
        if path.exists() {
            let _ = crate::parser::parse_ui_file(path, registry)?;
        } else {
            return Ok(None);
        }
    }
    Ok(registry
        .files
        .get(path)
        .and_then(|m| m.macros.get(name).cloned()))
}

fn get_file_constants(path: &Path, registry: &mut MacroRegistry) -> HashMap<String, String> {
    if !registry.files.contains_key(path) && path.exists() {
        let _ = crate::parser::parse_ui_file(path, registry);
    }
    let raw = registry
        .files
        .get(path)
        .map(|m| m.constants.clone())
        .unwrap_or_default();
    resolve_constants(&raw)
}

fn resolve_constants(raw: &HashMap<String, String>) -> HashMap<String, String> {
    let mut resolved = HashMap::new();
    for (k, v) in raw {
        resolved.insert(k.clone(), resolve_value(v, raw));
    }
    resolved
}

fn extract_param_overrides(items: &mut Vec<BodyItem>, params: &mut HashMap<String, String>) {
    let mut retained = Vec::with_capacity(items.len());
    for item in items.drain(..) {
        match item {
            BodyItem::Property(text) => {
                if let Some((name, value)) = parse_param_assignment(&text) {
                    params.insert(name, value);
                } else {
                    retained.push(BodyItem::Property(text));
                }
            }
            BodyItem::Child(node) => retained.push(BodyItem::Child(node)),
        }
    }
    *items = retained;
}

fn apply_params_to_items(items: &mut [BodyItem], params: &HashMap<String, String>) {
    for item in items.iter_mut() {
        match item {
            BodyItem::Property(text) => {
                if parse_param_assignment(text).is_none() {
                    *text = replace_params(text, params);
                }
            }
            BodyItem::Child(node) => {
                for child_item in node.items.iter_mut() {
                    match child_item {
                        BodyItem::Property(text) => {
                            if parse_param_assignment(text).is_none() {
                                *text = replace_params(text, params);
                            }
                        }
                        BodyItem::Child(child_node) => {
                            apply_params_to_items(&mut child_node.items, params);
                        }
                    }
                }
            }
        }
    }
}

fn dedupe_node_properties(node: &mut Node, params: Option<&HashMap<String, String>>) {
    let mut seen = HashSet::new();
    let mut kept = Vec::with_capacity(node.items.len());
    let warn = params
        .and_then(|map| map.get("__warn_duplicates"))
        .map(|v| v == "true")
        .unwrap_or(false);
    for item in node.items.drain(..).rev() {
        match item {
            BodyItem::Property(text) => {
                if let Some(key) = property_key(&text) {
                    if !seen.contains(&key) {
                        seen.insert(key);
                        kept.push(BodyItem::Property(text));
                    } else if warn {
                        eprintln!(
                            "ui-mangle warning: duplicate property '{}' in node '{}'",
                            key,
                            node.id.clone().unwrap_or_else(|| node.type_name.clone())
                        );
                    }
                } else {
                    kept.push(BodyItem::Property(text));
                }
            }
            BodyItem::Child(child) => kept.push(BodyItem::Child(child)),
        }
    }
    kept.reverse();
    node.items = kept;
}

fn reorder_node_items(node: &mut Node) {
    let mut props = Vec::new();
    let mut children = Vec::new();
    for item in node.items.drain(..) {
        match item {
            BodyItem::Property(text) => props.push(BodyItem::Property(text)),
            BodyItem::Child(child) => children.push(BodyItem::Child(child)),
        }
    }
    props.extend(children);
    node.items = props;
}

fn qualify_spread_alias(items: &mut [BodyItem], alias: &str, constants: &HashSet<String>) {
    for item in items {
        match item {
            BodyItem::Property(text) => {
                *text = qualify_spread_in_text(text, alias, constants);
            }
            BodyItem::Child(child) => {
                qualify_spread_alias(&mut child.items, alias, constants);
            }
        }
    }
}

fn qualify_spread_in_text(text: &str, alias: &str, constants: &HashSet<String>) -> String {
    let bytes = text.as_bytes();
    let mut out = String::with_capacity(text.len());
    let mut i = 0usize;
    while i < bytes.len() {
        if i + 3 < bytes.len()
            && bytes[i] == b'.'
            && bytes[i + 1] == b'.'
            && bytes[i + 2] == b'.'
            && bytes[i + 3] == b'@'
            && let Some((name, end)) = parse_ident_at(bytes, i + 4)
            && constants.contains(name)
        {
            out.push_str("...");
            out.push('$');
            out.push_str(alias);
            out.push_str(".@");
            out.push_str(name);
            i = end;
            continue;
        }
        out.push(bytes[i] as char);
        i += 1;
    }
    out
}

fn property_key(text: &str) -> Option<String> {
    let trimmed = text.trim();
    if trimmed.is_empty() || trimmed.starts_with('@') {
        return None;
    }
    let iter = trimmed.chars().peekable();
    let mut key = String::new();
    for c in iter {
        if c == ':' {
            break;
        }
        if c.is_whitespace() {
            continue;
        }
        key.push(c);
    }
    if key.is_empty() { None } else { Some(key) }
}

fn replace_params_once(text: &str, params: &HashMap<String, String>) -> String {
    let bytes = text.as_bytes();
    let mut out = String::with_capacity(text.len());
    let mut i = 0usize;
    while i < bytes.len() {
        if bytes[i] == b'@' {
            let spread =
                i >= 3 && bytes[i - 1] == b'.' && bytes[i - 2] == b'.' && bytes[i - 3] == b'.';
            if let Some((name, end)) = parse_ident_at(bytes, i + 1) {
                if spread {
                    if let Some(alias) = params.get("__alias") {
                        out.push('$');
                        out.push_str(alias);
                        out.push_str(".@");
                        out.push_str(name);
                        i = end;
                        continue;
                    }
                    out.push('@');
                    i += 1;
                    continue;
                }
                if !is_import_ref(bytes, i)
                    && let Some(value) = params.get(name)
                {
                    out.push_str(value);
                    i = end;
                    continue;
                }
            }
        }
        out.push(bytes[i] as char);
        i += 1;
    }
    out
}

fn resolve_value(value: &str, params: &HashMap<String, String>) -> String {
    let mut current = value.to_string();
    for _ in 0..8 {
        if !current.contains('@') {
            break;
        }
        let next = replace_params_once(&current, params);
        if next == current {
            break;
        }
        current = next;
    }
    current
}

fn replace_params(text: &str, params: &HashMap<String, String>) -> String {
    if !text.contains('@') {
        return text.to_string();
    }
    resolve_value(text, params)
}

fn replace_params_with_imports(
    text: &str,
    params: &HashMap<String, String>,
    imports: &HashMap<String, std::path::PathBuf>,
    registry: &mut MacroRegistry,
) -> String {
    let bytes = text.as_bytes();
    let mut out = String::with_capacity(text.len());
    let mut i = 0usize;
    while i < bytes.len() {
        if bytes[i] == b'$'
            && let Some((alias, alias_end)) = parse_ident_at(bytes, i + 1)
        {
            let j = alias_end;
            if j + 1 < bytes.len()
                && bytes[j] == b'.'
                && bytes[j + 1] == b'@'
                && let Some((name, name_end)) = parse_ident_at(bytes, j + 2)
            {
                let spread =
                    i >= 3 && bytes[i - 1] == b'.' && bytes[i - 2] == b'.' && bytes[i - 3] == b'.';
                if !spread && let Some(import_path) = imports.get(alias) {
                    let constants = get_file_constants(import_path, registry);
                    if let Some(value) = constants.get(name) {
                        let replaced = replace_params(value, &constants);
                        out.push_str(&replaced);
                        i = name_end;
                        continue;
                    }
                }
            }
        }
        if bytes[i] == b'@' {
            let spread =
                i >= 3 && bytes[i - 1] == b'.' && bytes[i - 2] == b'.' && bytes[i - 3] == b'.';
            if spread {
                out.push('@');
                i += 1;
                continue;
            }
            if let Some((name, end)) = parse_ident_at(bytes, i + 1)
                && !is_import_ref(bytes, i)
                && let Some(value) = params.get(name)
            {
                out.push_str(value);
                i = end;
                continue;
            }
        }
        out.push(bytes[i] as char);
        i += 1;
    }
    out
}

fn is_import_ref(bytes: &[u8], at: usize) -> bool {
    if at == 0 || bytes[at - 1] != b'.' {
        return false;
    }
    let mut j = at - 1;
    while j > 0 && is_ident_char(bytes[j - 1]) {
        j -= 1;
    }
    if j == 0 {
        return false;
    }
    bytes[j - 1] == b'$'
}

pub fn mangle_ids(
    node: &mut Node,
    parent: Option<String>,
    out: &mut Vec<(String, String)>,
) -> Result<()> {
    let mut current = None;
    if let Some(id) = node.id.take() {
        let id = camelize_id(&id);
        let mangled = if let Some(parent_ref) = parent.as_ref() {
            format!("{}{}", parent_ref, id)
        } else {
            id
        };
        node.id = Some(mangled.clone());
        current = Some(mangled.clone());
        out.push((mangled, node.type_name.clone()));
    }

    for item in &mut node.items {
        if let BodyItem::Child(child) = item {
            let parent_id = current.clone().or_else(|| parent.clone());
            mangle_ids(child, parent_id, out)?;
        }
    }
    Ok(())
}

fn camelize_id(input: &str) -> String {
    let mut out = String::with_capacity(input.len());
    let mut upper_next = true;
    for c in input.chars() {
        if c.is_ascii_alphanumeric() {
            if upper_next {
                out.push(c.to_ascii_uppercase());
                upper_next = false;
            } else {
                out.push(c);
            }
        } else {
            upper_next = true;
        }
    }
    if out.is_empty() {
        "Id".to_string()
    } else {
        out
    }
}

// Expose for tests
#[cfg(test)]
pub(crate) fn expand_and_render_for_tests(ui_path: &Path, registry: &mut MacroRegistry) -> String {
    let ast = crate::parser::parse_ui_file(ui_path, registry).unwrap();
    let file_params = ast.constants.clone();
    let mut expanded_items = expand_items(
        ast.items,
        &ast.imports,
        registry,
        if file_params.is_empty() {
            None
        } else {
            Some(&file_params)
        },
    )
    .unwrap();
    let mut ids = Vec::new();
    for item in &mut expanded_items {
        if let BodyItem::Child(node) = item {
            mangle_ids(node, None, &mut ids).unwrap();
        }
    }
    crate::render::render_items(&expanded_items, 0)
}
