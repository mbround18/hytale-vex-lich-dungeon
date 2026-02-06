import React from 'react';

type ColorSwatchProps = {
  name: string;
  color: string;
  hex: string;
};

export default function ColorSwatch({ name, color, hex }: ColorSwatchProps) {
  return (
    <div className="flex flex-col gap-2">
      <div className="h-16 w-full rounded-xl shadow-lg border border-white/10" style={{ backgroundColor: color }} />
      <div>
        <p className="text-xs font-bold text-white uppercase">{name}</p>
        <p className="text-[10px] font-mono text-gray-400">{hex}</p>
      </div>
    </div>
  );
}
