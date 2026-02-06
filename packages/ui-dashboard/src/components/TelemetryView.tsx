import React from 'react';
import { EventLogItem, SectionTitle, SplitPane, SyntaxHighlighter } from 'ui-shared/components';

interface TelemetryViewProps {
  search: string;
  onSearchChange: (value: string) => void;
  events: any[];
  selectedEvent: any | null;
  onSelectEvent: (ev: any | null) => void;
  onPurge: () => void;
  onDownload: () => void;
}

export default function TelemetryView({
  search,
  onSearchChange,
  events,
  selectedEvent,
  onSelectEvent,
  onPurge,
  onDownload
}: TelemetryViewProps) {
  return (
    <SplitPane
      className="flex-1 overflow-hidden"
      sidebarClassName="w-80 lg:w-96 border-r border-[#30223f] flex flex-col bg-[#120b1b]"
      contentClassName="flex-1 flex flex-col bg-[#08060d]/90 p-8 overflow-auto custom-scrollbar"
      sidebar={(
        <>
          <div className="p-4 space-y-4">
            <input
              type="text"
              value={search}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder="Scan packet types..."
              className="w-full bg-black/20 border border-violet-500/30 rounded-lg py-2 px-4 text-xs text-white focus:outline-none focus:border-violet-500"
            />
            <button
              onClick={onPurge}
              className="w-full py-2 text-[10px] font-bold uppercase tracking-widest text-[#9f8cc9] border border-[#3a2a50] rounded-lg hover:bg-white/5 hover:text-white transition-all"
            >
              Purge Telemetry
            </button>
            <button
              onClick={onDownload}
              className="w-full py-2 text-[10px] font-bold uppercase tracking-widest text-white bg-violet-600/90 border border-violet-500/40 rounded-lg hover:bg-violet-500 transition-all"
            >
              Download Full Telemetry
            </button>
          </div>
          <div className="flex-1 overflow-y-auto custom-scrollbar p-2 space-y-1 border-t border-[#30223f]">
            {events.map((ev: any) => (
              <EventLogItem
                key={ev.internalId}
                type={ev.type || 'Event'}
                time={new Date(ev.timestamp).toLocaleTimeString()}
                data={ev.data}
                active={selectedEvent?.internalId === ev.internalId}
                onClick={() => onSelectEvent(ev)}
              />
            ))}
          </div>
        </>
      )}
      content={(
        <>
          {!selectedEvent && (
            <div className="h-full flex flex-col items-center justify-center opacity-10">
              <i className="fa-solid fa-microscope text-6xl mb-4" />
              <p className="text-[10px] uppercase tracking-[0.4em] font-black">Waiting for node selection</p>
            </div>
          )}
          {selectedEvent && (
            <div className="space-y-6">
              <SectionTitle>{selectedEvent.type}</SectionTitle>
              <div className="rounded-xl border border-violet-500/20 bg-[#0c0812] p-6 shadow-2xl">
                <SyntaxHighlighter data={selectedEvent.data} />
              </div>
            </div>
          )}
        </>
      )}
    />
  );
}
