import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { AnsiUp } from "ansi_up";
import { Badge, StatusPill } from "ui-shared/components";
import { createWebSocketStream } from "ui-shared/streams";

type ProcessId = "gradle" | "docker" | "pnpm";

type ConnectionStatus = "CONNECTED" | "RETRYING" | "DISCONNECTED" | "OFFLINE";

type ConnectionState = {
  status: ConnectionStatus;
  retryIn: number;
};

type ProcessState = {
  running: boolean;
  log_count: number;
};

type LogEntry = {
  time: string;
  text: string;
};

type ToastState = {
  msg: string;
  icon: string;
};

type IconProps = {
  path: string;
  className?: string;
};

type StatusDotProps = {
  status: ConnectionStatus;
};

type LogLineProps = {
  text: string;
  time: string;
};

type TerminalProps = {
  logs: LogEntry[];
  filter: string;
  autoScroll: boolean;
  onScrollStateChange: (isAtBottom: boolean) => void;
};

type ProcessMeta = {
  id: ProcessId;
  name: string;
  icon: string;
  color: string;
};

const ansi = new AnsiUp();

const PROCESSES: ProcessMeta[] = [
  {
    id: "gradle",
    name: "Gradle Daemon",
    icon: "🐘",
    color: "text-blue-400",
  },
  {
    id: "docker",
    name: "Docker Engine",
    icon: "🐳",
    color: "text-cyan-400",
  },
  {
    id: "pnpm",
    name: "Vite Client",
    icon: "📦",
    color: "text-yellow-400",
  },
];

const API_BASE = window.location.origin;
const WS_PROTOCOL =
  window.location.protocol === "https:" ||
  window.location.href.startsWith("blob:")
    ? "wss://"
    : "ws://";
const WS_BASE = WS_PROTOCOL + window.location.host;

const Icon = ({ path, className }: IconProps) => (
  <svg
    className={className}
    fill="none"
    stroke="currentColor"
    viewBox="0 0 24 24"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="2"
      d={path}
    />
  </svg>
);

const StatusDot = ({ status }: StatusDotProps) => {
  let color = "bg-gray-500";
  let animation = "";

  if (status === "CONNECTED") {
    color = "bg-green-500";
    animation = "animate-pulse-soft";
  } else if (status === "RETRYING") {
    color = "bg-yellow-500";
    animation = "animate-pulse-fast";
  } else if (status === "OFFLINE" || status === "DISCONNECTED") {
    color = "bg-red-500";
  }

  return <div className={`w-2 h-2 rounded-full ${color} ${animation}`} />;
};

const LogLine = ({ text, time }: LogLineProps) => {
  const htmlContent = useMemo(() => {
    let html = ansi.ansi_to_html(text);
    html = html.replace(/(https?:\/\/[^\s<]+)/g, (url) => {
      const href = url.replace("0.0.0.0", "127.0.0.1");
      return `<a href="${href}" target="_blank" class="log-link">${url}</a>`;
    });
    return { __html: html };
  }, [text]);

  let borderColor = "border-transparent";
  const lower = text.toLowerCase();
  if (
    lower.includes("error") ||
    lower.includes("failed") ||
    lower.includes("fatal")
  ) {
    borderColor = "border-red-500";
  } else if (lower.includes("warn")) {
    borderColor = "border-yellow-500";
  } else if (lower.includes("success") || lower.includes("ready")) {
    borderColor = "border-green-500";
  }

  const isSystem = text.startsWith(">>");

  return (
    <div
      className={`flex gap-4 px-2 py-0.5 border-l-[3px] hover:bg-white/5 transition-colors ${borderColor}`}
    >
      <span className="text-gray-600 font-mono text-[11px] pt-0.5 opacity-50 select-none shrink-0">
        {time}
      </span>
      <span
        className={`font-mono text-[13px] break-all whitespace-pre-wrap ${
          isSystem ? "text-blue-400 font-bold italic" : "text-gray-300"
        }`}
        dangerouslySetInnerHTML={htmlContent}
      />
    </div>
  );
};

const Terminal = ({
  logs,
  filter,
  autoScroll,
  onScrollStateChange,
}: TerminalProps) => {
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const visibleLogs = useMemo(() => {
    if (!filter) return logs;
    const lowerFilter = filter.toLowerCase();
    return logs.filter((l) => l.text.toLowerCase().includes(lowerFilter));
  }, [logs, filter]);

  useEffect(() => {
    if (autoScroll && bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: "auto" });
    }
  }, [visibleLogs, autoScroll]);

  const handleScroll = (e) => {
    const { scrollTop, scrollHeight, clientHeight } = e.target;
    const isAtBottom = scrollHeight - clientHeight <= scrollTop + 30;
    onScrollStateChange(isAtBottom);
  };

  return (
    <div
      ref={containerRef}
      className="flex-1 overflow-y-auto p-4 space-y-0.5"
      onScroll={handleScroll}
    >
      {visibleLogs.map((log, i) => (
        <LogLine key={i} text={log.text} time={log.time} />
      ))}
      <div ref={bottomRef} />
    </div>
  );
};

const App = () => {
  const [activeProcessId, setActiveProcessId] = useState<ProcessId>("gradle");

  const [connState, setConnState] = useState<
    Record<ProcessId, ConnectionState>
  >({
    gradle: { status: "DISCONNECTED", retryIn: 0 },
    docker: { status: "DISCONNECTED", retryIn: 0 },
    pnpm: { status: "DISCONNECTED", retryIn: 0 },
  });

  const [processData, setProcessData] = useState<
    Record<ProcessId, ProcessState>
  >({
    gradle: { running: false, log_count: 0 },
    docker: { running: false, log_count: 0 },
    pnpm: { running: false, log_count: 0 },
  });

  const [logs, setLogs] = useState<Record<ProcessId, LogEntry[]>>({
    gradle: [],
    docker: [],
    pnpm: [],
  });
  const [autoScroll, setAutoScroll] = useState(true);
  const [filter, setFilter] = useState("");
  const [toast, setToast] = useState<ToastState | null>(null);
  const [isDemoMode, setIsDemoMode] = useState(false);
  const [isOffline, setIsOffline] = useState(!navigator.onLine);

  const subscriptionsRef = useRef<
    Record<ProcessId, { unsubscribe?: () => void }>
  >({} as Record<ProcessId, { unsubscribe?: () => void }>);
  const connStatusRef = useRef<Record<ProcessId, ConnectionStatus>>(
    {} as Record<ProcessId, ConnectionStatus>,
  );

  const activeProcess =
    PROCESSES.find((p) => p.id === activeProcessId) ?? PROCESSES[0];

  const addLog = useCallback((id: ProcessId, text: string) => {
    const time = new Date().toLocaleTimeString("en-GB", {
      hour12: false,
    });
    setLogs((prev) => {
      const newLogs = [...prev[id], { time, text }];
      if (newLogs.length > 3000) newLogs.shift();
      return { ...prev, [id]: newLogs };
    });
  }, []);

  const generateMockLogs = (id: ProcessId): LogEntry[] => {
    const msgs = [
      `[INFO] Starting ${id} service...`,
      `[INFO] Service listening on 127.0.0.1:${id === "docker" ? "2375" : "8080"}`,
      `>> System: Ready to accept connections`,
    ];
    return msgs.map((text) => ({
      time: new Date().toLocaleTimeString("en-GB", { hour12: false }),
      text,
    }));
  };

  const updateConnState = useCallback(
    (id: ProcessId, next: ConnectionState) => {
      setConnState((prev) => ({ ...prev, [id]: next }));

      const prevStatus = connStatusRef.current[id];
      if (next.status !== prevStatus) {
        if (next.status === "CONNECTED") {
          addLog(id, ">> Socket: Connected");
        }
        if (next.status === "RETRYING") {
          addLog(id, `>> Socket lost. Reconnecting in ${next.retryIn}s...`);
        }
      }
      connStatusRef.current[id] = next.status;
    },
    [addLog],
  );

  const disconnectAll = useCallback(() => {
    Object.values(subscriptionsRef.current).forEach((sub) =>
      sub?.unsubscribe?.(),
    );
    subscriptionsRef.current = {};
  }, []);

  const subscribeProcess = useCallback(
    (id: ProcessId) => {
      if (!navigator.onLine) {
        updateConnState(id, { status: "OFFLINE", retryIn: 0 });
        return;
      }

      subscriptionsRef.current[id]?.unsubscribe?.();

      const stream$ = createWebSocketStream({
        url: () => `${WS_BASE}/ws/${id}`,
        parse: (event) => event.data,
        onStatus: (status) => {
          if (!navigator.onLine) return;
          if (status.connected) {
            updateConnState(id, { status: "CONNECTED", retryIn: 0 });
            return;
          }
          if (status.retryIn) {
            updateConnState(id, {
              status: "RETRYING",
              retryIn: status.retryIn,
            });
            return;
          }
          updateConnState(id, { status: "DISCONNECTED", retryIn: 0 });
        },
      });

      const subscription = stream$.subscribe({
        next: (payload) => {
          try {
            const data = JSON.parse(payload);
            if (data.type === "log") addLog(id, data.line);
          } catch {
            addLog(id, payload);
          }
        },
        error: () => {
          // errors are handled by retry logic
        },
      });

      subscriptionsRef.current[id] = subscription;
    },
    [addLog, updateConnState],
  );

  const reconnectAll = useCallback(() => {
    disconnectAll();
    PROCESSES.forEach((p) => subscribeProcess(p.id));
    showToast("Force Reconnecting...", "🔄");
  }, [disconnectAll, subscribeProcess]);

  const handleControl = async (action: "start" | "stop" | "restart") => {
    try {
      const response = await fetch(`${API_BASE}/api/control`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          process: activeProcessId,
          action: action,
        }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || "Control request failed");
      }

      showToast(`${action.toUpperCase()} sent to ${activeProcess.name}`, "✓");
      addLog(
        activeProcessId,
        `>> Control: ${action.toUpperCase()} signal sent`,
      );
    } catch (e) {
      showToast(`Failed to ${action}: ${e.message}`, "❌");
      addLog(activeProcessId, `>> Error: ${e.message}`);
    }
  };

  const showToast = (msg: string, icon: string) => {
    setToast({ msg, icon });
    setTimeout(() => setToast(null), 3000);
  };

  const copyLogs = () => {
    const text = logs[activeProcessId]
      .map((l) => `[${l.time}] ${l.text}`)
      .join("\n");
    navigator.clipboard.writeText(text);
    showToast("Logs copied to clipboard", "📋");
  };

  useEffect(() => {
    const handleOnline = () => {
      setIsOffline(false);
      addLog("gradle", ">> Network Online. Reconnecting services...");
      reconnectAll();
    };
    const handleOffline = () => {
      setIsOffline(true);
      disconnectAll();
      setConnState((prev) => {
        const next = { ...prev };
        Object.keys(next).forEach(
          (k) => (next[k] = { status: "OFFLINE", retryIn: 0 }),
        );
        return next;
      });
    };

    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, [reconnectAll, addLog, disconnectAll]);

  useEffect(() => {
    PROCESSES.forEach((p) => subscribeProcess(p.id));
    return () => disconnectAll();
  }, [subscribeProcess, disconnectAll]);

  useEffect(() => {
    const fetchStatus = async () => {
      if (isOffline) return;
      try {
        const res = await fetch(`${API_BASE}/api/status`);
        const contentType = res.headers.get("content-type");

        if (
          !res.ok ||
          !contentType ||
          !contentType.includes("application/json")
        ) {
          throw new Error("Invalid API response");
        }

        const data = (await res.json()) as {
          processes: Record<ProcessId, ProcessState>;
        };
        setProcessData(data.processes);
        setIsDemoMode(false);
      } catch (e) {
        if (!isDemoMode) {
          console.warn("Backend unavailable, enabling demo mode");
          setIsDemoMode(true);
          setProcessData((prev) => ({
            gradle: { ...prev.gradle, running: true, log_count: 5 },
            docker: { ...prev.docker, running: true, log_count: 5 },
            pnpm: { ...prev.pnpm, running: true, log_count: 5 },
          }));
        }
      }
    };
    fetchStatus();
    const interval = setInterval(fetchStatus, 3000);

    return () => clearInterval(interval);
  }, [isDemoMode, isOffline]);

  useEffect(() => {
    const fetchLogs = async () => {
      if (logs[activeProcessId].length > 0) return;
      try {
        const res = await fetch(`${API_BASE}/api/logs/${activeProcessId}`);
        const contentType = res.headers.get("content-type");
        if (
          !res.ok ||
          !contentType ||
          !contentType.includes("application/json")
        ) {
          throw new Error("Invalid API");
        }

        const data = (await res.json()) as { logs: string[] };
        const history = data.logs.map((text) => ({
          time: new Date().toLocaleTimeString(),
          text,
        }));
        setLogs((prev) => ({ ...prev, [activeProcessId]: history }));
      } catch (e) {
        if (logs[activeProcessId].length === 0) {
          setLogs((prev) => ({
            ...prev,
            [activeProcessId]: generateMockLogs(activeProcessId),
          }));
        }
      }
    };
    fetchLogs();
    setFilter("");
    setAutoScroll(true);
  }, [activeProcessId, logs]);

  const currentConn = connState[activeProcessId] || {
    status: "DISCONNECTED",
    retryIn: 0,
  };

  let footerStatusText = currentConn.status;
  if (currentConn.status === "RETRYING") {
    footerStatusText = `RETRYING IN ${currentConn.retryIn}s`;
  }

  return (
    <div className="flex flex-col h-full bg-vex-void text-text-body">
      <header className="h-14 border-b border-vex-line bg-vex-panel flex items-center justify-between px-4 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-vex-violet rounded flex items-center justify-center font-bold text-white shadow-vex-glow">
            H
          </div>
          <h1 className="font-title font-semibold text-sm tracking-tight text-text-strong">
            HYTALE <span className="text-text-muted">DEV_MANAGER</span>
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-[12px] font-medium text-text-muted">
            SESSION:
          </span>
          <StatusPill
            status={isDemoMode ? "Demo Mode" : "Connected"}
            variant={isDemoMode ? "warning" : "live"}
            className="text-[9px]"
          />
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        <aside className="w-64 border-r border-vex-line bg-vex-night flex flex-col shrink-0">
          <div className="p-4 border-b border-vex-line">
            <button
              onClick={reconnectAll}
              className="w-full py-2 bg-vex-panel hover-bg-vex-panel-strong border border-vex-line rounded text-xs font-semibold transition-colors flex items-center justify-center gap-2 text-text-body"
            >
              <Icon
                path="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                className="w-3 h-3"
              />
              RECONNECT SOCKETS
            </button>
          </div>
          <nav className="flex-1 overflow-y-auto p-2 space-y-1">
            {PROCESSES.map((p) => {
              const data = processData[p.id];
              const conn = connState[p.id];
              const isActive = activeProcessId === p.id;

              let statusLabel = "STOPPED";
              if (data.running) statusLabel = "RUNNING";
              if (conn.status === "RETRYING") statusLabel = "RECONNECTING";
              if (conn.status === "OFFLINE") statusLabel = "OFFLINE";
              const statusVariant =
                statusLabel === "RUNNING"
                  ? "green"
                  : statusLabel === "RECONNECTING"
                    ? "gold"
                    : statusLabel === "OFFLINE"
                      ? "rose"
                      : "default";

              return (
                <button
                  key={p.id}
                  onClick={() => setActiveProcessId(p.id)}
                  className={`w-full text-left p-3 rounded-lg flex items-center justify-between transition-all duration-200 ${
                    isActive
                      ? "bg-vex-panel-strong border border-vex-line"
                      : "hover-bg-vex-panel border border-transparent"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={`text-lg ${isActive ? "" : "grayscale opacity-70"}`}
                    >
                      {p.icon}
                    </div>
                    <div className="flex flex-col">
                      <span
                        className={`text-[13px] font-semibold leading-tight ${
                          isActive ? "text-text-strong" : "text-text-muted"
                        }`}
                      >
                        {p.name}
                      </span>
                      <div className="mt-1">
                        <Badge variant={statusVariant}>{statusLabel}</Badge>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="flex flex-col items-end text-[9px] font-mono text-text-muted">
                      <span>BUFF</span>
                      <span>{data.log_count}</span>
                    </div>
                    <StatusDot status={conn.status} />
                  </div>
                </button>
              );
            })}
          </nav>
        </aside>

        <main className="flex-1 flex flex-col bg-vex-void overflow-hidden relative">
          <div className="h-14 border-b border-vex-line bg-vex-panel flex items-center justify-between px-6 shrink-0 z-10">
            <div className="flex items-center gap-4">
              <h2 className="text-lg font-bold flex items-center gap-2 text-text-strong font-title">
                {activeProcess.icon} {activeProcess.name}
              </h2>
              <div className="flex items-center gap-2 px-2 py-1 rounded bg-black/30 border border-white/5 text-[11px] font-mono">
                <span className="text-text-muted">PID:</span>
                <span className="text-vex-cyan">
                  {processData[activeProcessId]?.running ? "ACTIVE" : "---"}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="relative group">
                <div className="absolute inset-y-0 left-0 flex items-center pl-2.5 pointer-events-none text-text-muted">
                  <Icon
                    path="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                    className="w-3.5 h-3.5"
                  />
                </div>
                <input
                  type="text"
                  placeholder="Filter logs..."
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                  className="bg-vex-night border border-vex-line text-text-body text-xs rounded-md pl-8 pr-3 py-1.5 focus:outline-none focus:border-vex-violet w-48 transition-all"
                />
              </div>
              <div className="w-[1px] h-6 bg-vex-line"></div>
              <button
                onClick={() => handleControl("start")}
                className="px-3 py-1.5 bg-necro-green hover:brightness-105 rounded text-black text-xs font-bold flex items-center gap-1.5 transition-all active:scale-95 shadow-sm btn-accent"
              >
                <Icon
                  path="M4.516 7.548c0.436-0.446 1.043-0.481 1.576 0L10 11.295l3.908-3.747c0.533-0.481 1.141-0.446 1.574 0 0.436 0.445 0.408 1.197 0 1.615l-4.695 4.502c-0.533 0.481-1.408 0.481-1.94 0l-4.695-4.502c-0.408-0.418-0.436-1.17 0-1.615z"
                  className="w-3 h-3"
                />
                START
              </button>
              <button
                onClick={() => handleControl("stop")}
                className="px-3 py-1.5 bg-vex-rose hover:brightness-105 rounded text-black text-xs font-bold flex items-center gap-1.5 transition-all active:scale-95 shadow-sm btn-accent"
              >
                <div className="w-2.5 h-2.5 bg-black rounded-sm"></div> STOP
              </button>
              <button
                onClick={() => handleControl("restart")}
                className="px-3 py-1.5 bg-ancient-gold hover:brightness-105 rounded text-black text-xs font-bold flex items-center gap-1.5 transition-all active:scale-95 shadow-sm btn-accent"
              >
                <Icon
                  path="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                  className="w-3 h-3"
                />
                RESTART
              </button>
              <div className="w-[1px] h-6 bg-vex-line"></div>
              <button
                onClick={copyLogs}
                className="p-2 text-text-muted hover-text-strong hover-bg-vex-panel rounded transition-colors"
                title="Copy"
              >
                <Icon
                  path="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3"
                  className="w-4 h-4"
                />
              </button>
              <button
                onClick={() =>
                  setLogs((prev) => ({ ...prev, [activeProcessId]: [] }))
                }
                className="p-2 text-text-muted hover-text-strong hover-bg-vex-panel rounded transition-colors"
                title="Clear"
              >
                <Icon
                  path="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                  className="w-4 h-4"
                />
              </button>
            </div>
          </div>

          <Terminal
            logs={logs[activeProcessId]}
            filter={filter}
            autoScroll={autoScroll}
            onScrollStateChange={setAutoScroll}
          />

          <footer className="h-8 border-t border-vex-line bg-vex-night flex items-center justify-between px-4 shrink-0 text-[10px] font-bold text-text-muted tracking-wider select-none z-10">
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-1.5">
                <StatusDot status={currentConn.status} />
                SOCKET:{" "}
                <span className="text-text-body">{footerStatusText}</span>
              </div>
              <div>
                BUFFER:{" "}
                <span className="text-text-body">
                  {logs[activeProcessId].length} LINES
                </span>
              </div>
            </div>
            <div className="flex items-center gap-4">
              {!autoScroll && (
                <button
                  onClick={() => setAutoScroll(true)}
                  className="text-vex-cyan hover:underline cursor-pointer flex items-center gap-1"
                >
                  <span>↓</span> AUTO-SCROLL PAUSED
                </button>
              )}
            </div>
          </footer>
        </main>
      </div>

      {toast && (
        <div className="fixed bottom-12 right-6 bg-vex-violet text-white px-4 py-2 rounded-lg shadow-2xl flex items-center gap-3 border border-vex-violet/50 animate-[slideUp_0.3s_ease-out]">
          <div>{toast.icon}</div>
          <div className="text-sm font-medium">{toast.msg}</div>
        </div>
      )}
    </div>
  );
};

export default App;
