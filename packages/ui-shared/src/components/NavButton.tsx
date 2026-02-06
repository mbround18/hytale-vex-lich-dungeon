import React from 'react';

type NavButtonProps = {
  active?: boolean;
  children: React.ReactNode;
  onClick?: () => void;
};

export default function NavButton({ active, children, onClick }: NavButtonProps) {
  const baseClasses = "px-4 py-2 rounded-lg text-xs font-bold uppercase tracking-widest transition-all duration-200 border border-transparent";
  const activeClasses = "bg-gradient-to-br from-violet-500/90 to-purple-700/90 text-white shadow-[0_6px_16px_rgba(139,92,246,0.35)] border-violet-500/50";
  const inactiveClasses = "text-[#9f8cc9] hover:text-white hover:bg-white/5";

  return (
    <button onClick={onClick} className={`${baseClasses} ${active ? activeClasses : inactiveClasses}`}>
      {children}
    </button>
  );
}
