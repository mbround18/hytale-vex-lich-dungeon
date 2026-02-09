import React from "react";

type BadgeProps = {
  variant?: "default" | "gold" | "green" | "rose" | "cyan";
  children: React.ReactNode;
};

export default function Badge({ variant = "default", children }: BadgeProps) {
  const variants: Record<string, string> = {
    default: "bg-violet-500/10 text-violet-400 border-violet-500/30",
    gold: "bg-amber-400/10 text-amber-400 border-amber-400/30",
    green: "bg-green-400/10 text-green-400 border-green-400/30",
    rose: "bg-pink-400/10 text-pink-400 border-pink-400/30",
    cyan: "bg-cyan-400/10 text-cyan-400 border-cyan-400/30",
  };

  return (
    <span
      className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider border ${variants[variant]}`}
    >
      {children}
    </span>
  );
}
