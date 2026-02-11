# Vex UI Design System

A unified, redistributable design system for all Vex UI applications. Built with React, TypeScript, Tailwind CSS, and SCSS.

## Architecture

```
ui-shared/                    # Core design system & components
├── src/
│   ├── AppProvider.tsx       # Root provider (imports shared styles)
│   ├── styles/
│   │   ├── app.scss          # Main entry point (Tailwind + base styles)
│   │   ├── _base.scss        # Typography, utilities, reset
│   │   └── _tokens.scss      # Design tokens (colors, shadows, gradients)
│   ├── components/           # Reusable components
│   └── layouts/              # Layout templates
├── tailwind.config.ts        # Shared Tailwind configuration
└── package.json              # Exports all shared resources

ui-dashboard/                 # Dashboard application
├── src/
│   ├── main.tsx              # App entry (uses AppProvider)
│   ├── App.tsx               # Application root
│   ├── styles/app.scss       # Dashboard-specific overrides only
│   └── tailwind.config.ts    # Extends shared config
└── package.json

ui-frontdoor/                 # Frontdoor/homepage application
├── src/
│   ├── main.tsx              # App entry (uses AppProvider)
│   ├── App.tsx               # Application root
│   ├── styles/app.scss       # Frontdoor-specific overrides only
│   └── tailwind.config.ts    # Extends shared config
└── package.json
```

## Key Design Tokens

All tokens are defined in `ui-shared/src/styles/_tokens.scss` and exposed through Tailwind.

### Colors

**Core Palette**

- `vex-violet`: #8b5cf6 (primary accent)
- `vex-ember`: #fbbf24 (warm accent)
- `vex-green`: #4ade80 (success)
- `vex-rose`: #f472b6 (attention)
- `vex-cyan`: #38bdf8 (info)

**Backgrounds**

- `vex-void`: #08060d (darkest background)
- `vex-night`: #120b1b
- `vex-panel`: #161025
- `vex-panel-strong`: #1f1633

**Text**

- `text-strong`: #f5f3ff (headings)
- `text-body`: #d7c9ff (body text)
- `text-muted`: #9f8cc9 (secondary text)

**Theme Variants**

- Necromancer theme: `necro-green`, `necro-dark`
- Ancient theme: `ancient-gold`

### Typography

- `font-sans`: IBM Plex Sans (body text, default)
- `font-title`: Space Grotesk (headings, UI labels)
- `font-mono`: JetBrains Mono (code, data)
- `font-fantasy`: Cinzel (decorative)
- `font-body`: MedievalSharp (decorative cursive)

### Shadows & Effects

- `shadow-vex-soft`: Soft diffuse shadow
- `shadow-vex-glow-purple`: Purple glow effect
- `shadow-vex-nav`: Navigation highlight
- `shadow-vex-card`: Card emphasis

### Backgrounds

- `bg-vex-gradient`: Primary gradient (used for main bg)

## Usage

### Application Setup

Every app should wrap its root in `AppProvider` to ensure styles are loaded:

```tsx
// src/main.tsx
import React from "react";
import ReactDOM from "react-dom/client";
import AppProvider from "ui-shared/provider";
import App from "./App";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <AppProvider>
      <App />
    </AppProvider>
  </React.StrictMode>,
);
```

### Tailwind Configuration

Each app's `tailwind.config.ts` extends the shared config:

```typescript
// tailwind.config.ts
import sharedConfig from "../ui-shared/tailwind.config";

export default {
  ...sharedConfig,
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
};
```

### Using Design Tokens in Styles

In SCSS files, import tokens:

```scss
@use "ui-shared/styles/tokens" as tokens;

.my-component {
  color: tokens.$text-body;
  background: tokens.$vex-panel;
  box-shadow: tokens.$shadow-vex-card;
}
```

### Using Colors in Tailwind

All tokens are available in Tailwind classes:

```tsx
<div className="bg-vex-panel border-vex-border text-text-body">
  <h1 className="text-vex-violet font-title">Title</h1>
  <p className="text-text-muted">Subtitle</p>
</div>
```

### Accessing Shared Components

Import components from the barrel export:

```tsx
import { Badge, HeroBanner, DataTable } from "ui-shared/components";
```

Or specific components:

```tsx
import Badge from "ui-shared/components/Badge";
```

## Component Library

### Available Components

- `Badge` - Status badges with color variants
- `HeroBanner` - Large featured banner sections
- `DataTable` - Responsive data display
- `KpiCard` - Key performance indicator card
- `ArchiveCard` - Archive/history card
- `DevLogCard` - Development log entry
- `WikiCard` - Documentation card
- `EventLogItem` - Event log entry
- `TrackingCard` - Tracking/metric card
- `MetricList` - List of metrics
- `PlayerRow` - Player data row
- `IndexRow` - Index/catalog row
- `StatusPill` - Status indicator
- `SectionTitle` - Section heading
- `Section` - Content section
- `SyntaxHighlighter` - Code syntax highlighting
- `SplitPane` - Two-column layout
- `VexGlobalStyles` - Global style component (legacy, use AppProvider instead)
- `BrandHeader` - Brand/header component

### Using Components

```tsx
import { Badge, HeroBanner } from "ui-shared/components";

export function MyComponent() {
  return (
    <>
      <HeroBanner>
        <h1>Welcome</h1>
        <Badge variant="gold">Featured</Badge>
      </HeroBanner>
    </>
  );
}
```

## Guidelines

### ✅ Do's

- Use shared tokens from `ui-shared/styles/tokens.scss`
- Extend tailwind config via `sharedConfig`, don't override
- Keep component-specific styles in app-level `app.scss`
- Use SCSS for component styles, Tailwind for layout/utilities
- Import `AppProvider` in all app entry points
- Create reusable components in `ui-shared/components/`

### ❌ Don'ts

- Don't define color tokens in individual app stylesheets
- Don't duplicate component styles across apps
- Don't create separate tailwind configs unless extending shared base
- Don't import individual style files (use AppProvider instead)
- Don't inline CSS variables - use SCSS tokens instead

## Extending the Design System

### Adding a New Token

1. Define in `ui-shared/src/styles/_tokens.scss`
2. Export in `tailwind.config.ts` under `theme.extend`
3. Document in this file

### Adding a Shared Component

1. Create in `ui-shared/src/components/ComponentName.tsx`
2. Export in `ui-shared/src/components/index.ts`
3. Add to component library list above

### Application-Specific Styles

Keep only non-reusable, application-specific styles in:

- `src/styles/app.scss` (component overrides, page layouts)
- Component-level SCSS modules (if using CSS modules)

---

## File Structure Rationale

**Why AppProvider?**

- Single source of style initialization
- Prevents duplicate stylesheet imports
- Consistent across all applications

**Why split \_tokens.scss and \_base.scss?**

- `_tokens.scss`: Pure configuration, no side effects
- `_base.scss`: Actual stylesheet rules (resets, utilities, typography)

**Why extend Tailwind for colors?**

- Enables `className="text-vex-purple"` syntax
- Keeps token definitions DRY (defined once, used everywhere)
- Provides IDE autocompletion

---

## Maintenance

When updating design tokens:

1. Update `ui-shared/src/styles/_tokens.scss`
2. Update `ui-shared/tailwind.config.ts` color/shadow definitions
3. Update this documentation
4. All apps automatically pick up changes

No need to update individual app tailwind configs unless adding app-specific extensions.
