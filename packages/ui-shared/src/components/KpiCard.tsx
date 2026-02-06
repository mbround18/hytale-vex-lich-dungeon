import React from 'react';

type KpiCardProps = {
  title: string;
  value: string | number;
  icon?: React.ElementType;
  color?: string;
};

export default function KpiCard({ title, value, icon: Icon, color = 'text-white' }: KpiCardProps) {
  return (
    <div className="relative group overflow-hidden rounded-2xl bg-gradient-to-br from-[#1b132c]/90 to-[#0c0812]/95 border border-violet-500/30 p-6 min-h-[110px] transition-transform hover:-translate-y-1">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(139,92,246,0.15),transparent_60%)] opacity-50 pointer-events-none" />
      <div className="relative z-10 flex flex-col justify-center h-full">
        <p className="text-[10px] uppercase tracking-[0.28em] font-bold text-[#9f8cc9] mb-2">{title}</p>
        <h2 className={`text-4xl font-bold font-heading ${color}`}>{value}</h2>
      </div>
      {Icon && (
        <div className={`absolute -bottom-4 -right-4 text-6xl opacity-10 rotate-12 group-hover:scale-110 transition-transform duration-500 ${color}`}>
          <Icon />
        </div>
      )}
    </div>
  );
}
