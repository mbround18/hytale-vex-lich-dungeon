import React, { useMemo, useEffect, useState, useRef } from "react";
import {
  Trash2,
  Download,
  Search,
  Terminal,
  Activity,
  Clock,
  Code,
  FileJson,
  AlertCircle,
  Pause,
  Play,
  ArrowUp,
  Rewind,
  FastForward,
  StopCircle,
} from "lucide-react";
import {
  clearEventBuffer,
  events$,
  streamStatus$,
} from "../state/dashboardBus";
import type { TelemetryEvent } from "../types";

type TelemetryViewProps = {
  replayActive?: boolean;
  replayLabel?: string;
  onReplayToEvent?: (events: TelemetryEvent[], event: TelemetryEvent) => void;
  onReplayFromEvent?: (events: TelemetryEvent[], event: TelemetryEvent) => void;
  onExitReplay?: () => void;
  onClearLog?: () => void;
};

export default function TelemetryView({
  replayActive = false,
  replayLabel,
  onReplayToEvent,
  onReplayFromEvent,
  onExitReplay,
  onClearLog,
}: TelemetryViewProps) {
  // --- State ---
  const [events, setEvents] = useState<TelemetryEvent[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<TelemetryEvent | null>(
    null,
  );
  const [search, setSearch] = useState("");
  const [scopeFilter, setScopeFilter] = useState<
    "all" | "engine" | "vex" | "server"
  >("all");
  const [isConnected, setIsConnected] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [autoPaused, setAutoPaused] = useState(false);
  const [bufferedCount, setBufferedCount] = useState(0);

  const pausedRef = useRef<boolean>(false);
  const listRef = useRef<HTMLDivElement | null>(null);
  const latestEventsRef = useRef<TelemetryEvent[]>([]);
  const hasBufferedRef = useRef(false);
  const pausedSnapshotRef = useRef(0);

  useEffect(() => {
    pausedRef.current = isPaused;
  }, [isPaused]);

  useEffect(() => {
    const normalize = (ev: any): TelemetryEvent => {
      const ts = ev.timestamp ?? ev.data?.timestamp ?? Date.now();
      const id = ev.id ?? ev.internalId ?? `${ev.type ?? "event"}-${ts}`;
      return {
        ...ev,
        id,
        timestamp: ts,
        type: ev.type ?? ev.data?.type ?? "UnknownEvent",
        payload: ev.payload ?? ev.data ?? ev,
      };
    };

    const eventsSub = events$.subscribe((next) => {
      const mapped = next.map((ev) => normalize(ev)).slice(0, 500);
      latestEventsRef.current = mapped;
      if (pausedRef.current) {
        hasBufferedRef.current = true;
        const pending = Math.max(0, mapped.length - pausedSnapshotRef.current);
        setBufferedCount(pending);
        return;
      }
      hasBufferedRef.current = false;
      setBufferedCount(0);
      setEvents(mapped);
    });

    const statusSub = streamStatus$.subscribe((status) => {
      setIsConnected(status.connected);
    });

    return () => {
      eventsSub.unsubscribe();
      statusSub.unsubscribe();
    };
  }, []);

  useEffect(() => {
    if (isPaused) {
      pausedSnapshotRef.current = latestEventsRef.current.length;
      setBufferedCount(0);
      return;
    }
    if (!hasBufferedRef.current) {
      setBufferedCount(0);
      return;
    }
    setEvents(latestEventsRef.current);
    hasBufferedRef.current = false;
    setBufferedCount(0);
  }, [isPaused]);

  // --- Handlers ---

  const handleSelectEvent = (ev: TelemetryEvent) => {
    setSelectedEvent(ev);
    if (!isPaused) {
      pausedSnapshotRef.current = latestEventsRef.current.length;
      setIsPaused(true);
      setAutoPaused(true);
    }
  };

  const handlePurge = () => {
    setEvents([]);
    setSelectedEvent(null);
    latestEventsRef.current = [];
    hasBufferedRef.current = false;
    pausedSnapshotRef.current = 0;
    setBufferedCount(0);
    onClearLog?.();
    clearEventBuffer();
  };

  const handleDownload = () => {
    const dataStr =
      "data:text/json;charset=utf-8," +
      encodeURIComponent(JSON.stringify(events, null, 2));
    const downloadAnchorNode = document.createElement("a");
    downloadAnchorNode.setAttribute("href", dataStr);
    downloadAnchorNode.setAttribute(
      "download",
      `telemetry_dump_${Date.now()}.json`,
    );
    document.body.appendChild(downloadAnchorNode);
    downloadAnchorNode.click();
    downloadAnchorNode.remove();
  };

  const handleTogglePause = () => {
    setIsPaused((prev) => {
      const next = !prev;
      if (!next) {
        setAutoPaused(false);
      }
      return next;
    });
  };

  const handleFollow = () => {
    setIsPaused(false);
    setAutoPaused(false);
    setBufferedCount(0);
    if (listRef.current) {
      listRef.current.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const handleReplayToEvent = () => {
    if (!selectedEvent || !onReplayToEvent) return;
    onReplayToEvent(events, selectedEvent);
  };

  const handleReplayFromEvent = () => {
    if (!selectedEvent || !onReplayFromEvent) return;
    onReplayFromEvent(events, selectedEvent);
  };

  const getEventScope = (type?: string) => {
    if (!type) return "server";
    if (type.startsWith("MBRound18.ImmortalEngine.")) return "engine";
    if (type.startsWith("MBRound18.hytale.vexlichdungeon.")) return "vex";
    return "server";
  };

  // --- Filtering ---
  const filteredEvents = useMemo(() => {
    const lower = search.toLowerCase();
    return events.filter((ev) => {
      if (scopeFilter !== "all" && getEventScope(ev.type) !== scopeFilter) {
        return false;
      }
      if (!search) return true;
      return (
        (ev.type || "").toLowerCase().includes(lower) ||
        JSON.stringify(ev.payload || {})
          .toLowerCase()
          .includes(lower)
      );
    });
  }, [events, scopeFilter, search]);

  return (
    <div className="flex w-full h-full bg-[#050608] font-sans overflow-hidden text-gray-200">
      {/* --- SIDEBAR --- */}
      <div className="w-80 lg:w-96 flex flex-col border-r border-white/10 bg-[#0b0d12] z-20 shadow-2xl">
        {/* Header */}
        <div className="p-4 border-b border-white/10 bg-black/20">
          <div className="flex justify-between items-center mb-1">
            <h2 className="text-sm font-bold text-cyan-300 flex items-center gap-2 tracking-widest uppercase">
              <Activity size={16} />
              Event Stream
            </h2>
            <div className="flex items-center gap-2">
              <div
                className={`flex items-center gap-1.5 px-2 py-0.5 rounded text-[9px] font-bold uppercase border ${
                  isConnected
                    ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
                    : "bg-rose-500/10 text-rose-400 border-rose-500/20"
                }`}
              >
                <div
                  className={`w-1.5 h-1.5 rounded-full ${isConnected ? "bg-emerald-400 animate-pulse" : "bg-rose-400"}`}
                />
                {isConnected ? "LIVE" : "OFFLINE"}
              </div>
              {isPaused && bufferedCount > 0 && (
                <div className="flex items-center gap-1.5 px-2 py-0.5 rounded text-[9px] font-bold uppercase border bg-amber-500/10 text-amber-300 border-amber-500/20">
                  <div className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
                  {bufferedCount} buffered
                </div>
              )}
            </div>
          </div>
          <div className="text-[10px] text-gray-600 font-mono">
            LIVE PACKET INTERCEPTION // {events.length} CAPTURED
          </div>
        </div>

        {/* Controls */}
        <div className="p-3 space-y-3 border-b border-white/5">
          <div className="flex flex-wrap gap-2">
            {(
              [
                { id: "all", label: "All" },
                { id: "engine", label: "Engine" },
                { id: "vex", label: "Vex" },
                { id: "server", label: "Server" },
              ] as const
            ).map((scope) => (
              <button
                key={scope.id}
                onClick={() => setScopeFilter(scope.id)}
                className={`px-2.5 py-1 rounded-full text-[9px] uppercase tracking-[0.2em] font-bold border transition-all ${
                  scopeFilter === scope.id
                    ? "bg-cyan-500/15 text-cyan-200 border-cyan-500/30"
                    : "text-slate-400 border-white/10 hover:border-white/25 hover:text-slate-200"
                }`}
              >
                {scope.label}
              </button>
            ))}
          </div>
          <div className="relative group">
            <Search
              className="absolute left-3 top-2.5 text-gray-500 group-focus-within:text-cyan-300 transition-colors"
              size={14}
            />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Filter packet types..."
              className="w-full bg-black/40 border border-white/10 rounded-md py-2 pl-9 pr-3 text-xs focus:outline-none focus:border-cyan-500/50 focus:bg-cyan-500/5 transition-all placeholder:text-gray-700 font-mono text-gray-300"
            />
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleTogglePause}
              className={`flex-1 flex items-center justify-center gap-2 py-2 text-[10px] font-bold uppercase tracking-wider border rounded transition-all ${
                isPaused
                  ? "text-amber-400/80 border-amber-500/20 bg-amber-500/10"
                  : "text-gray-400 border-gray-700 hover:bg-white/5"
              }`}
            >
              {isPaused ? <Play size={12} /> : <Pause size={12} />}{" "}
              {isPaused ? "Resume" : "Pause"}
            </button>
            {isPaused && (
              <button
                onClick={handleFollow}
                className={`flex-1 flex items-center justify-center gap-2 py-2 text-[10px] font-bold uppercase tracking-wider border rounded transition-all ${
                  autoPaused
                    ? "text-cyan-200 border-cyan-500/30 bg-cyan-500/10"
                    : "text-gray-400 border-gray-700 hover:bg-white/5"
                }`}
                title="Jump to latest event and resume follow mode"
              >
                <ArrowUp size={12} /> Follow
              </button>
            )}
            <button
              onClick={handlePurge}
              className="w-1/4 flex items-center justify-center py-2 text-rose-400/80 border border-rose-500/20 rounded hover:bg-rose-500/10 transition-all"
              title="Clear Log"
            >
              <Trash2 size={12} />
            </button>
            <button
              onClick={handleDownload}
              className="w-1/4 flex items-center justify-center py-2 text-emerald-400/80 border border-emerald-500/20 rounded hover:bg-emerald-500/10 transition-all"
              title="Export JSON"
            >
              <Download size={12} />
            </button>
          </div>
          {replayActive && (
            <div className="flex items-center justify-between px-3 py-2 text-[10px] font-mono rounded border border-cyan-500/20 bg-cyan-500/10 text-cyan-100">
              <span>Replay Mode {replayLabel ? `// ${replayLabel}` : ""}</span>
              {onExitReplay && (
                <button
                  onClick={onExitReplay}
                  className="inline-flex items-center gap-1 text-[9px] font-bold uppercase tracking-widest text-rose-300 border border-rose-500/20 rounded px-2 py-1 hover:bg-rose-500/10"
                >
                  <StopCircle size={12} /> Stop
                </button>
              )}
            </div>
          )}
        </div>

        {/* Event List */}
        <div
          ref={listRef}
          className="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar"
        >
          {filteredEvents.length === 0 ? (
            <div className="p-8 text-center opacity-30 flex flex-col items-center">
              {isConnected ? (
                <>
                  <div className="animate-spin duration-3000 mb-4">
                    <Activity size={32} className="text-cyan-400" />
                  </div>
                  <p className="text-xs font-mono">LISTENING FOR SIGNALS...</p>
                </>
              ) : (
                <>
                  <AlertCircle size={32} className="mb-4 text-rose-500" />
                  <p className="text-xs font-mono text-rose-500">
                    CONNECTION LOST
                  </p>
                </>
              )}
            </div>
          ) : (
            filteredEvents.map((ev: TelemetryEvent) => (
              <EventLogItem
                key={ev.internalId || ev.id}
                type={ev.type || "UnknownEvent"}
                time={new Date(ev.timestamp).toLocaleTimeString()}
                active={
                  selectedEvent?.id === ev.id ||
                  selectedEvent?.internalId === ev.internalId
                }
                onClick={() => handleSelectEvent(ev)}
              />
            ))
          )}
        </div>
      </div>

      {/* --- MAIN CONTENT: JSON VIEWER --- */}
      <div className="flex-1 flex flex-col bg-[#08060d] relative">
        {/* Background Texture */}
        <div className="absolute inset-0 pointer-events-none z-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-[0.02]" />

        {!selectedEvent ? (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-700 relative z-10">
            <FileJson size={64} className="mb-6 opacity-20" />
            <div className="text-xs font-mono tracking-[0.2em] uppercase">
              Select a packet to inspect payload
            </div>
          </div>
        ) : (
          <div className="flex-1 flex flex-col h-full overflow-hidden relative z-10">
            {/* Detail Header */}
            <div className="px-6 py-5 border-b border-white/5 bg-[#0c0910] flex justify-between items-start">
              <div>
                <h2 className="text-xl font-bold text-cyan-100 font-mono tracking-tight flex items-center gap-2">
                  <span className="text-cyan-400">
                    <Code size={20} />
                  </span>
                  {selectedEvent.type}
                </h2>
                <div className="flex items-center gap-4 mt-2 text-xs text-gray-500 font-mono">
                  <span className="flex items-center gap-1.5">
                    <Clock size={12} className="text-cyan-300" />
                    {new Date(selectedEvent.timestamp).toLocaleString()}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <Terminal size={12} className="text-emerald-400" />
                    ID: {selectedEvent.id}
                  </span>
                </div>
              </div>
              <div className="flex flex-col items-end gap-2">
                <div className="px-3 py-1 rounded bg-cyan-500/10 border border-cyan-500/20 text-cyan-200 text-[10px] font-mono uppercase tracking-widest">
                  PAYLOAD INSPECTOR
                </div>
                {(onReplayToEvent || onReplayFromEvent) && (
                  <div className="flex items-center gap-2">
                    {onReplayToEvent && (
                      <button
                        onClick={handleReplayToEvent}
                        className="px-3 py-2 text-[9px] font-black text-white bg-[#132025] border border-[#2b3a40] rounded hover:bg-[#0ea5a4] transition-all uppercase tracking-widest"
                      >
                        <span className="inline-flex items-center gap-1">
                          <Rewind size={12} /> Replay To
                        </span>
                      </button>
                    )}
                    {onReplayFromEvent && (
                      <button
                        onClick={handleReplayFromEvent}
                        className="px-3 py-2 text-[9px] font-black text-white bg-[#1b2b2a] border border-[#274f4c] rounded hover:bg-[#0ea5a4] transition-all uppercase tracking-widest"
                      >
                        <span className="inline-flex items-center gap-1">
                          <FastForward size={12} /> Play From
                        </span>
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* JSON Content */}
            <div className="flex-1 overflow-auto p-6 custom-scrollbar">
              <div className="bg-[#050407] border border-white/5 rounded-lg p-1 shadow-inner relative group">
                <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <span className="text-[10px] text-gray-600 font-mono">
                    JSON
                  </span>
                </div>
                <SyntaxHighlighter data={selectedEvent.payload} />
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// --- SUB-COMPONENTS ---

function EventLogItem({
  type,
  time,
  active,
  onClick,
}: {
  type: string;
  time: string;
  active: boolean;
  onClick: () => void;
}) {
  // Determine color based on event type keyword for quick visual scanning
  let accentClass = "bg-gray-500";
  if (type.includes("Player")) accentClass = "bg-emerald-500";
  else if (type.includes("World")) accentClass = "bg-cyan-500";
  else if (type.includes("Combat") || type.includes("Damage"))
    accentClass = "bg-rose-500";
  else if (type.includes("Room") || type.includes("Dungeon"))
    accentClass = "bg-amber-500";

  return (
    <button
      onClick={onClick}
      className={`w-full text-left px-3 py-2.5 rounded border flex items-center justify-between gap-3 transition-all group relative overflow-hidden ${
        active
          ? "bg-cyan-500/10 border-cyan-500/40"
          : "border-transparent hover:bg-white/5 hover:border-white/5"
      }`}
    >
      {/* Active Indicator Bar */}
      {active && (
        <div className="absolute left-0 top-0 bottom-0 w-[2px] bg-cyan-400" />
      )}

      <div className="flex items-center gap-3 min-w-0">
        <div
          className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${active ? "animate-pulse shadow-[0_0_5px_currentColor] text-cyan-300" : "opacity-40"} ${accentClass}`}
        />
        <div className="min-w-0">
          <div
          className={`text-xs font-mono truncate ${active ? "text-cyan-100 font-bold" : "text-gray-400 group-hover:text-gray-300"}`}
          >
            {type.split(".").pop()} {/* Show only last part of class name */}
          </div>
        </div>
      </div>

      <div
        className={`text-[10px] font-mono flex-shrink-0 ${active ? "text-cyan-200" : "text-gray-600"}`}
      >
        {time}
      </div>
    </button>
  );
}

function SyntaxHighlighter({ data }: { data: any }) {
  const jsonString = useMemo(() => {
    if (!data) return "null";
    return JSON.stringify(data, null, 2);
  }, [data]);

  const highlightedHtml = useMemo(() => {
    let json = jsonString
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");

    return json.replace(
      /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
      function (match) {
        let cls = "text-amber-300";
        if (/^"/.test(match)) {
          if (/:$/.test(match)) {
            cls = "text-cyan-300 font-bold"; // key
          } else {
            cls = "text-emerald-300"; // string
          }
        } else if (/true|false/.test(match)) {
          cls = "text-rose-400 font-bold";
        } else if (/null/.test(match)) {
          cls = "text-gray-500 italic";
        }
        return '<span class="' + cls + '">' + match + "</span>";
      },
    );
  }, [jsonString]);

  return (
    <pre
      className="font-mono text-xs leading-relaxed p-4 overflow-x-auto text-gray-300 selection:bg-cyan-500/30"
      dangerouslySetInnerHTML={{ __html: highlightedHtml }}
    />
  );
}
