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
      { label: 'Rewards', href: '/#rewards' },
      { label: 'Join', href: '/#download' }
    ]
  },
  {
    id: 'dev',
    label: 'Developer Hub',
    links: [
      { label: 'Overview', href: '/dev/' },
      { label: 'UI Grimoire', href: '/dev/ui/' },
      { label: 'Hosting', href: '/dev/hosting/' },
      { label: 'UI Cheat Sheet', href: '/dev/ui/cheat-sheet.md' },
      { label: 'UI Core Rules', href: '/dev/ui/core-rules.md' },
      { label: 'UI Patterns', href: '/dev/ui/patterns.md' }
    ]
  },
  {
    id: 'logs',
    label: 'Dev Logs',
    links: [
      { label: '2026-02-04 Dungeon Instance Mgmt', href: '/dev/logs/2026-02-04-dungeon-instance-management.md' },
      { label: '2026-02-04 Prefab Placement', href: '/dev/logs/2026-02-04-prefab-placement-hooks.md' },
      { label: '2026-02-02 Dev Feed Hardening', href: '/dev/logs/2026-02-02-dev-feed-and-forwarder-hardening.md' },
      { label: '2026-02-02 UI Tooling Sweep', href: '/dev/logs/2026-02-02-ui-tooling-and-friends-sweep.md' },
      { label: 'All Logs (index.json)', href: '/dev/logs/index.json' }
    ]
  }
];
