const pages = [
  { title: "Core Rules", file: "core-rules.md" },
  { title: "Overview", file: "rules.md" },
  { title: "Syntax", file: "syntax.md" },
  { title: "Cheat Sheet", file: "cheat-sheet.md" },
  { title: "Patterns", file: "patterns.md" },
  { title: "Validator", file: "validator.md" },
];

const converter = new showdown.Converter({
  tables: true,
  strikethrough: true,
  ghCodeBlocks: true,
});

const nav = document.getElementById("nav");
const output = document.getElementById("output");

function setActive(link) {
  for (const a of nav.querySelectorAll("a")) {
    a.classList.toggle("active", a === link);
  }
}

async function loadPage(file, link) {
  const res = await fetch(file);
  const md = await res.text();
  output.innerHTML = converter.makeHtml(md);
  if (link) setActive(link);
}

for (const page of pages) {
  const a = document.createElement("a");
  a.href = `#${page.file}`;
  a.textContent = page.title;
  a.addEventListener("click", (e) => {
    e.preventDefault();
    loadPage(page.file, a);
  });
  nav.appendChild(a);
}

const initial = pages[0];
loadPage(initial.file, nav.querySelector("a"));
