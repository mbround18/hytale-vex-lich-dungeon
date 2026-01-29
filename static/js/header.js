document.addEventListener("alpine:init", () => {
  Alpine.data("vexHeader", ({ variant = "home" } = {}) => ({
    variant,
    hover: null,
    links: [],
    init() {
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
            tooltip: {
              title: "Development Hub",
              body: "UI docs, engine notes, and API references.",
            },
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
            href: "#",
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
            id: "api",
            label: "API Reference",
            href: "#",
            type: "link",
            active: true,
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
            href: "#",
            type: "link",
            active: true,
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
          return "px-6 py-2 rounded border-2 border-vex-purple bg-vex-deep/50 text-white text-lg font-bold hover:bg-vex-purple hover:border-necro-green hover:shadow-[0_0_15px_rgba(74,222,128,0.4)] transition duration-300";
        }
        return "text-lg text-slate-300 hover:text-necro-green transition hover:scale-105 transform";
      }

      if (link.type === "pill") {
        return "bg-vex-purple/10 text-vex-purple px-4 py-2 rounded-lg border border-vex-purple/30 hover:bg-vex-purple/20 transition-all";
      }

      if (link.active) {
        return "text-white border-b-2 border-vex-purple pb-0.5";
      }

      return "text-text-secondary hover:text-white transition-colors";
    },
    tooltipClass() {
      return "absolute left-1/2 -translate-x-1/2 top-full mt-3 w-56 bg-vex-dark/95 border border-vex-purple/40 text-slate-200 text-sm rounded-lg px-3 py-2 opacity-100 shadow-[0_10px_30px_rgba(0,0,0,0.4)]";
    },
  }));
});
