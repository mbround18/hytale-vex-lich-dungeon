import React from 'react';

export default function VexGlobalStyles() {
  return (
    <style>{`
      @import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=IBM+Plex+Sans:wght@300;400;500;600&family=JetBrains+Mono:wght@400;600&display=swap');

      :root {
        --vex-void: #08060d;
        --vex-night: #120b1b;
        --vex-panel: #161025;
        --vex-panel-strong: #1f1633;
        --vex-border: #30223f;
        --vex-violet: #8b5cf6;
        --vex-ember: #fbbf24;
        --vex-green: #4ade80;
        --vex-rose: #f472b6;
        --vex-cyan: #38bdf8;
        --text-body: #d7c9ff;
        --text-muted: #9f8cc9;
        --grid-dot: rgba(255, 255, 255, 0.05);
      }

      body {
        font-family: 'IBM Plex Sans', sans-serif;
        background-color: var(--vex-void);
        background-image:
          radial-gradient(1100px 700px at 15% -10%, rgba(96, 43, 132, 0.22), transparent 55%),
          radial-gradient(900px 600px at 85% 5%, rgba(34, 211, 238, 0.08), transparent 55%),
          linear-gradient(160deg, #07050c 0%, #0b0912 55%, #05040a 100%);
        color: var(--text-body);
        min-height: 100vh;
      }

      .font-heading { font-family: 'Space Grotesk', sans-serif; letter-spacing: -0.01em; }
      .font-mono { font-family: 'JetBrains Mono', monospace; }

      .vex-grid {
        background-image: radial-gradient(var(--grid-dot) 0.5px, transparent 0.5px);
        background-size: 28px 28px;
      }

      ::-webkit-scrollbar { width: 6px; height: 6px; }
      ::-webkit-scrollbar-track { background: transparent; }
      ::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.15); border-radius: 999px; }

      @keyframes pulse-ring {
        0% { transform: scale(0.8); opacity: 0.5; }
        100% { transform: scale(2); opacity: 0; }
      }
      .animate-pulse-ring::before {
        content: '';
        position: absolute;
        inset: -2px;
        border-radius: 50%;
        border: 2px solid currentColor;
        animation: pulse-ring 2s cubic-bezier(0.215, 0.61, 0.355, 1) infinite;
      }
    `}</style>
  );
}
