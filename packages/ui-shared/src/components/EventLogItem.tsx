import React from "react";

type EventLogItemProps = {
  type: string;
  time: string;
  data: any;
  active?: boolean;
  onClick?: () => void;
};

export default function EventLogItem({
  type,
  time,
  data,
  active,
  onClick,
}: EventLogItemProps) {
  return (
    <div
      onClick={onClick}
      className={`
        p-3 rounded-lg cursor-pointer border-l-4 transition-all duration-200 mb-1
        ${
          active
            ? "bg-[#1f1633] border-violet-500 shadow-[inset_20px_0_40px_-20px_rgba(139,92,246,0.15)]"
            : "bg-white/5 border-transparent hover:bg-white/10 hover:border-violet-500/30"
        }
      `}
    >
      <div className="flex justify-between items-center mb-1">
        <span className="text-[10px] font-bold text-violet-400 uppercase tracking-tight">
          {type}
        </span>
        <span className="text-[9px] font-mono text-[#9f8cc9]">{time}</span>
      </div>
      <div className="text-[10px] text-[#d7c9ff]/60 truncate font-mono">
        {JSON.stringify(data)}
      </div>
    </div>
  );
}
