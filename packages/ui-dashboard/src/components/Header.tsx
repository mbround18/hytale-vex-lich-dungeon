import React from "react";
import { NavLink } from "react-router-dom";
import { StatusPill } from "ui-shared/components";

type HeaderProps = {
  status: string;
};

const tabs: Array<{
  id: "stats" | "map" | "archives" | "logs";
  label: string;
  to: string;
}> = [
  { id: "stats", label: "Command", to: "/stats" },
  { id: "map", label: "Nexus Map", to: "/map" },
  { id: "archives", label: "Archives", to: "/archives" },
  { id: "logs", label: "Telemetry", to: "/logs" },
];

const baseClasses =
  "px-4 py-2 rounded-lg text-xs font-bold uppercase tracking-widest transition-all duration-200 border border-transparent nav-button";
const activeClasses = "active";
const inactiveClasses = "text-[#9bb3c7] hover:text-white hover:bg-white/5";

export default function Header({ status }: HeaderProps) {
  return (
    <header className="h-16 app-header flex items-center justify-between px-6 shrink-0 z-50">
      <div className="flex items-center gap-6">
        <div className="flex items-center">
          <div className="w-9 h-9 bg-[#0ea5a4] rounded-md flex items-center justify-center text-[#e6fbff] brand-badge">
            <i className="fa-solid fa-skull-crossbones text-sm" />
          </div>
          <div className="ml-3">
            <span className="block text-[10px] font-black tracking-[.3em] text-[#9bb3c7] uppercase opacity-70">
              VEX NETWORK
            </span>
            <span className="block text-sm font-black text-white tracking-tight">
              DUNGEON COMMAND
            </span>
          </div>
        </div>

        <nav className="flex gap-2">
          {tabs.map((tab) => (
            <NavLink
              key={tab.id}
              to={tab.to}
              className={({ isActive }) =>
                `${baseClasses} ${isActive ? activeClasses : inactiveClasses}`
              }
            >
              {tab.label}
            </NavLink>
          ))}
        </nav>
      </div>

      <div className="flex items-center gap-4">
        <StatusPill
          status={status}
          variant={status === "online" ? "live" : "offline"}
        />
      </div>
    </header>
  );
}
