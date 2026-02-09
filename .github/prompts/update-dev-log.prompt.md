---
name: update-dev-log
description: "Update dev logs from recent git history."
argument-hint: "Optional focus area or title"
agent: agent
---

You are updating this repo's dev log based on recent git history.

Requirements:

- Dev logs live in packages/ui-frontdoor/public/dev/logs and are indexed by packages/ui-frontdoor/public/dev/logs/index.json (most recent first).
- Each log file uses the existing frontmatter format (createdAt, updatedAt, title, tags, commit).
- Use git history to ensure any missed commits are logged.
- Include commit metadata (short hash, date) to determine what needs logging.

Steps:

1. Read packages/ui-frontdoor/public/dev/logs/index.json and determine the most recent log date (from filename or frontmatter).
2. Scan recent commits since that date using git log. Capture short hash, date, subject.
3. Check if those commits are already covered (commit field or mentioned) in existing logs.
4. For any missing commits, create a new log file named YYYY-MM-DD-short-title.md (today's date unless a better date fits the commit history).
5. Write a concise dev log with:
   - Title
   - 1–2 sentence summary
   - Highlights list (bulleted)
   - Notes (optional)
6. Update packages/ui-frontdoor/public/dev/logs/index.json to add the new log at the top.

Output should be only the updates needed—avoid touching unrelated files.
