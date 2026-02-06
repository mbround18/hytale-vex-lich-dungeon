import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        'vex-dark': '#1a1821',
        'vex-purple': '#8b5cf6',
        'vex-deep': '#4c1d95',
        'necro-green': '#4ade80',
        'necro-dark': '#14532d',
        'ancient-gold': '#fbbf24',
        'stone-gray': '#292524',
        'vex-surface': '#251f33',
        'vex-border': '#362b48'
      },
      fontFamily: {
        fantasy: ['"Cinzel"', 'serif'],
        body: ['"MedievalSharp"', 'cursive'],
        sans: ['"Inter"', 'sans-serif']
      },
      backgroundImage: {
        'dungeon-pattern': "url('https://www.transparenttextures.com/patterns/black-scales.png')"
      }
    }
  },
  plugins: []
} satisfies Config;
