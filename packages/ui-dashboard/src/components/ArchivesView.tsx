import React from "react";
import { ArchiveCard, Badge, SectionTitle } from "ui-shared/components";

interface ArchivesViewProps {
  archives: any[];
  selectedArchive: any | null;
  onSelectArchive: (archive: any | null) => void;
  onClearArchives: () => void;
  onDeleteArchive: (id: string) => void;
  onDownloadInstanceLog: (id: string) => void;
  replay: {
    active: boolean;
    playing: boolean;
    cursor: number;
    events: any[];
  };
  replayRangeMax: number;
  replayStartTime: string;
  replayCurrentTime: string;
  replayEndTime: string;
  onStartReplay: (id: string) => void;
  onToggleReplay: () => void;
  onStopReplay: () => void;
  onSeekReplay: (value: string) => void;
}

export default function ArchivesView({
  archives,
  selectedArchive,
  onSelectArchive,
  onClearArchives,
  onDeleteArchive,
  onDownloadInstanceLog,
  replay,
  replayRangeMax,
  replayStartTime,
  replayCurrentTime,
  replayEndTime,
  onStartReplay,
  onToggleReplay,
  onStopReplay,
  onSeekReplay,
}: ArchivesViewProps) {
  return (
    <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <SectionTitle>Persistent Archives</SectionTitle>
          <div className="flex items-center gap-3">
            <button
              onClick={onClearArchives}
              className="px-3 py-2 text-[9px] font-black text-white bg-[#2b1a42] border border-[#5f2b84] rounded hover:bg-[#8b5cf6] transition-all uppercase tracking-widest"
            >
              Clear All
            </button>
          </div>
        </div>
        <div className="space-y-4">
          {archives.map((arch) => (
            <ArchiveCard
              key={arch.id}
              title={arch.id}
              meta={[
                {
                  label: "Timestamp",
                  value: new Date(arch.timestamp).toLocaleString(),
                },
                {
                  label: "Rooms",
                  value: `${Object.keys(arch.data.rooms || {}).length} Rooms`,
                  tone: "gold",
                },
                {
                  label: "Players",
                  value: `${(arch.data.players || []).length} Players`,
                  tone: "green",
                },
              ]}
              actions={
                <>
                  <button
                    onClick={() => onSelectArchive(arch)}
                    className="px-3 py-2 text-[9px] font-black text-white bg-[#1b1326] border border-[#3a2a50] rounded hover:bg-[#8b5cf6] transition-all uppercase tracking-widest"
                  >
                    View
                  </button>
                  <button
                    onClick={() => onDownloadInstanceLog(arch.id)}
                    className="px-3 py-2 text-[9px] font-black text-white bg-[#2b1a42] border border-[#5f2b84] rounded hover:bg-[#8b5cf6] transition-all uppercase tracking-widest"
                  >
                    Download Log
                  </button>
                  <button
                    onClick={() => onDeleteArchive(arch.id)}
                    className="px-3 py-2 text-[9px] font-black text-white bg-[#2b1218] border border-[#5f2b84] rounded hover:bg-[#f472b6] transition-all uppercase tracking-widest"
                  >
                    Delete
                  </button>
                  <Badge variant="green">Completed</Badge>
                </>
              }
            />
          ))}
          {archives.length === 0 && (
            <div className="text-center p-8 text-stone-600 text-xs">
              No archived runs found in database.
            </div>
          )}
        </div>

        {selectedArchive && (
          <div className="stat-card p-6 mt-6">
            <div className="flex items-center justify-between mb-4">
              <div>
                <SectionTitle>Archive Details</SectionTitle>
                <p className="section-subtitle mt-2">{selectedArchive.id}</p>
              </div>
              <button
                onClick={() => onSelectArchive(null)}
                className="px-3 py-2 text-[9px] font-black text-white bg-[#1b1326] border border-[#3a2a50] rounded hover:bg-[#8b5cf6] transition-all uppercase tracking-widest"
              >
                Close
              </button>
            </div>
            <div className="p-4 rounded-xl border border-white/5 bg-black/30 mb-4">
              <div className="flex flex-wrap items-center gap-3 justify-between">
                <div>
                  <p className="text-[11px] text-[#cdb6ff] uppercase">
                    Replay Timeline
                  </p>
                  <p className="text-sm text-white mt-1">
                    Start:{" "}
                    <span className="text-[#fbbf24]">{replayStartTime}</span>
                  </p>
                  <p className="text-sm text-white">
                    Now:{" "}
                    <span className="text-[#4ade80]">{replayCurrentTime}</span>
                  </p>
                  <p className="text-sm text-white">
                    End: <span className="text-[#f472b6]">{replayEndTime}</span>
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => onStartReplay(selectedArchive.id)}
                    className="px-3 py-2 text-[9px] font-black text-white bg-[#1b1326] border border-[#3a2a50] rounded hover:bg-[#8b5cf6] transition-all uppercase tracking-widest"
                  >
                    Load Run
                  </button>
                  <button
                    onClick={onToggleReplay}
                    className="px-3 py-2 text-[9px] font-black text-white bg-[#2b1a42] border border-[#5f2b84] rounded hover:bg-[#8b5cf6] transition-all uppercase tracking-widest"
                  >
                    {replay.playing ? "Pause" : "Play"}
                  </button>
                  <button
                    onClick={onStopReplay}
                    className="px-3 py-2 text-[9px] font-black text-white bg-[#2b1218] border border-[#5f2b84] rounded hover:bg-[#f472b6] transition-all uppercase tracking-widest"
                  >
                    Stop
                  </button>
                </div>
              </div>
              <input
                type="range"
                className="w-full mt-4"
                min={0}
                max={replayRangeMax}
                value={replay.cursor}
                onChange={(e) => onSeekReplay(e.target.value)}
              />
              {replay.events.length === 0 && (
                <p className="text-[10px] text-[#9f8cc9] mt-2">
                  Load Run to build a replay timeline from telemetry.
                </p>
              )}
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <div className="p-4 rounded-xl border border-white/5 bg-black/30">
                <p className="text-[11px] text-[#cdb6ff] uppercase">Rooms</p>
                <p className="text-2xl font-semibold text-white">
                  {Object.keys(selectedArchive.data?.rooms || {}).length}
                </p>
              </div>
              <div className="p-4 rounded-xl border border-white/5 bg-black/30">
                <p className="text-[11px] text-[#cdb6ff] uppercase">Players</p>
                <p className="text-2xl font-semibold text-white">
                  {(selectedArchive.data?.players || []).length}
                </p>
              </div>
              <div className="p-4 rounded-xl border border-white/5 bg-black/30">
                <p className="text-[11px] text-[#cdb6ff] uppercase">Status</p>
                <p className="text-sm font-semibold text-white">
                  {selectedArchive.data?.status || "closed"}
                </p>
              </div>
              <div className="p-4 rounded-xl border border-white/5 bg-black/30">
                <p className="text-[11px] text-[#cdb6ff] uppercase">
                  Timestamp
                </p>
                <p className="text-sm font-semibold text-white">
                  {new Date(selectedArchive.timestamp || "").toLocaleString()}
                </p>
              </div>
            </div>
            <div className="mt-4 rounded border border-[#5f2b84] bg-black/60 p-4">
              <pre className="text-[#cdb6ff] text-xs">
                {JSON.stringify(selectedArchive.data || {}, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
