import React from 'react';
import SectionTitle from './SectionTitle';

type TrackingCardProps = {
  title?: string;
  className?: string;
  children: React.ReactNode;
};

export default function TrackingCard({ title, className = '', children }: TrackingCardProps) {
  return (
    <div className={`p-6 rounded-2xl bg-gradient-to-br from-[#171025]/85 to-[#0a0710]/95 border border-violet-500/30 shadow-[0_20px_45px_rgba(4,2,7,0.55)] ${className}`}>
      {title && <SectionTitle>{title}</SectionTitle>}
      {children}
    </div>
  );
}
