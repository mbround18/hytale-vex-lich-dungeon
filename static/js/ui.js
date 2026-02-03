/* --- Configuration --- */
const pages = [
  { title: "Core Rules", file: "core-rules.md", icon: "📘" },
  { title: "Overview", file: "rules.md", icon: "📜" },
  { title: "UI Usage", file: "ui-usage.md", icon: "🧭" },
  { title: "Java Usage", file: "java-usage.md", icon: "☕" },
  { title: "Gallery", file: "gallery.md", icon: "🖼️" },
  { title: "Syntax", file: "syntax.md", icon: "🔮" },
  { title: "Cheat Sheet", file: "cheat-sheet.md", icon: "⚡" },
  { title: "Patterns", file: "patterns.md", icon: "🧩" },
  { title: "Validator", file: "validator.md", icon: "✅" },
];

/* --- Showdown Setup --- */
const converter = new showdown.Converter({
  tables: true,
  strikethrough: true,
  ghCodeBlocks: true,
  tasklists: true,
});

const nav = document.getElementById("nav");
const output = document.getElementById("output");
const fileSet = new Set(pages.map((page) => page.file));
let ignoreNextHash = false;

/* --- UI Functions --- */
function setActive(link) {
  // Reset all
  nav.querySelectorAll("a").forEach((a) => {
    a.classList.remove(
      "bg-vex-purple-dim",
      "text-necro-green",
      "border-l-4",
      "border-necro-green",
    );
    a.classList.add(
      "text-gray-400",
      "border-l-4",
      "border-transparent",
      "hover:text-white",
      "hover:bg-white/5",
    );
  });

  // Set active
  if (link) {
    link.classList.remove(
      "text-gray-400",
      "border-transparent",
      "hover:text-white",
      "hover:bg-white/5",
    );
    link.classList.add(
      "bg-vex-purple-dim",
      "text-necro-green",
      "border-l-4",
      "border-necro-green",
    );
  }
}

/* --- Content Loading --- */
async function loadPage(file, link) {
  if (link) setActive(link);

  try {
    const res = await fetch(`/dev/ui/${file}`);
    if (!res.ok) throw new Error("File not found");
    const md = await res.text();
    output.innerHTML = converter.makeHtml(md);
  } catch (e) {
    console.warn(`Could not fetch ${file}. Loading mock data for preview.`);
    // Fallback for when files aren't present
    const mockContent = getMockContent(file);
    output.innerHTML = converter.makeHtml(mockContent);
  }

  if (window.vexSetExternalTargets) {
    window.vexSetExternalTargets(output);
  }

  // Apply Syntax Highlighting
  output.querySelectorAll("pre code").forEach((block) => {
    // If no class is set, guess SCSS as it handles the $var and { } syntax well
    if (!block.className) {
      block.classList.add("language-scss");
    }
    hljs.highlightElement(block);
  });
}

function getLinkForFile(file) {
  return nav.querySelector(`a[data-file="${file}"]`);
}

function loadFromHash() {
  const raw = window.location.hash ? window.location.hash.slice(1) : "";
  const file = decodeURIComponent(raw);
  if (fileSet.has(file)) {
    loadPage(file, getLinkForFile(file));
    return true;
  }
  return false;
}

/* --- Initialization --- */
pages.forEach((page) => {
  const a = document.createElement("a");
  a.href = `#${page.file}`;
  a.dataset.file = page.file;
  a.className =
    "flex items-center gap-3 px-4 py-3 text-sm font-medium rounded-r transition-all duration-200 border-l-4 border-transparent text-gray-400 hover:text-white hover:bg-white/5 group";

  // Icon
  const iconSpan = document.createElement("span");
  iconSpan.textContent = page.icon || "📄";
  iconSpan.className =
    "opacity-70 group-hover:opacity-100 group-hover:scale-110 transition-transform";

  const textSpan = document.createElement("span");
  textSpan.textContent = page.title;

  a.appendChild(iconSpan);
  a.appendChild(textSpan);

  a.addEventListener("click", (e) => {
    e.preventDefault();
    ignoreNextHash = true;
    window.location.hash = page.file;
    loadPage(page.file, a);
  });
  nav.appendChild(a);
});

window.addEventListener("hashchange", () => {
  if (ignoreNextHash) {
    ignoreNextHash = false;
    return;
  }
  loadFromHash();
});

// Load initial page
const initialLink = nav.querySelector("a");
if (initialLink) {
  if (!loadFromHash()) {
    loadPage(pages[0].file, initialLink);
  }
}

/* --- Mock Data (Based on provided images) --- */
function getMockContent(filename) {
  if (filename === "rules.md") {
    return `
# Hytale UI Overview

Welcome to the **Vex Dev Grimoire**, a public-facing guide to building stable Hytale UI files.

## Purpose
This documentation is the **single source of truth** for UI syntax, layout patterns, and validation rules. It is derived from the behavior seen in core client UI files to keep custom interfaces aligned with the game's parser.

## How to Use This Grimoire

### 1. Syntax Guide 🔮
Understand the fundamental structure of \`.ui\` files, including:
* Template definitions and instantiation
* Property assignments and tuples
* Valid statement boundaries and semicolons
\`\`\`
`;
  }

  if (filename === "syntax.md") {
    return `
# Syntax Guide

The Hytale UI syntax is similar to JSON with semicolon terminated blocks. The engine parser is strict.

## Example Structure
\`\`\`
@style "ui-styles" {
  "bg-primary" : #121212;
  "accent"     : #8b5cf6;
}

@template "MainPanel" {
  "width"      : 960;
  "height"     : 720;
  "background" : "bg-primary";
}
\`\`\`

> **Tip:** Use the validator before shipping. Missing semicolons cause hard crashes.
`;
  }

  if (filename === "cheat-sheet.md") {
    return `
# Quick Cheat Sheet

| Statement | Example |
|----------|---------|
| Variable | \`"color" : #ff0044;\` |
| Tuple    | \`"padding" : (12, 24);\` |
| Color    | \`#7ad4ff\` |

## Common Snippets
\`\`\`
@template "Button" {
  "width" : 200;
  "height" : 48;
  "background" : "accent";
}
\`\`\`
`;
  }

  if (filename === "patterns.md") {
    return `
# UI Patterns

## Modal Layout
Use the \`panel\` archetype for overlays:
\`\`\`
@template "Modal" {
  "anchor" : "center";
  "background" : "bg-primary";
  "border" : "accent";
}
\`\`\`

## Grid Cards
\`\`\`
@template "Card" {
  "columns" : 3;
  "gap" : 18;
}
\`\`\`
`;
  }

  if (filename === "validator.md") {
    return `
# Validator

Run the validator tool against your UI files before packing assets. It checks:
* Missing semicolons
* Unknown properties
* Invalid color types
* Duplicate template names

## Example Output
\`\`\`
[ERROR] Line 64: Unknown property "paddingX"
\`\`\`
`;
  }

  if (filename === "core-rules.md") {
    return `
# Core Rules

The ImmortalEngine UI parser is strict and deterministic. Always validate these rules:

1. **Semicolons are mandatory** for all statements.
2. **Property names are case-sensitive** and must match the engine schema.
3. **Templates must be uniquely named** across a single file.
4. **Values are strongly typed** (colors, tuples, strings).
`;
  }

  return `# Missing content\nNo data found for ${filename}.`;
}
