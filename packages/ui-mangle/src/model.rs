use std::collections::HashMap;
use std::path::PathBuf;

#[derive(Clone, Debug)]
pub struct Node {
    pub kind: NodeKind,
    pub type_name: String,
    pub id: Option<String>,
    pub items: Vec<BodyItem>,
}

#[derive(Clone, Debug)]
pub enum NodeKind {
    Normal,
    MacroCall { prefix: String, name: String },
}

#[derive(Clone, Debug)]
pub enum BodyItem {
    Property(String),
    Child(Node),
}

#[derive(Clone, Debug)]
pub struct MacroDef {
    pub type_name: String,
    pub id: Option<String>,
    pub items: Vec<BodyItem>,
    pub defaults: HashMap<String, String>,
}

#[derive(Default, Debug)]
pub struct FileMacros {
    pub macros: HashMap<String, MacroDef>,
    pub imports: HashMap<String, PathBuf>,
    pub constants: HashMap<String, String>,
}

#[derive(Default)]
pub struct MacroRegistry {
    pub files: HashMap<PathBuf, FileMacros>,
}

#[derive(Debug)]
pub struct FileAst {
    pub items: Vec<BodyItem>,
    pub imports: HashMap<String, PathBuf>,
    pub constants: HashMap<String, String>,
}
