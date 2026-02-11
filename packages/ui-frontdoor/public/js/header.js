let navConfigPromise;

function isExternalHref(href) {
  if (!href) return false;
  if (href.startsWith("mailto:") || href.startsWith("tel:")) return true;
  if (!/^https?:/i.test(href)) return false;
  try {
    const url = new URL(href, window.location.href);
    return url.origin !== window.location.origin;
  } catch (e) {
    return false;
  }
}

function setExternalTargets(root = document) {
  if (!root) return;
  const links = root.querySelectorAll("a[href]");
  links.forEach((link) => {
    const href = link.getAttribute("href");
    if (!isExternalHref(href)) return;
    link.setAttribute("target", "_blank");
    link.setAttribute("rel", "noopener");
  });
}

window.vexSetExternalTargets = setExternalTargets;
document.addEventListener("DOMContentLoaded", () => setExternalTargets());

async function loadNavigationConfig() {
  if (!navConfigPromise) {
    navConfigPromise = fetch("/navigation.json", { cache: "no-store" })
      .then((res) => (res.ok ? res.json() : null))
      .catch(() => null);
  }
  return navConfigPromise;
}

document.addEventListener("alpine:init", () => {
  Alpine.data("vexHeader", ({ variant = "home" } = {}) => ({
    variant,
    hover: null,
    navOpen: false,
    sidebarOpen: false,
    hoverTimer: null,
    links: [],
    async init() {
      const config = await loadNavigationConfig();
      if (
        config &&
        config[this.variant] &&
        Array.isArray(config[this.variant].links)
      ) {
        this.links = config[this.variant].links;
        return;
      }
      this.links = this.buildLinks();
    },
    buildLinks() {
      if (this.variant === "home") {
        return [
          { id: "lore", label: "Lore", href: "#lore", type: "text" },
          {
            id: "grimoire",
            label: "Grimoire",
            href: "/dev/",
            type: "text",
            children: [
              { label: "Developer Hub", href: "/dev/" },
              { label: "UI Grimoire", href: "/dev/ui/" },
              { label: "Hosting", href: "/dev/hosting/" },
            ],
          },
          {
            id: "dungeon",
            label: "The Dungeon",
            href: "#dungeon",
            type: "text",
          },
          { id: "rewards", label: "Rewards", href: "#rewards", type: "text" },
          {
            id: "enter",
            label: "Enter the Depths",
            href: "#download",
            type: "button",
          },
        ];
      }

      if (this.variant === "dev") {
        return [
          { id: "dungeon", label: "Dungeon", href: "/", type: "link" },
          {
            id: "overview",
            label: "Overview",
            href: "/dev/",
            type: "link",
            active: true,
          },
          {
            id: "grimoire",
            label: "UI Grimoire",
            href: "/dev/ui/",
            type: "link",
          },
          {
            id: "hosting",
            label: "Hosting",
            href: "/dev/hosting/",
            type: "link",
          },
          {
            id: "resources",
            label: "Resources",
            href: "#",
            type: "link",
            children: [
              {
                label: "Modding Template",
                href: "https://github.com/mbround18/hytale-modding-template",
              },
              {
                label: "UI VS Code Extension",
                href: "https://marketplace.visualstudio.com/items?itemName=MBRound18.hytale-ui-ultimate",
              },
            ],
          },
          {
            id: "github",
            label: "GitHub",
            href: "https://github.com/mbround18",
            type: "pill",
          },
        ];
      }

      if (this.variant === "ui") {
        return [
          { id: "dungeon", label: "Dungeon", href: "/", type: "link" },
          { id: "dev", label: "Dev Hub", href: "/dev/", type: "link" },
          {
            id: "hosting",
            label: "Hosting",
            href: "/dev/hosting/",
            type: "link",
          },
          {
            id: "grimoire",
            label: "UI Grimoire",
            href: "/dev/ui/",
            type: "link",
            active: true,
          },
          {
            id: "resources",
            label: "Resources",
            href: "#",
            type: "link",
            children: [
              {
                label: "Modding Template",
                href: "https://github.com/mbround18/hytale-modding-template",
              },
              {
                label: "UI VS Code Extension",
                href: "https://marketplace.visualstudio.com/items?itemName=MBRound18.hytale-ui-ultimate",
              },
            ],
          },
          {
            id: "github",
            label: "GitHub",
            href: "https://github.com/mbround18",
            type: "pill",
          },
        ];
      }

      if (this.variant === "hosting") {
        return [
          { id: "dungeon", label: "Dungeon", href: "/", type: "link" },
          { id: "dev", label: "Dev Hub", href: "/dev/", type: "link" },
          {
            id: "grimoire",
            label: "UI Grimoire",
            href: "/dev/ui/",
            type: "link",
          },
          {
            id: "hosting",
            label: "Hosting",
            href: "/dev/hosting/",
            type: "link",
            active: true,
          },
          {
            id: "resources",
            label: "Resources",
            href: "#",
            type: "link",
            children: [
              {
                label: "Modding Template",
                href: "https://github.com/mbround18/hytale-modding-template",
              },
              {
                label: "UI VS Code Extension",
                href: "https://marketplace.visualstudio.com/items?itemName=MBRound18.hytale-ui-ultimate",
              },
            ],
          },
          {
            id: "github",
            label: "GitHub",
            href: "https://github.com/mbround18",
            type: "pill",
          },
        ];
      }

      return [];
    },
    linkClass(link) {
      if (this.variant === "home") {
        if (link.type === "button") {
          return "px-5 py-2 rounded-lg bg-vex-purple text-black font-semibold hover:bg-[#9061f9] transition";
        }
        return "text-text-secondary hover:text-white transition-colors";
      }

      if (link.type === "pill") {
        return "bg-vex-purple/10 text-vex-purple px-4 py-2 rounded-lg border border-vex-purple/30 hover:bg-vex-purple/20 transition-all";
      }

      if (link.active) {
        return "text-white border-b-2 border-vex-purple pb-0.5";
      }

      return "text-text-secondary hover:text-white transition-colors";
    },
    isExternal(link) {
      return isExternalHref(link && link.href);
    },
    linkTarget(link) {
      return this.isExternal(link) ? "_blank" : null;
    },
    linkRel(link) {
      return this.isExternal(link) ? "noopener" : null;
    },
    setHover(id) {
      if (this.hoverTimer) {
        clearTimeout(this.hoverTimer);
        this.hoverTimer = null;
      }
      this.hover = id;
    },
    scheduleHoverClear() {
      if (this.hoverTimer) {
        clearTimeout(this.hoverTimer);
      }
      this.hoverTimer = setTimeout(() => {
        this.hover = null;
      }, 120);
    },
    mobileLinkClass(link) {
      if (this.variant === "home") {
        if (link.type === "button") {
          return "block w-full text-center px-4 py-2 rounded-lg bg-vex-purple text-black font-semibold hover:bg-[#9061f9] transition";
        }
        return "block text-text-secondary hover:text-white transition text-base";
      }

      if (link.type === "pill") {
        return "block w-full text-center bg-vex-purple/10 text-vex-purple px-4 py-2 rounded-lg border border-vex-purple/30 hover:bg-vex-purple/20 transition-all";
      }

      if (link.active) {
        return "block text-white font-semibold";
      }

      return "block text-text-secondary hover:text-white transition-colors";
    },
    dropdownClass() {
      return "absolute left-1/2 -translate-x-1/2 top-full mt-3 w-56 bg-vex-surface/95 border border-vex-border text-text-secondary text-sm rounded-lg px-3 py-2 shadow-[0_10px_30px_rgba(0,0,0,0.4)]";
    },
    tooltipClass() {
      return "absolute left-1/2 -translate-x-1/2 top-full mt-3 w-56 bg-vex-dark/95 border border-vex-purple/40 text-slate-200 text-sm rounded-lg px-3 py-2 opacity-100 shadow-[0_10px_30px_rgba(0,0,0,0.4)]";
    },
  }));
});
