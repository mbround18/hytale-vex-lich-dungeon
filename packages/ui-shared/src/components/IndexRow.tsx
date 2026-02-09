import React from "react";
import Badge from "./Badge";

type IndexRowProps = {
  id: string;
  title: string;
  type: string;
  status: "Stable" | "Beta" | "Deprecated" | string;
  modified: string;
};

export default function IndexRow({
  id,
  title,
  type,
  status,
  modified,
}: IndexRowProps) {
  return (
    <div className="flex items-center justify-between p-3 border-b border-[#30223f] hover:bg-white/5 transition-colors cursor-pointer group last:border-0">
      <div className="flex items-center gap-4">
        <div className="w-8 h-8 rounded flex items-center justify-center bg-[#08060d] border border-[#30223f] text-[#9f8cc9] font-mono text-[10px]">
          {id}
        </div>
        <div>
          <p className="text-sm font-bold text-[#d7c9ff] group-hover:text-white transition-colors">
            {title}
          </p>
          <p className="text-[10px] text-[#9f8cc9] uppercase tracking-wider">
            {type}
          </p>
        </div>
      </div>
      <div className="flex items-center gap-6">
        <span className="text-[10px] font-mono text-[#9f8cc9] hidden md:block">
          {modified}
        </span>
        <Badge
          variant={
            status === "Stable"
              ? "green"
              : status === "Deprecated"
                ? "rose"
                : "gold"
          }
        >
          {status}
        </Badge>
      </div>
    </div>
  );
}
