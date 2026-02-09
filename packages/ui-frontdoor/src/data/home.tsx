import React from 'react';

export type HeroAction = {
  label: string;
  href: string;
  variant: 'primary' | 'secondary';
  icon?: string;
};

export type LoreCard = {
  icon: string;
  title: string;
  description: string;
  accent: 'purple' | 'gold' | 'green';
};

export type DungeonFeature = {
  step: string;
  title: string;
  description: string;
  status?: 'active' | 'coming';
};

export type VideoCategory = 'dev' | 'trailer';

export type VideoEntry = {
  title: string;
  href: string;
  category: VideoCategory;
  summary?: string;
};

export const homeContent = {
  hero: {
    kicker: 'New Hytale Adventure Mod',
    title: ['Will You Survive', "Vex's Dungeon?"],
    subtitle:
      "The Lich King Vex has awakened. He taunts the adventurers of Hytale with a challenge: survive his trap-filled dungeon, defeat his minions, and claim the Ultimate Reward.",
    actions: [
      {
        label: 'Accept & Star Repo',
        href: 'https://github.com/mbround18/hytale-vex-lich-dungeon',
        variant: 'primary',
        icon: '⭐'
      },
      {
        label: 'Donate to Vex',
        href: 'https://ko-fi.com/mbround18',
        variant: 'secondary',
        icon: '☕'
      }
    ] satisfies HeroAction[],
    note: '*Starring the repo helps summon the dungeon faster!'
  },
  heroPortrait: {
    title: 'Vex The Lich',
    quote: '"Your souls will fuel my phylactery!"',
    bossLevel: 'Boss Level: 99',
    hint: "(Place 'ContainerPatch.png' here)"
  },
  lore: {
    id: 'lore',
    title: 'The Tale of Vex',
    paragraphs: [
      'Deep beneath the corrupted lands of Hytale, Vex the Lich has constructed a labyrinth of despair. Once a master tinkerer, his obsession with eternal life led him to fuse ancient magic with forbidden machinery.',
      (
        <>
          Now, he sits upon his throne of gears and bones, watching... waiting. He doesn&apos;t just want your life; he
          wants to test your wit. Only those who can navigate his mechanical horrors deserve the{' '}
          <span className="text-necro-green font-bold">Relics of the Old World</span>.
        </>
      )
    ] as React.ReactNode[],
    cards: [
      {
        icon: '🔮',
        title: 'Dark Magic',
        description: 'Spells that twist reality and drain your stamina.',
        accent: 'purple'
      },
      {
        icon: '⚙️',
        title: 'Clockwork Traps',
        description: 'Floors that shift and walls that crush.',
        accent: 'gold'
      },
      {
        icon: '⚗️',
        title: 'Alchemy',
        description: 'Potions that might heal you... or seal your doom.',
        accent: 'green'
      }
    ] satisfies LoreCard[]
  },
  dungeon: {
    id: 'dungeon',
    title: 'Inside the Dungeon',
    subtitle: 'Every run is different. Every room is a threat.',
    features: [
      {
        step: '1',
        title: 'Procedural Rooms',
        description: 'No two adventures are alike. Vex rearranges his lair every time you enter.',
        status: 'active'
      },
      {
        step: '2',
        title: 'Custom Mobs',
        description: 'Clockwork skeletons and slime-infused zombies are being assembled in the workshop.',
        status: 'coming'
      },
      {
        step: '3',
        title: 'The Boss Fight',
        description: 'Vex is currently charging his ultimate spell. The final battle awaits.',
        status: 'coming'
      }
    ] satisfies DungeonFeature[],
    preview: {
      title: 'Gameplay Preview',
      badge: 'Coming Soon'
    }
  },
  videos: {
    id: 'videos',
    title: 'Vex Broadcasts',
    subtitle: 'Trailers, dev logs, and dungeon previews from the workshop.',
    categories: [
      { id: 'dev' as VideoCategory, label: 'Dev' },
      { id: 'trailer' as VideoCategory, label: 'Trailers' }
    ],
    entries: [
      {
        title: 'Dev Log: Dungeon Systems Overview',
        href: 'https://www.youtube.com/watch?v=o3uHD7LCegs',
        category: 'dev',
        summary: 'Quick update on dungeon generation and live tooling.'
      }
    ] satisfies VideoEntry[]
  },
  callToAction: {
    id: 'download',
    title: 'Prepare for Launch',
    subtitle: 'Vex is finalizing his traps. Star the repository to be notified when the dungeon opens.',
    actions: [
      {
        label: 'Star on GitHub',
        href: 'https://github.com/mbround18/hytale-vex-lich-dungeon',
        icon: 'github'
      },
      {
        label: 'Support on Ko-fi',
        href: 'https://ko-fi.com/mbround18',
        icon: 'coffee'
      }
    ],
    footer: ['Development in Progress', 'Join the Adventure']
  },
  footer: {
    title: "Vex's Challenge",
    subtitle: 'Not an official Hytale product. Approved by Vex (mostly).',
    links: [
      { label: 'GitHub', href: 'https://github.com/mbround18/hytale-vex-lich-dungeon' },
      { label: 'Dev Logs', href: '/dev/logs' },
      { label: 'Dev Hub', href: '/dev' }
    ]
  }
};
