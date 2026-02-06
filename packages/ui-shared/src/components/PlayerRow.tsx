import React from 'react';
import { Map, Users } from 'lucide-react';

type PlayerRowProps = {
  name: string;
  world: string;
  room: string;
  status?: 'active' | 'idle' | 'offline';
};

export default function PlayerRow({ name, world, room, status = 'active' }: PlayerRowProps) {
  return (
    <div className="flex items-center justify-between p-3 rounded-lg bg-white/5 border border-white/5 hover:bg-white/10 transition-colors group">
      <div className="flex items-center gap-4">
        <div className="w-8 h-8 rounded-lg bg-green-400/10 border border-green-400/20 flex items-center justify-center text-green-400 relative">
          {status === 'active' && (
            <span className="absolute top-0 right-0 w-2 h-2 bg-green-400 rounded-full shadow-[0_0_8px_rgba(74,222,128,0.8)]" />
          )}
          <Users size={14} />
        </div>
        <div>
          <p className="text-sm font-bold text-white font-heading">{name}</p>
          <p className="text-[10px] text-[#9f8cc9] font-mono">{world}</p>
        </div>
      </div>
      <div className="px-3 py-1 rounded bg-amber-400/10 text-amber-400 text-xs font-mono font-bold border border-amber-400/20">
        <Map size={10} className="inline mr-1" /> {room}
      </div>
    </div>
  );
}
