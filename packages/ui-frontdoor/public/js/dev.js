/* --- LOG LOADER CONFIG --- */
const LOG_INDEX_URL = "./logs/index.json";
const LOG_DIR = "./logs/";

/* --- LOG PARSER LOGIC --- */
const converter = new showdown.Converter({ simpleLineBreaks: true });
const container = document.getElementById("log-container");

// Regex to extract Frontmatter
const frontmatterRegex = /^---\s*([\s\S]*?)\s*---/;

function parseFrontmatter(text) {
  const match = text.match(frontmatterRegex);
  if (!match) return { attributes: {}, body: text };

  const frontmatterBlock = match[1];
  const body = text.replace(frontmatterRegex, "").trim();

  const attributes = {};
  frontmatterBlock.split("\n").forEach((line) => {
    const parts = line.split(":");
    if (parts.length >= 2) {
      const key = parts[0].trim();
      let value = parts.slice(1).join(":").trim();
      // Remove quotes if present
      if (value.startsWith('"') && value.endsWith('"'))
        value = value.slice(1, -1);
      // Parse arrays [a, b]
      if (value.startsWith("[") && value.endsWith("]")) {
        value = value
          .slice(1, -1)
          .split(",")
          .map((s) => s.trim());
      }
      attributes[key] = value;
    }
  });

  return { attributes, body };
}

function titleFromFilename(filename) {
  const base = filename.replace(/^\d{4}-\d{2}-\d{2}-/, "").replace(/\.md$/, "");
  if (!base) return "Untitled Log";
  return base
    .split("-")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function normalizeLog(log, filename) {
  const dateMatch = filename.match(/^(\d{4}-\d{2}-\d{2})/);
  const fallbackDate = dateMatch ? dateMatch[1] : null;
  const attributes = { ...log.attributes };

  attributes.title = attributes.title || titleFromFilename(filename);
  attributes.createdAt = attributes.createdAt || fallbackDate;
  attributes.updatedAt = attributes.updatedAt || attributes.createdAt;

  if (typeof attributes.tags === "string") {
    attributes.tags = attributes.tags.split(",").map((tag) => tag.trim());
  }

  return { ...log, attributes, filename };
}

async function fetchLogIndex() {
  const response = await fetch(LOG_INDEX_URL, { cache: "no-store" });
  if (!response.ok) {
    throw new Error(`Failed to load log index (${response.status})`);
  }
  const data = await response.json();
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.files)) return data.files;
  if (data && Array.isArray(data.logs)) return data.logs;
  return [];
}

async function loadLogs() {
  if (!container) return;
  container.innerHTML = "";

  try {
    const files = await fetchLogIndex();
    if (!files.length) {
      container.innerHTML =
        '<div class="text-sm text-text-secondary">No logs found yet.</div>';
      return;
    }

    const parsedLogs = await Promise.all(
      files.map(async (filename) => {
        const response = await fetch(`${LOG_DIR}${filename}`, {
          cache: "no-store",
        });
        if (!response.ok) {
          throw new Error(`Failed to load log ${filename}`);
        }
        const content = await response.text();
        const parsed = parseFrontmatter(content);
        return normalizeLog(parsed, filename);
      }),
    );

    parsedLogs
      .sort(
        (a, b) =>
          new Date(b.attributes.createdAt) - new Date(a.attributes.createdAt),
      )
      .forEach((log) => {
        const article = document.createElement("article");
        article.className =
          "bg-vex-surface rounded-xl border border-vex-border p-8 shadow-material relative overflow-hidden hover:border-vex-purple/30 transition-colors";

        const date = log.attributes.createdAt
          ? new Date(log.attributes.createdAt)
          : null;
        const dateStr = date
          ? date.toLocaleDateString("en-US", {
              year: "numeric",
              month: "long",
              day: "numeric",
            })
          : "Unknown date";

        const htmlContent = converter.makeHtml(log.body);

        article.innerHTML = `
          <div class="flex items-center justify-between mb-6">
            <div class="flex items-center gap-3">
              <span class="text-xs font-mono text-necro-green border border-necro-green/20 bg-necro-green/5 px-2 py-1 rounded">
                ${dateStr}
              </span>
              ${
                log.attributes.updatedAt &&
                log.attributes.updatedAt !== log.attributes.createdAt
                  ? `<span class="text-[10px] text-text-secondary italic">Updated: ${log.attributes.updatedAt}</span>`
                  : ""
              }
            </div>
            <div class="flex gap-2">
              ${
                Array.isArray(log.attributes.tags)
                  ? log.attributes.tags
                      .map(
                        (tag) =>
                          `<span class="text-[10px] font-bold uppercase tracking-wider text-text-secondary bg-vex-bg px-2 py-1 rounded">#${tag}</span>`,
                      )
                      .join("")
                  : ""
              }
            </div>
          </div>
          <div class="log-content">
            ${htmlContent}
          </div>
        `;
        if (window.vexSetExternalTargets) {
          window.vexSetExternalTargets(article);
        }
        container.appendChild(article);
      });
  } catch (error) {
    container.innerHTML =
      '<div class="text-sm text-text-secondary">Failed to load dev logs.</div>';
    console.error(error);
  }
}

async function loadPermissions() {
  const permissionsContainer = document.getElementById("permissions-container");
  if (!permissionsContainer) return;

  try {
    const response = await fetch("/permissions.json", { cache: "no-store" });
    if (!response.ok) {
      throw new Error(`Failed to load permissions (${response.status})`);
    }
    const permissions = await response.json();
    if (!Array.isArray(permissions) || permissions.length === 0) {
      permissionsContainer.innerHTML =
        '<div class="text-xs text-text-secondary">No permissions listed yet.</div>';
      return;
    }

    const sorted = permissions.slice().sort((a, b) => {
      const pluginA = (a.plugin || "").toLowerCase();
      const pluginB = (b.plugin || "").toLowerCase();
      if (pluginA !== pluginB) return pluginA.localeCompare(pluginB);
      return (a.permission || "").localeCompare(b.permission || "");
    });

    const html = sorted
      .map((entry) => {
        const plugin = entry.plugin || "Unknown";
        const permission = entry.permission || "unknown.permission";
        const command = entry.command || "—";
        const description = entry.description || "Permission required";
        return `
          <div class="rounded-lg border border-vex-border/60 bg-vex-surface/40 p-3 md:p-4">
            <div class="grid gap-2 md:grid-cols-12 md:gap-4">
              <div class="md:col-span-3">
                <div class="text-[10px] uppercase tracking-[0.2em] text-text-secondary font-mono md:hidden">Plugin</div>
                <div class="text-sm font-semibold text-white">${plugin}</div>
              </div>
              <div class="md:col-span-3">
                <div class="text-[10px] uppercase tracking-[0.2em] text-text-secondary font-mono md:hidden">Permission</div>
                <div class="font-mono text-[11px] text-necro-green">${permission}</div>
              </div>
              <div class="md:col-span-3">
                <div class="text-[10px] uppercase tracking-[0.2em] text-text-secondary font-mono md:hidden">Command</div>
                <div class="font-mono text-[11px] text-ancient-gold">${command}</div>
              </div>
              <div class="md:col-span-3">
                <div class="text-[10px] uppercase tracking-[0.2em] text-text-secondary font-mono md:hidden">Description</div>
                <div class="text-[11px] text-text-secondary leading-relaxed">${description}</div>
              </div>
            </div>
          </div>
        `;
      })
      .join("");

    permissionsContainer.innerHTML = html;
  } catch (error) {
    permissionsContainer.innerHTML =
      '<div class="text-xs text-text-secondary">Failed to load permissions.</div>';
    console.error(error);
  }
}

async function fetchLatestRelease(repo) {
  const response = await fetch(
    `https://api.github.com/repos/${repo}/releases/latest`,
    {
      cache: "no-store",
    },
  );
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Failed to load release for ${repo} (${response.status})`);
  }
  return response.json();
}

function formatDate(isoDate) {
  if (!isoDate) return "Unknown date";
  const date = new Date(isoDate);
  if (Number.isNaN(date.getTime())) return "Unknown date";
  return date.toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

async function loadReleaseCard(container) {
  const repo = container.getAttribute("data-repo");
  if (!repo) return;

  try {
    const release = await fetchLatestRelease(repo);
    if (!release) {
      container.innerHTML =
        '<div class="text-[11px] text-text-secondary">No releases published yet.</div>';
      return;
    }

    const name = release.name || release.tag_name || "Latest release";
    const published = formatDate(release.published_at);
    const url = release.html_url || `https://github.com/${repo}/releases`;
    const notes = release.body ? release.body.split("\n")[0] : "";

    container.innerHTML = `
      <div class="flex items-center justify-between gap-3">
        <div class="text-[11px] font-semibold text-white">${name}</div>
        <a href="${url}" target="_blank" class="text-[11px] text-ancient-gold hover:text-white transition">Open</a>
      </div>
      <div class="text-[10px] text-text-secondary mt-1">Published ${published}</div>
      ${notes ? `<div class="text-[10px] text-text-secondary mt-2 line-clamp-2">${notes}</div>` : ""}
    `;
  } catch (error) {
    container.innerHTML =
      '<div class="text-[11px] text-text-secondary">Unable to load releases right now.</div>';
    console.error(error);
  }
}

async function loadReleaseCards() {
  const releaseContainers = document.querySelectorAll("[data-repo]");
  if (!releaseContainers.length) return;
  releaseContainers.forEach((container) => loadReleaseCard(container));
}

// Initialize
loadLogs();
loadPermissions();
loadReleaseCards();
