import React from 'react';

type SectionTitleProps = {
  children: React.ReactNode;
  icon?: React.ElementType;
};

export default function SectionTitle({ children, icon: Icon }: SectionTitleProps) {
  return (
    <div className="flex items-center gap-2 mb-6">
      {Icon && <Icon size={14} className="text-violet-500" />}
      <h3 className="text-[11px] uppercase tracking-[0.34em] font-bold text-violet-500 font-heading">
        {children}
      </h3>
    </div>
  );
}
