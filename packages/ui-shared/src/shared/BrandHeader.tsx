import React from "react";

interface BrandHeaderProps {
  title: string;
  subtitle: string;
}

export function BrandHeader({ title, subtitle }: BrandHeaderProps) {
  return (
    <header className="brand-header">
      <div className="brand-badge">VEX</div>
      <div>
        <div className="brand-subtitle">{subtitle}</div>
        <div className="brand-title">{title}</div>
      </div>
    </header>
  );
}
