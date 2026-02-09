import React from "react";

export type StatusPillProps = {
  status: string;
  variant?: "live" | "offline" | "warning";
  className?: string;
};

export default function StatusPill({
  status,
  variant = "live",
  className = "",
}: StatusPillProps) {
  const dotClass =
    variant === "offline"
      ? "status-dot offline bg-red-500 shadow-[0_0_10px_rgba(239,68,68,0.6)]"
      : variant === "warning"
        ? "status-dot gold bg-amber-400 shadow-[0_0_10px_rgba(251,191,36,0.6)]"
        : "status-dot live bg-green-400 shadow-[0_0_10px_rgba(74,222,128,0.6)]";
  return (
    <div
      className={`status-pill inline-flex items-center gap-2 px-3 py-1 rounded-full border border-violet-500/40 bg-black/60 text-[10px] uppercase tracking-[0.18em] font-bold text-[#9f8cc9] ${className}`}
    >
      <div className={dotClass} />
      <span>{status}</span>
    </div>
  );
}
