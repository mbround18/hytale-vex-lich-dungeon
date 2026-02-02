use crate::model::{BodyItem, FileAst, MacroDef, MacroRegistry, Node, NodeKind};
use crate::util::{is_ident_char, is_ident_start};
use anyhow::{anyhow, Context, Result};
use regex::Regex;
use std::collections::HashMap;
use std::path::{Path, PathBuf};

pub fn parse_ui_file(path: &Path, registry: &mut MacroRegistry) -> Result<FileAst> {
    let content = std::fs::read_to_string(path)
        .with_context(|| format!("read {}", path.display()))?;
    let mut parser = ParserState::new(path.to_path_buf(), content);
    let mut items = Vec::new();
    let mut imports = HashMap::new();
    let mut constants = HashMap::new();

    while !parser.eof() {
        parser.skip_ws_and_comments();
        if parser.eof() {
            break;
        }

        if parser.peek_char() == Some('@') {
            if let Some(macro_def) = parser.try_parse_macro_def()? {
                let file_macros = registry.files.entry(path.to_path_buf()).or_default();
                file_macros.macros.insert(macro_def.0, macro_def.1);
                continue;
            }
        }

        let stmt = parser.parse_statement_or_node(&mut items)?;
        if let Some(statement) = stmt {
            if let Some((prefix, import_path)) = parse_import_statement(&statement) {
                let resolved = resolve_import_path(path, &import_path);
                imports.insert(prefix, resolved);
                continue;
            }
            if let Some((name, value)) = parse_param_assignment(&statement) {
                constants.insert(name, value);
                continue;
            }
            items.push(BodyItem::Property(statement));
        }
    }

    registry
        .files
        .entry(path.to_path_buf())
        .or_default()
        .imports
        .extend(imports.clone());
    registry
        .files
        .entry(path.to_path_buf())
        .or_default()
        .constants
        .extend(constants.clone());

    Ok(FileAst { items, imports, constants })
}

fn resolve_import_path(current_file: &Path, rel: &str) -> PathBuf {
    let base = current_file.parent().unwrap_or_else(|| Path::new("."));
    base.join(rel)
}

fn parse_import_statement(stmt: &str) -> Option<(String, String)> {
    let re = Regex::new(r#"^\s*\$(\w+)\s*=\s*\"([^\"]+)\"\s*;\s*$"#).ok()?;
    let caps = re.captures(stmt)?;
    Some((caps.get(1)?.as_str().to_string(), caps.get(2)?.as_str().to_string()))
}

pub fn parse_param_assignment(text: &str) -> Option<(String, String)> {
    let trimmed = text.trim();
    if !trimmed.starts_with('@') {
        return None;
    }
    let parts: Vec<&str> = trimmed.trim_end_matches(';').splitn(2, '=').collect();
    if parts.len() != 2 {
        return None;
    }
    let name = parts[0].trim().trim_start_matches('@').to_string();
    if name.is_empty() {
        return None;
    }
    let value = parts[1].trim().to_string();
    if value.is_empty() {
        return None;
    }
    Some((name, value))
}

struct ParserState {
    path: PathBuf,
    src: String,
    idx: usize,
}

impl ParserState {
    fn new(path: PathBuf, src: String) -> Self {
        Self { path, src, idx: 0 }
    }

    fn eof(&self) -> bool {
        self.idx >= self.src.len()
    }

    fn peek_char(&self) -> Option<char> {
        self.src[self.idx..].chars().next()
    }

    fn skip_ws_and_comments(&mut self) {
        loop {
            while let Some(c) = self.peek_char() {
                if c.is_whitespace() {
                    self.idx += c.len_utf8();
                } else {
                    break;
                }
            }
            if self.src[self.idx..].starts_with("//") {
                if let Some(pos) = self.src[self.idx..].find('\n') {
                    self.idx += pos + 1;
                } else {
                    self.idx = self.src.len();
                }
                continue;
            }
            break;
        }
    }

    fn try_parse_macro_def(&mut self) -> Result<Option<(String, MacroDef)>> {
        let start = self.idx;
        let Some('@') = self.peek_char() else {
            return Ok(None);
        };
        self.idx += 1;
        let name = self.parse_ident()?;
        self.skip_ws_and_comments();
        if self.peek_char() != Some('=') {
            self.idx = start;
            return Ok(None);
        }
        self.idx += 1;
        self.skip_ws_and_comments();
        if !self.starts_node_header() || !self.has_block_before_statement_end() {
            self.idx = start;
            return Ok(None);
        }
        let mut node = self.parse_node()?;
        let mut defaults = HashMap::new();
        extract_param_defaults(&mut node.items, &mut defaults);
        let def = MacroDef {
            type_name: node.type_name,
            id: node.id,
            items: node.items,
            defaults,
        };
        Ok(Some((name, def)))
    }

    fn starts_node_header(&self) -> bool {
        match self.peek_char() {
            Some('$') | Some('#') => true,
            Some(c) if is_ident_start(c as u8) => true,
            _ => false,
        }
    }

    fn has_block_before_statement_end(&self) -> bool {
        let mut idx = self.idx;
        let bytes = self.src.as_bytes();
        let mut in_string = false;
        while idx < bytes.len() {
            let c = bytes[idx] as char;
            if c == '"' {
                in_string = !in_string;
                idx += 1;
                continue;
            }
            if !in_string && c == ';' {
                return false;
            }
            if !in_string && c == '{' {
                return true;
            }
            idx += 1;
        }
        false
    }

    fn parse_statement_or_node(&mut self, items: &mut Vec<BodyItem>) -> Result<Option<String>> {
        self.skip_ws_and_comments();
        if self.eof() {
            return Ok(None);
        }
        match self.peek_char().unwrap() {
            '$' => {
                if self.is_import_statement() {
                    let stmt = self.read_statement()?;
                    Ok(Some(stmt))
                } else {
                    let node = self.parse_node()?;
                    items.push(BodyItem::Child(node));
                    Ok(None)
                }
            }
            '#' => {
                let node = self.parse_node()?;
                items.push(BodyItem::Child(node));
                Ok(None)
            }
            c if is_ident_start(c as u8) => {
                let saved = self.idx;
                let _ = self.parse_ident()?;
                self.skip_ws_and_comments();
                match self.peek_char() {
                    Some('#') | Some('{') => {
                        self.idx = saved;
                        let node = self.parse_node()?;
                        items.push(BodyItem::Child(node));
                        Ok(None)
                    }
                    _ => {
                        self.idx = saved;
                        let stmt = self.read_statement()?;
                        Ok(Some(stmt))
                    }
                }
            }
            _ => {
                let stmt = self.read_statement()?;
                Ok(Some(stmt))
            }
        }
    }

    fn parse_node(&mut self) -> Result<Node> {
        self.skip_ws_and_comments();
        if self.eof() {
            return Err(anyhow!("Unexpected EOF in {}", self.path.display()));
        }
        match self.peek_char().unwrap() {
            '$' => self.parse_macro_call_node(),
            '#' => self.parse_id_only_node(),
            c if is_ident_start(c as u8) => self.parse_typed_node(),
            _ => Err(anyhow!("Unexpected token in {}", self.path.display())),
        }
    }

    fn parse_macro_call_node(&mut self) -> Result<Node> {
        self.expect_char('$')?;
        let prefix = self.parse_ident()?;
        self.expect_char('.')?;
        self.expect_char('@')?;
        let name = self.parse_ident()?;
        self.skip_ws_and_comments();
        let id = if self.peek_char() == Some('#') {
            self.idx += 1;
            Some(self.parse_ident()?)
        } else {
            None
        };
        self.skip_ws_and_comments();
        self.expect_char('{')?;
        let items = self.parse_block_items()?;
        Ok(Node {
            kind: NodeKind::MacroCall { prefix, name },
            type_name: String::new(),
            id,
            items,
        })
    }

    fn parse_id_only_node(&mut self) -> Result<Node> {
        self.expect_char('#')?;
        let id = self.parse_ident()?;
        self.skip_ws_and_comments();
        self.expect_char('{')?;
        let items = self.parse_block_items()?;
        Ok(Node {
            kind: NodeKind::Normal,
            type_name: "Group".to_string(),
            id: Some(id),
            items,
        })
    }

    fn parse_typed_node(&mut self) -> Result<Node> {
        let type_name = self.parse_ident()?;
        self.skip_ws_and_comments();
        let id = if self.peek_char() == Some('#') {
            self.idx += 1;
            Some(self.parse_ident()?)
        } else {
            None
        };
        self.skip_ws_and_comments();
        self.expect_char('{')?;
        let items = self.parse_block_items()?;
        Ok(Node {
            kind: NodeKind::Normal,
            type_name,
            id,
            items,
        })
    }

    fn parse_block_items(&mut self) -> Result<Vec<BodyItem>> {
        let mut items = Vec::new();
        loop {
            self.skip_ws_and_comments();
            if self.eof() {
                return Err(anyhow!("Unclosed block in {}", self.path.display()));
            }
            if self.peek_char() == Some('}') {
                self.idx += 1;
                break;
            }
            if self.peek_char() == Some('@') {
                let stmt = self.read_statement()?;
                items.push(BodyItem::Property(stmt));
                continue;
            }
            match self.peek_char().unwrap() {
                '$' | '#' => {
                    let node = self.parse_node()?;
                    items.push(BodyItem::Child(node));
                }
                c if is_ident_start(c as u8) => {
                    let saved = self.idx;
                    let _ = self.parse_ident()?;
                    self.skip_ws_and_comments();
                    match self.peek_char() {
                        Some('#') | Some('{') => {
                            self.idx = saved;
                            let node = self.parse_node()?;
                            items.push(BodyItem::Child(node));
                        }
                        _ => {
                            self.idx = saved;
                            let stmt = self.read_statement()?;
                            items.push(BodyItem::Property(stmt));
                        }
                    }
                }
                _ => {
                    let stmt = self.read_statement()?;
                    items.push(BodyItem::Property(stmt));
                }
            }
        }
        Ok(items)
    }

    fn read_statement(&mut self) -> Result<String> {
        let start = self.idx;
        let mut in_string = false;
        while !self.eof() {
            let c = self.peek_char().unwrap();
            if c == '"' {
                in_string = !in_string;
                self.idx += 1;
                continue;
            }
            if !in_string && c == ';' {
                self.idx += 1;
                let text = self.src[start..self.idx].trim().to_string();
                return Ok(text);
            }
            self.idx += c.len_utf8();
        }
        Err(anyhow!("Unterminated statement in {}", self.path.display()))
    }

    fn parse_ident(&mut self) -> Result<String> {
        let start = self.idx;
        let mut iter = self.src[self.idx..].char_indices();
        if let Some((_, c)) = iter.next() {
            if !is_ident_start(c as u8) {
                return Err(anyhow!("Expected ident in {}", self.path.display()));
            }
            let end = start + c.len_utf8();
            self.idx = end;
        } else {
            return Err(anyhow!("Expected ident in {}", self.path.display()));
        }
        for (offset, c) in iter {
            if is_ident_char(c as u8) {
                self.idx = start + offset + c.len_utf8();
            } else {
                break;
            }
        }
        Ok(self.src[start..self.idx].to_string())
    }

    fn expect_char(&mut self, expected: char) -> Result<()> {
        self.skip_ws_and_comments();
        if self.peek_char() != Some(expected) {
            return Err(anyhow!("Expected '{}' in {}", expected, self.path.display()));
        }
        self.idx += expected.len_utf8();
        Ok(())
    }

    fn is_import_statement(&self) -> bool {
        if self.peek_char() != Some('$') {
            return false;
        }
        let mut idx = self.idx + 1;
        let bytes = self.src.as_bytes();
        if idx >= bytes.len() || !is_ident_start(bytes[idx]) {
            return false;
        }
        idx += 1;
        while idx < bytes.len() && is_ident_char(bytes[idx]) {
            idx += 1;
        }
        while idx < bytes.len() {
            let c = bytes[idx] as char;
            if c.is_whitespace() {
                idx += 1;
                continue;
            }
            return c == '=';
        }
        false
    }
}

fn extract_param_defaults(items: &mut Vec<BodyItem>, defaults: &mut HashMap<String, String>) {
    let mut retained = Vec::with_capacity(items.len());
    for item in items.drain(..) {
        match item {
            BodyItem::Property(text) => {
                if let Some((name, value)) = parse_param_assignment(&text) {
                    defaults.insert(name, value);
                } else {
                    retained.push(BodyItem::Property(text));
                }
            }
            BodyItem::Child(node) => retained.push(BodyItem::Child(node)),
        }
    }
    *items = retained;
}
