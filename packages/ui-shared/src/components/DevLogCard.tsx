import React from 'react';
import { Calendar, ChevronRight, Users } from 'lucide-react';

type DevLogCardProps = {
  title: string;
  date: string;
  author: string;
  tags: string[];
  excerpt: string;
};

export default function DevLogCard({ title, date, author, tags, excerpt }: DevLogCardProps) {
  return (
    <div className="group relative p-6 rounded-2xl bg-[#120b1b] border border-[#30223f] hover:border-violet-500/50 transition-all duration-300 hover:shadow-[0_10px_40px_rgba(139,92,246,0.15)]">
      <div className="absolute top-0 right-0 p-6 opacity-30 group-hover:opacity-100 transition-opacity">
        <ChevronRight className="text-violet-500" />
      </div>

      <div className="flex items-center gap-3 mb-4 text-xs">
        <span className="flex items-center gap-1 text-[#9f8cc9] font-mono">
          <Calendar size={12} /> {date}
        </span>
        <span className="w-1 h-1 bg-[#30223f] rounded-full" />
        <span className="flex items-center gap-1 text-violet-400 font-bold">
          <Users size={12} /> {author}
        </span>
      </div>

      <h3 className="text-xl font-heading font-bold text-white mb-3 group-hover:text-violet-300 transition-colors">
        {title}
      </h3>

      <p className="text-sm text-[#d7c9ff] leading-relaxed mb-6">
        {excerpt}
      </p>

      <div className="flex flex-wrap gap-2">
        {tags.map((tag, i) => (
          <span key={i} className="px-2 py-1 rounded bg-white/5 text-[10px] uppercase tracking-wider font-bold text-[#9f8cc9] border border-white/5">
            {tag}
          </span>
        ))}
      </div>
    </div>
  );
}
