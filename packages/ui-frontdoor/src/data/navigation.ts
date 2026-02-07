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
      { label: 'Overview', href: '/dev' },
      { label: 'UI Grimoire', href: '/dev/ui' },
      { label: 'Hosting', href: '/dev/hosting' },
      { label: 'UI Cheat Sheet', href: '/dev/ui/cheat-sheet.md' },
      { label: 'UI Core Rules', href: '/dev/ui/core-rules.md' },
      { label: 'UI Patterns', href: '/dev/ui/patterns.md' }
    ]
  }
];
