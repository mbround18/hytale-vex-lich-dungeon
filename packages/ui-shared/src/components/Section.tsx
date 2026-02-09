import React from "react";

type SectionProps = {
  title: string;
  children: React.ReactNode;
};

export default function Section({ title, children }: SectionProps) {
  return (
    <div className="mb-16">
      <div className="flex items-center gap-3 mb-8 border-b border-white/10 pb-4">
        <div className="w-1 h-6 bg-violet-500 rounded-full" />
        <h2 className="text-xl font-heading font-bold text-white">{title}</h2>
      </div>
      {children}
    </div>
  );
}
