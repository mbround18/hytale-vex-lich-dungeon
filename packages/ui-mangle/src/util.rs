use crate::args::Args;
use anyhow::Result;
use regex::Regex;
use std::collections::{BTreeSet, HashSet};
use std::ffi::OsStr;
use std::fs;
use std::path::{Component, Path, PathBuf};
use walkdir::WalkDir;

pub fn normalize_path(path: &Path) -> Result<PathBuf> {
    let abs = if path.is_absolute() {
        path.to_path_buf()
    } else {
        std::env::current_dir()?.join(path)
    };
    Ok(abs)
}

pub fn collect_ui_files(args: &Args, ui_root: &Path) -> Result<Vec<PathBuf>> {
    let mut files = BTreeSet::new();

    if let Some(java_root) = &args.java_root {
        let java_root = normalize_path(java_root)?;
        let refs = scan_java_for_ui_refs(&java_root)?;
        for ui_ref in refs {
            if let Some(path) = resolve_ui_ref(ui_root, &ui_ref) {
                if path.exists() {
                    files.insert(path);
                }
            }
        }
    }

    for entry in WalkDir::new(ui_root).into_iter().filter_map(Result::ok) {
        if entry.file_type().is_file() && entry.path().extension() == Some(OsStr::new("ui")) {
            files.insert(entry.path().to_path_buf());
        }
    }

    Ok(files.into_iter().collect())
}

fn scan_java_for_ui_refs(java_root: &Path) -> Result<HashSet<String>> {
    let mut refs = HashSet::new();
    let re = Regex::new(r#"\"([^\"]+\.ui)\""#)?;
    for entry in WalkDir::new(java_root).into_iter().filter_map(Result::ok) {
        if !entry.file_type().is_file() || entry.path().extension() != Some(OsStr::new("java")) {
            continue;
        }
        let content = fs::read_to_string(entry.path())?;
        for cap in re.captures_iter(&content) {
            if let Some(m) = cap.get(1) {
                refs.insert(m.as_str().replace('\\', "/"));
            }
        }
    }
    Ok(refs)
}

fn resolve_ui_ref(ui_root: &Path, ui_ref: &str) -> Option<PathBuf> {
    let mut rel = ui_ref;
    if let Some(idx) = ui_ref.find("Common/UI/Custom/") {
        rel = &ui_ref[idx + "Common/UI/Custom/".len()..];
    } else if let Some(idx) = ui_ref.find("UI/Custom/") {
        rel = &ui_ref[idx + "UI/Custom/".len()..];
    }
    let path = ui_root.join(rel);
    Some(path)
}

pub fn parse_ident_at(bytes: &[u8], start: usize) -> Option<(&str, usize)> {
    if start >= bytes.len() || !is_ident_start(bytes[start]) {
        return None;
    }
    let mut end = start + 1;
    while end < bytes.len() && is_ident_char(bytes[end]) {
        end += 1;
    }
    let name = std::str::from_utf8(&bytes[start..end]).ok()?;
    Some((name, end))
}

pub fn relative_path(from_dir: &Path, to: &Path) -> String {
    let from_components: Vec<Component<'_>> = from_dir.components().collect();
    let to_components: Vec<Component<'_>> = to.components().collect();

    let mut common = 0usize;
    while common < from_components.len()
        && common < to_components.len()
        && from_components[common] == to_components[common]
    {
        common += 1;
    }

    let mut parts: Vec<String> = Vec::new();
    for _ in common..from_components.len() {
        parts.push("..".to_string());
    }
    for comp in &to_components[common..] {
        parts.push(comp.as_os_str().to_string_lossy().to_string());
    }

    if parts.is_empty() {
        ".".to_string()
    } else {
        parts.join("/")
    }
}

pub fn is_ident_start(c: u8) -> bool {
    (c >= b'a' && c <= b'z') || (c >= b'A' && c <= b'Z') || c == b'_'
}

pub fn is_ident_char(c: u8) -> bool {
    is_ident_start(c) || (c >= b'0' && c <= b'9')
}
