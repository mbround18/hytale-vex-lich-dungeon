import React from "react";

export type DataTableColumn<T> = {
  key: string;
  label: React.ReactNode;
  render?: (row: T) => React.ReactNode;
  className?: string;
};

export type DataTableProps<T> = {
  columns: Array<DataTableColumn<T>>;
  rows: T[];
  getRowKey?: (row: T, index: number) => string | number;
  emptyMessage?: React.ReactNode;
  className?: string;
};

export default function DataTable<T>({
  columns,
  rows,
  getRowKey,
  emptyMessage,
  className = "",
}: DataTableProps<T>) {
  return (
    <table className={`w-full border-collapse text-xs ${className}`}>
      <thead>
        <tr>
          {columns.map((col) => (
            <th
              key={col.key}
              className="text-left px-3 py-3 text-[10px] uppercase tracking-[0.08em] text-[#9f8cc9] font-semibold border-b border-violet-500/20"
            >
              {col.label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, index) => (
          <tr
            key={getRowKey ? getRowKey(row, index) : index}
            className="hover:bg-violet-500/10"
          >
            {columns.map((col) => (
              <td
                key={col.key}
                className={`px-3 py-3 border-b border-[#30223f] ${col.className || ""}`}
              >
                {col.render ? col.render(row) : (row as any)[col.key]}
              </td>
            ))}
          </tr>
        ))}
        {rows.length === 0 && emptyMessage && (
          <tr>
            <td
              colSpan={columns.length}
              className="px-3 py-6 text-center text-xs text-[#5f2b84]"
            >
              {emptyMessage}
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );
}
