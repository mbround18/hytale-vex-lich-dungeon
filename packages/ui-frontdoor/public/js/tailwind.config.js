tailwind.config = {
  theme: {
    extend: {
      colors: {
        "vex-dark": "#1a1821",
        "vex-purple": "#8b5cf6",
        "vex-purple-dim": "rgba(139, 92, 246, 0.1)",
        "vex-deep": "#4c1d95",
        "necro-green": "#4ade80",
        "necro-dark": "#14532d",
        "ancient-gold": "#fbbf24",
        "stone-gray": "#292524",
        "vex-bg": "#121212",
        "vex-surface": "#1e1e1e",
        "vex-surface-hover": "#2c2c2c",
        "vex-border": "#333333",
        "text-primary": "#f3f4f6",
        "text-secondary": "#9ca3af",
        "vex-panel": "#1e1e1e",
        "code-bg": "#282c34",
      },
      fontFamily: {
        fantasy: ["Cinzel", "serif"],
        body: ["MedievalSharp", "cursive"],
        brand: ["Cinzel", "serif"],
        sans: ["Inter", "sans-serif"],
        mono: ["Fira Code", "monospace"],
      },
      boxShadow: {
        material:
          "0 4px 6px -1px rgba(0, 0, 0, 0.5), 0 2px 4px -1px rgba(0, 0, 0, 0.3)",
      },
      backgroundImage: {
        "dungeon-pattern":
          "url('https://www.transparenttextures.com/patterns/black-scales.png')",
      },
      typography: (theme) => ({
        DEFAULT: {
          css: {
            color: theme("colors.gray.300"),
            h1: { fontFamily: "Cinzel", color: theme("colors.ancient-gold") },
            h2: {
              fontFamily: "Cinzel",
              color: theme("colors.white"),
              marginTop: "2em",
            },
            code: {
              color: theme("colors.necro-green"),
              backgroundColor: "#2d2d2d",
              padding: "0.2rem 0.4rem",
              borderRadius: "0.25rem",
              fontWeight: "400",
            },
            "code::before": { content: '""' },
            "code::after": { content: '""' },
            pre: {
              backgroundColor: theme("colors.code-bg"),
              borderRadius: "0.5rem",
              border: "1px solid #333",
            },
          },
        },
      }),
    },
  },
};
