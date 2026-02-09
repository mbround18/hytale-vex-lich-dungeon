import React from "react";

export type ArchiveMeta = {
  label: string;
  value: React.ReactNode;
  tone?: "default" | "gold" | "green" | "rose";
};

export type ArchiveCardProps = {
  title: string;
  meta: ArchiveMeta[];
  actions?: React.ReactNode;
  className?: string;
};

const toneClass = (tone?: ArchiveMeta["tone"]) => {
  switch (tone) {
    case "gold":
      return "text-[#fbbf24]";
    case "green":
      return "text-[#4ade80]";
    case "rose":
      return "text-[#f472b6]";
    default:
      return "text-[#9f8cc9]";
  }
};

export default function ArchiveCard({
  title,
  meta,
  actions,
  className = "",
}: ArchiveCardProps) {
  return (
    <div
      className={`stat-card p-4 rounded-lg flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 ${className}`}
    >
      <div>
        <p className="text-xs font-bold text-white mb-1">{title}</p>
        <div className="flex flex-wrap gap-4">
          {meta.map((item) => (
            <p
              key={item.label}
              className={`text-[9px] uppercase ${toneClass(item.tone)}`}
            >
              <span className="opacity-60">{item.label}</span>{" "}
              <span>{item.value}</span>
            </p>
          ))}
        </div>
      </div>
      {actions && (
        <div className="flex flex-wrap items-center gap-3">{actions}</div>
      )}
    </div>
  );
}
