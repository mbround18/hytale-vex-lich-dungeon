import React from "react";

type SyntaxHighlighterProps = {
  data: any;
};

export default function SyntaxHighlighter({ data }: SyntaxHighlighterProps) {
  const jsonStr = JSON.stringify(data, null, 2);
  const highlighted = jsonStr.replace(
    /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
    (match) => {
      let cls = "text-pink-400";
      if (/^"/.test(match)) {
        if (/:$/.test(match)) cls = "text-amber-400 font-bold";
        else cls = "text-green-400";
      } else if (/true|false/.test(match)) cls = "text-violet-400 font-bold";
      return `<span class="${cls}">${match}</span>`;
    },
  );

  return (
    <pre
      className="font-mono text-xs leading-relaxed text-[#d7c9ff]"
      dangerouslySetInnerHTML={{ __html: highlighted }}
    />
  );
}
