import React from 'react';
import { ChevronRight } from 'lucide-react';

type WikiCardProps = {
  title: string;
  description: string;
  icon: React.ElementType;
  color?: string;
};

export default function WikiCard({ title, description, icon: Icon, color = 'text-violet-400' }: WikiCardProps) {
  return (
    <div className="p-5 rounded-xl bg-[#161025] border border-[#30223f] hover:bg-[#1f1633] transition-colors cursor-pointer group">
      <div className="flex items-start justify-between mb-4">
        <div className={`p-3 rounded-lg bg-white/5 ${color} group-hover:scale-110 transition-transform duration-300`}>
          <Icon size={24} />
        </div>
        <ChevronRight size={16} className="text-[#30223f] group-hover:text-white transition-colors" />
      </div>
      <h4 className="text-white font-heading font-bold mb-2">{title}</h4>
      <p className="text-xs text-[#9f8cc9] leading-relaxed">{description}</p>
    </div>
  );
}
