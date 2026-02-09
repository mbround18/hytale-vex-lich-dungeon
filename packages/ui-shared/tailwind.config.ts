import type { Config } from "tailwindcss";

export default {
  content: [
    "./index.html",
    "./src/**/*.{ts,tsx}",
    "../ui-dashboard/src/**/*.{ts,tsx}",
    "../ui-frontdoor/src/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Core Vex palette
        vex: {
          void: "#08060d",
          night: "#120b1b",
          panel: "#161025",
          "panel-strong": "#1f1633",
          border: "#30223f",
          line: "#3a2a50",
          violet: "#8b5cf6",
          ember: "#fbbf24",
          green: "#4ade80",
          rose: "#f472b6",
          cyan: "#38bdf8",
          purple: "#8b5cf6",
          dark: "#1a1821",
          deep: "#4c1d95",
          surface: "#251f33",
        },
        // Necromancer theme
        necro: {
          green: "#4ade80",
          dark: "#14532d",
        },
        // Ancient theme
        ancient: {
          gold: "#fbbf24",
        },
        // Text colors
        text: {
          strong: "#f5f3ff",
          body: "#d7c9ff",
          muted: "#9f8cc9",
        },
        // Semantic colors
        stone: {
          gray: "#292524",
        },
      },
      fontFamily: {
        sans: ['"IBM Plex Sans"', "system-ui", "sans-serif"],
        title: ['"Space Grotesk"', "system-ui", "sans-serif"],
        mono: ['"JetBrains Mono"', "monospace"],
        fantasy: ['"Cinzel"', "serif"],
        body: ['"MedievalSharp"', "cursive"],
      },
      backgroundImage: {
        "vex-gradient":
          "radial-gradient(1200px 800px at 20% 10%, rgba(96, 43, 132, 0.25), transparent), radial-gradient(900px 700px at 90% 0%, rgba(139, 92, 246, 0.18), transparent), linear-gradient(160deg, #08060d 0%, #0e0a16 50%, #06050a 100%)",
        "dungeon-pattern":
          "url('https://www.transparenttextures.com/patterns/black-scales.png')",
      },
      spacing: {
        "safe-inset-top": "max(1rem, env(safe-area-inset-top))",
        "safe-inset-bottom": "max(1rem, env(safe-area-inset-bottom))",
      },
      boxShadow: {
        "vex-soft": "0 30px 80px rgba(6, 3, 10, 0.65)",
        "vex-glow-purple": "0 0 25px rgba(139, 92, 246, 0.45)",
        "vex-nav": "0 6px 16px rgba(139, 92, 246, 0.35)",
        "vex-card": "0 30px 70px rgba(5, 3, 8, 0.55)",
      },
    },
  },
  plugins: [],
} satisfies Config;
