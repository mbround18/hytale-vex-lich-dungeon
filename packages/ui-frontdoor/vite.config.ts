import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";
import fs from "node:fs/promises";
import { existsSync } from "node:fs";
import { remark } from "remark";
import remarkGfm from "remark-gfm";
import remarkHtml from "remark-html";

const __dirname = path.dirname(new URL(import.meta.url).pathname);

type MarkdownIndexEntry = {
  file: string;
  title: string;
  date?: string;
  excerpt?: string;
  htmlPath: string;
  frontmatter?: Record<string, any>;
};

type MarkdownIndex = {
  entries: MarkdownIndexEntry[];
};

const markdownAssets = () => {
  let publicDir = path.resolve(__dirname, "public");
  const processor = remark().use(remarkGfm).use(remarkHtml);

  const parseFrontmatter = (content: string) => {
    const frontmatterRegex = /^---\n([\s\S]*?)\n---\n([\s\S]*)$/;
    const match = content.match(frontmatterRegex);

    if (!match) {
      return { frontmatter: {}, markdown: content };
    }

    const [, frontmatterStr, markdown] = match;
    const frontmatter: Record<string, any> = {};

    frontmatterStr.split("\n").forEach((line) => {
      const keyValue = line.match(/^(\w+):\s*(.+)$/);
      if (!keyValue) return;
      const [, key, value] = keyValue;
      if (value.startsWith("[") && value.endsWith("]")) {
        frontmatter[key] = value
          .slice(1, -1)
          .split(",")
          .map((v) => v.trim().replace(/^["']|["']$/g, ""));
      } else if (
        (value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))
      ) {
        frontmatter[key] = value.slice(1, -1);
      } else if (!isNaN(Number(value))) {
        frontmatter[key] = Number(value);
      } else if (value === "true" || value === "false") {
        frontmatter[key] = value === "true";
      } else {
        frontmatter[key] = value;
      }
    });

    return { frontmatter, markdown };
  };

  const extractTitle = (file: string, markdown: string, frontmatter: any) => {
    if (frontmatter?.title) return String(frontmatter.title);
    const lines = markdown.split(/\r?\n/).map((line) => line.trim());
    for (const line of lines) {
      if (line.startsWith("# ")) {
        return line.replace(/^#\s+/, "").trim();
      }
    }
    return file.replace(/\.md$/i, "").replace(/-/g, " ");
  };

  const extractExcerpt = (markdown: string) => {
    const lines = markdown.split(/\r?\n/).map((line) => line.trim());
    for (const line of lines) {
      if (!line) continue;
      if (line.startsWith("#")) continue;
      if (line.startsWith(">")) continue;
      return line;
    }
    return "";
  };

  const parseLogDate = (file: string) => {
    const datePart = file.split("-").slice(0, 3).join("-");
    return /^\d{4}-\d{2}-\d{2}$/.test(datePart) ? datePart : undefined;
  };

  const renderMarkdown = async (markdown: string) => {
    const file = await processor.process(markdown);
    return file.toString();
  };

  const buildEntry = (
    file: string,
    markdown: string,
    frontmatter: Record<string, any>,
    htmlBase: string,
  ): MarkdownIndexEntry => ({
    file,
    title: extractTitle(file, markdown, frontmatter),
    date: frontmatter?.createdAt ?? parseLogDate(file),
    excerpt: extractExcerpt(markdown),
    htmlPath: `${htmlBase}/${file.replace(/\.md$/i, ".html")}`,
    frontmatter,
  });

  const buildLogIndex = async (): Promise<{
    index: MarkdownIndex;
    html: Record<string, string>;
  }> => {
    const logsDir = path.join(publicDir, "dev", "logs");
    const indexPath = path.join(logsDir, "index.json");
    const rawIndex = await fs.readFile(indexPath, "utf8");
    const files = JSON.parse(rawIndex) as string[];
    const entries: MarkdownIndexEntry[] = [];
    const htmlByFile: Record<string, string> = {};

    for (const file of files) {
      if (!file.endsWith(".md")) continue;
      const sourcePath = path.join(logsDir, file);
      if (!existsSync(sourcePath)) continue;
      const raw = await fs.readFile(sourcePath, "utf8");
      const { frontmatter, markdown } = parseFrontmatter(raw);
      const entry = buildEntry(file, markdown, frontmatter, "/dev/logs");
      htmlByFile[entry.htmlPath] = await renderMarkdown(markdown);
      entries.push(entry);
    }

    return { index: { entries }, html: htmlByFile };
  };

  const buildUiIndex = async (): Promise<{
    index: MarkdownIndex;
    html: Record<string, string>;
  }> => {
    const uiDir = path.join(publicDir, "dev", "ui");
    const files = (await fs.readdir(uiDir)).filter((file) =>
      file.endsWith(".md"),
    );
    const entries: MarkdownIndexEntry[] = [];
    const htmlByFile: Record<string, string> = {};

    for (const file of files) {
      const sourcePath = path.join(uiDir, file);
      if (!existsSync(sourcePath)) continue;
      const raw = await fs.readFile(sourcePath, "utf8");
      const { frontmatter, markdown } = parseFrontmatter(raw);
      const entry = buildEntry(file, markdown, frontmatter, "/dev/ui");
      htmlByFile[entry.htmlPath] = await renderMarkdown(markdown);
      entries.push(entry);
    }

    entries.sort((a, b) => a.file.localeCompare(b.file));
    return { index: { entries }, html: htmlByFile };
  };

  return {
    name: "frontdoor-markdown-assets",
    configResolved(config: { publicDir?: string }) {
      if (config.publicDir) {
        publicDir = config.publicDir;
      }
    },
    async generateBundle() {
      const logIndex = await buildLogIndex();
      const uiIndex = await buildUiIndex();

      for (const [htmlPath, html] of Object.entries(logIndex.html)) {
        this.emitFile({
          type: "asset",
          fileName: htmlPath.replace(/^\//, ""),
          source: html,
        });
      }

      for (const [htmlPath, html] of Object.entries(uiIndex.html)) {
        this.emitFile({
          type: "asset",
          fileName: htmlPath.replace(/^\//, ""),
          source: html,
        });
      }

      this.emitFile({
        type: "asset",
        fileName: "dev/logs/index.meta.json",
        source: JSON.stringify(logIndex.index, null, 2),
      });

      this.emitFile({
        type: "asset",
        fileName: "dev/ui/index.meta.json",
        source: JSON.stringify(uiIndex.index, null, 2),
      });
    },
    configureServer(server: { middlewares: any }) {
      server.middlewares.use(async (req: any, res: any, next: any) => {
        if (!req.url) {
          next();
          return;
        }
        const url = new URL(req.url, "http://localhost");
        const pathname = url.pathname;

        try {
          if (pathname === "/dev/logs/index.meta.json") {
            const logIndex = await buildLogIndex();
            res.setHeader("Content-Type", "application/json; charset=utf-8");
            res.end(JSON.stringify(logIndex.index, null, 2));
            return;
          }

          if (pathname === "/dev/ui/index.meta.json") {
            const uiIndex = await buildUiIndex();
            res.setHeader("Content-Type", "application/json; charset=utf-8");
            res.end(JSON.stringify(uiIndex.index, null, 2));
            return;
          }

          if (pathname.startsWith("/dev/logs/") && pathname.endsWith(".html")) {
            const mdPath = pathname
              .replace(/\.html$/i, ".md")
              .replace(/^\/+/, "");
            const filePath = path.join(publicDir, mdPath);
            if (!existsSync(filePath)) {
              next();
              return;
            }
            const raw = await fs.readFile(filePath, "utf8");
            const { markdown } = parseFrontmatter(raw);
            const html = await renderMarkdown(markdown);
            res.setHeader("Content-Type", "text/html; charset=utf-8");
            res.end(html);
            return;
          }

          if (pathname.startsWith("/dev/ui/") && pathname.endsWith(".html")) {
            const mdPath = pathname
              .replace(/\.html$/i, ".md")
              .replace(/^\/+/, "");
            const filePath = path.join(publicDir, mdPath);
            if (!existsSync(filePath)) {
              next();
              return;
            }
            const raw = await fs.readFile(filePath, "utf8");
            const { markdown } = parseFrontmatter(raw);
            const html = await renderMarkdown(markdown);
            res.setHeader("Content-Type", "text/html; charset=utf-8");
            res.end(html);
            return;
          }
        } catch {
          next();
          return;
        }

        next();
      });
    },
  };
};

export default defineConfig({
  plugins: [react(), markdownAssets()],
  resolve: {
    alias: {
      "@shared": path.resolve(__dirname, "../ui-shared/src"),
    },
  },
  publicDir: path.resolve(__dirname, "public"),
  build: {
    outDir: path.resolve(__dirname, "../../dist/static"),
    emptyOutDir: false,
  },
});
