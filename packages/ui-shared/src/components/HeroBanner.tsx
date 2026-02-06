import React from 'react';
import Badge from './Badge';

export type HeroBannerProps = {
  badge?: string;
  title: string;
  subtitle?: string;
  right?: React.ReactNode;
  className?: string;
};

export default function HeroBanner({ badge, title, subtitle, right, className = '' }: HeroBannerProps) {
  return (
    <section className={`hero-banner ${className}`}>
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
        <div>
          {badge && <Badge>{badge}</Badge>}
          <h1 className="hero-title mt-4">{title}</h1>
          {subtitle && <p className="hero-subtitle mt-2">{subtitle}</p>}
        </div>
        {right && <div>{right}</div>}
      </div>
    </section>
  );
}
