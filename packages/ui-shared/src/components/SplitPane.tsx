import React from "react";

export type SplitPaneProps = {
  sidebar: React.ReactNode;
  content: React.ReactNode;
  className?: string;
  sidebarClassName?: string;
  contentClassName?: string;
};

export default function SplitPane({
  sidebar,
  content,
  className = "",
  sidebarClassName = "",
  contentClassName = "",
}: SplitPaneProps) {
  return (
    <div className={`flex ${className}`}>
      <aside className={sidebarClassName}>{sidebar}</aside>
      <section className={contentClassName}>{content}</section>
    </div>
  );
}
