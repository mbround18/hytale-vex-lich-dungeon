export type NavChild = {
  label: string;
  href: string;
  description?: string;
};

export type NavSection = {
  id: string;
  label: string;
  links: NavChild[];
};

export const primaryAction: NavChild = {
  label: 'Enter the Depths',
  href: '/#download'
};

export const navSections: NavSection[] = [
  {
    id: 'explore',
    label: 'Explore',
    links: [
      { label: 'Lore', href: '/#lore' },
      { label: 'The Dungeon', href: '/#dungeon' },
      { label: 'Broadcasts', href: '/#videos' },
      { label: 'Join', href: '/#download' }
    ]
  },
  {
    id: 'dev',
    label: 'Developer Hub',
    links: [
      { label: 'Overview', href: '/dev', description: 'Dev portal landing page.' },
      { label: 'Dev Logs', href: '/dev/logs', description: 'Chronological build notes.' },
      { label: 'Hosting', href: '/dev/hosting', description: 'Deployment and runbook notes.' },
      { label: 'UI Cheat Sheet', href: '/dev/ui/cheat-sheet.md', description: 'Quick reference for UI syntax.' },
      { label: 'UI Core Rules', href: '/dev/ui/core-rules.md', description: 'Foundational UI rules and guardrails.' },
      { label: 'UI Patterns', href: '/dev/ui/patterns.md', description: 'Reusable patterns and layouts.' }
    ]
  }
];
