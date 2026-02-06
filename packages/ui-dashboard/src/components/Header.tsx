import React from 'react';
import { NavButton, StatusPill } from 'ui-shared/components';

type HeaderProps = {
  status: string;
  view: string;
  onViewChange: (view: 'stats' | 'map' | 'archives' | 'logs') => void;
};

const tabs: Array<{ id: 'stats' | 'map' | 'archives' | 'logs'; label: string }> = [
  { id: 'stats', label: 'Command' },
  { id: 'map', label: 'Nexus Map' },
  { id: 'archives', label: 'Archives' },
  { id: 'logs', label: 'Telemetry' }
];

export default function Header({ status, view, onViewChange }: HeaderProps) {
  return (
    <header className="h-16 app-header flex items-center justify-between px-6 shrink-0 z-50">
      <div className="flex items-center gap-6">
        <div className="flex items-center">
          <div className="w-9 h-9 bg-[#8b5cf6] rounded-md flex items-center justify-center text-[#e7d7ff] brand-badge">
            <i className="fa-solid fa-skull-crossbones text-sm" />
          </div>
          <div className="ml-3">
            <span className="block text-[10px] font-black tracking-[.3em] text-[#cdb6ff] uppercase opacity-70">VEX SYSTEM</span>
            <span className="block text-sm font-black text-white tracking-tight">DUNGEON CONTROL</span>
          </div>
        </div>

        <nav className="flex gap-2">
          {tabs.map(tab => (
            <NavButton
              key={tab.id}
              active={view === tab.id}
              onClick={() => onViewChange(tab.id)}
            >
              {tab.label}
            </NavButton>
          ))}
        </nav>
      </div>

      <div className="flex items-center gap-4">
        <StatusPill status={status} variant={status === 'online' ? 'live' : 'offline'} />
      </div>
    </header>
  );
}
