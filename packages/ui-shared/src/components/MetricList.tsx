import React from 'react';

export type MetricItem = {
  label: string;
  value: React.ReactNode;
};

export type MetricListProps = {
  items: MetricItem[];
  className?: string;
};

export default function MetricList({ items, className = '' }: MetricListProps) {
  return (
    <div className={`grid gap-2 ${className}`}>
      {items.map(item => (
        <div key={item.label} className="flex items-center justify-between text-xs px-3 py-2 rounded-xl bg-black/60 border border-[#30223f]/60">
          <span className="text-[#9f8cc9] uppercase tracking-[0.12em] text-[10px] font-bold">{item.label}</span>
          <span className="text-[#f5f3ff] font-semibold">{item.value}</span>
        </div>
      ))}
    </div>
  );
}
