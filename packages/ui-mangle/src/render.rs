use crate::model::{BodyItem, Node, NodeKind};

pub fn render_items(items: &[BodyItem], indent: usize) -> String {
    let mut out = String::new();
    for item in items {
        match item {
            BodyItem::Property(text) => {
                if !text.trim().is_empty() {
                    out.push_str(&"  ".repeat(indent));
                    out.push_str(text.trim());
                    if !text.trim().ends_with(';') {
                        out.push(';');
                    }
                    out.push('\n');
                }
            }
            BodyItem::Child(node) => {
                out.push_str(&render_node(node, indent));
            }
        }
    }
    out
}

fn render_node(node: &Node, indent: usize) -> String {
    let mut out = String::new();
    out.push_str(&"  ".repeat(indent));
    match &node.kind {
        NodeKind::Normal => out.push_str(&node.type_name),
        NodeKind::MacroCall { prefix, name } => {
            out.push('$');
            out.push_str(prefix);
            out.push('.');
            out.push('@');
            out.push_str(name);
        }
    }
    if let Some(id) = &node.id {
        out.push(' ');
        out.push('#');
        out.push_str(id);
    }
    out.push_str(" {\n");
    out.push_str(&render_items(&node.items, indent + 1));
    out.push_str(&"  ".repeat(indent));
    out.push_str("}\n");
    out
}
