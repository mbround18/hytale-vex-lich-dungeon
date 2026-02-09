import React, { useState } from "react";
import {
  Activity,
  BookOpen,
  Box,
  Code,
  Cpu,
  Filter,
  Ghost,
  Layers,
  Search,
  Server,
  Terminal,
  Trash2,
} from "lucide-react";
import {
  Badge,
  ColorSwatch,
  DevLogCard,
  EventLogItem,
  IndexRow,
  KpiCard,
  NavButton,
  PlayerRow,
  Section,
  SyntaxHighlighter,
  TrackingCard,
  VexGlobalStyles,
  WikiCard,
} from "../components";

export default function DesignSystemApp() {
  const [activeTab, setActiveTab] = useState("components");
  const [selectedEventId, setSelectedEventId] = useState("1");

  const mockEvent = {
    type: "EntitySpawnedEvent",
    timestamp: new Date().toISOString(),
    data: {
      entityType: "Skeleton_Archer",
      world: { name: "instance-Lava-A" },
      room: { x: -1, z: 2 },
      meta: { difficulty: "Hard", variant: "Elite" },
    },
  };

  return (
    <div className="min-h-screen vex-grid pb-20">
      <VexGlobalStyles />

      <header className="sticky top-0 z-50 h-16 border-b border-[#30223f] bg-[#161025]/90 backdrop-blur-md flex items-center justify-between px-8 mb-12 shadow-[0_4px_30px_rgba(0,0,0,0.5)]">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-violet-600 flex items-center justify-center text-white shadow-[0_0_20px_rgba(139,92,246,0.4)]">
              <Ghost size={18} />
            </div>
            <div>
              <span className="block text-[9px] font-bold tracking-[0.25em] text-[#9f8cc9] uppercase">
                Vex System
              </span>
              <h1 className="text-sm font-bold text-white font-heading tracking-wide">
                Design System
              </h1>
            </div>
          </div>
          <div className="h-6 w-px bg-white/10 mx-2" />
          <nav className="flex gap-2">
            <NavButton
              active={activeTab === "foundations"}
              onClick={() => setActiveTab("foundations")}
            >
              Foundations
            </NavButton>
            <NavButton
              active={activeTab === "components"}
              onClick={() => setActiveTab("components")}
            >
              Components
            </NavButton>
            <NavButton
              active={activeTab === "layouts"}
              onClick={() => setActiveTab("layouts")}
            >
              Telemetry
            </NavButton>
            <NavButton
              active={activeTab === "devlogs"}
              onClick={() => setActiveTab("devlogs")}
            >
              Dev Logs
            </NavButton>
            <NavButton
              active={activeTab === "library"}
              onClick={() => setActiveTab("library")}
            >
              Knowledge Base
            </NavButton>
          </nav>
        </div>
        <div className="flex items-center gap-3">
          <div className="px-3 py-1 rounded-full bg-black/40 border border-white/10 flex items-center gap-2">
            <div className="w-1.5 h-1.5 rounded-full bg-green-400 relative animate-pulse-ring text-green-400" />
            <span className="text-[10px] font-bold uppercase tracking-wider text-[#9f8cc9]">
              System Live
            </span>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-8">
        {activeTab === "foundations" && (
          <>
            <Section title="Color Palette">
              <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-6">
                <ColorSwatch name="Vex Void" color="#08060d" hex="#08060d" />
                <ColorSwatch name="Vex Night" color="#120b1b" hex="#120b1b" />
                <ColorSwatch name="Vex Panel" color="#161025" hex="#161025" />
                <ColorSwatch name="Vex Violet" color="#8b5cf6" hex="#8b5cf6" />
                <ColorSwatch name="Vex Ember" color="#fbbf24" hex="#fbbf24" />
                <ColorSwatch name="Vex Green" color="#4ade80" hex="#4ade80" />
                <ColorSwatch name="Vex Rose" color="#f472b6" hex="#f472b6" />
                <ColorSwatch name="Vex Cyan" color="#38bdf8" hex="#38bdf8" />
              </div>
            </Section>

            <Section title="Typography">
              <div className="space-y-8 bg-black/20 p-8 rounded-2xl border border-white/5">
                <div>
                  <p className="text-xs text-[#9f8cc9] mb-2 uppercase tracking-widest font-bold">
                    Headings (Space Grotesk)
                  </p>
                  <h1 className="text-5xl font-heading font-bold text-white mb-2">
                    The Lich&apos;s Lair
                  </h1>
                  <h2 className="text-3xl font-heading font-bold text-white mb-2">
                    Dungeon Instance Alpha
                  </h2>
                  <h3 className="text-xl font-heading font-bold text-[#d7c9ff]">
                    Sector 7G - Active
                  </h3>
                </div>
                <div>
                  <p className="text-xs text-[#9f8cc9] mb-2 uppercase tracking-widest font-bold">
                    Body (IBM Plex Sans)
                  </p>
                  <p className="text-[#d7c9ff] max-w-2xl leading-relaxed">
                    The dungeon generation algorithm uses a wave-function
                    collapse model to stitch together pre-fabricated rooms. Each
                    room emits telemetry events upon player entry, allowing for
                    real-time topological reconstruction of the instance.
                  </p>
                </div>
                <div>
                  <p className="text-xs text-[#9f8cc9] mb-2 uppercase tracking-widest font-bold">
                    Monospace (JetBrains Mono)
                  </p>
                  <code className="text-sm text-violet-400 bg-violet-500/10 px-2 py-1 rounded">
                    event_id: &quot;8f7-22a-11b&quot; // timestamp: 167293992
                  </code>
                </div>
              </div>
            </Section>
          </>
        )}

        {activeTab === "components" && (
          <>
            <Section title="KPI Cards">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <KpiCard
                  title="Active Instances"
                  value="12"
                  icon={Server}
                  color="text-white"
                />
                <KpiCard
                  title="Live Entities"
                  value="843"
                  icon={Ghost}
                  color="text-amber-400"
                />
                <KpiCard
                  title="Rooms Forged"
                  value="2,094"
                  icon={Box}
                  color="text-green-400"
                />
                <KpiCard
                  title="Memory Load"
                  value="45%"
                  icon={Cpu}
                  color="text-cyan-400"
                />
              </div>
            </Section>

            <Section title="Tracking & Lists">
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <TrackingCard title="Player Census">
                  <div className="space-y-3">
                    <PlayerRow
                      name="MBRound18"
                      world="instance-Lava-A"
                      room="0,1"
                      status="active"
                    />
                    <PlayerRow
                      name="NullPointer"
                      world="instance-Void-B"
                      room="-2,4"
                      status="active"
                    />
                    <PlayerRow
                      name="VexMaster"
                      world="default"
                      room="Spawn"
                      status="active"
                    />
                  </div>
                </TrackingCard>

                <TrackingCard title="Entity Distribution">
                  <div className="space-y-4 pt-2">
                    {[
                      {
                        label: "Skeleton Archer",
                        count: 450,
                        total: 843,
                        color: "bg-violet-500",
                      },
                      {
                        label: "Ghoul",
                        count: 210,
                        total: 843,
                        color: "bg-green-500",
                      },
                      {
                        label: "Necromancer",
                        count: 85,
                        total: 843,
                        color: "bg-rose-500",
                      },
                    ].map((item) => (
                      <div key={item.label}>
                        <div className="flex justify-between text-xs font-bold text-[#d7c9ff] mb-1.5">
                          <span>{item.label}</span>
                          <span>{item.count}</span>
                        </div>
                        <div className="h-1.5 w-full bg-white/10 rounded-full overflow-hidden">
                          <div
                            className={`h-full ${item.color} shadow-[0_0_10px_currentColor]`}
                            style={{
                              width: `${(item.count / item.total) * 100}%`,
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </TrackingCard>
              </div>
            </Section>

            <Section title="Badges & UI Elements">
              <div className="flex flex-wrap gap-4 p-6 bg-[#161025] border border-[#30223f] rounded-2xl items-center">
                <Badge variant="default">Default Badge</Badge>
                <Badge variant="gold">Rare Drop</Badge>
                <Badge variant="green">Online</Badge>
                <Badge variant="rose">Critical Error</Badge>
                <Badge variant="cyan">Development</Badge>
                <div className="h-8 w-px bg-white/10 mx-4" />
                <button className="w-8 h-8 rounded-lg bg-violet-600 flex items-center justify-center text-white shadow-lg hover:bg-violet-500 transition-colors">
                  <Search size={14} />
                </button>
                <button className="w-8 h-8 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-[#9f8cc9] hover:text-white hover:bg-white/10 transition-colors">
                  <Trash2 size={14} />
                </button>
              </div>
            </Section>
          </>
        )}

        {activeTab === "layouts" && (
          <Section title="Telemetry View Pattern">
            <div className="flex h-[500px] border border-[#30223f] rounded-2xl overflow-hidden shadow-2xl">
              <div className="w-80 bg-[#120b1b] flex flex-col border-r border-[#30223f]">
                <div className="p-4 border-b border-[#30223f]">
                  <div className="relative">
                    <Search
                      className="absolute left-3 top-2.5 text-violet-500"
                      size={14}
                    />
                    <input
                      type="text"
                      placeholder="Filter stream..."
                      className="w-full bg-black/20 border border-violet-500/30 rounded-lg py-2 pl-9 pr-4 text-xs text-white focus:outline-none focus:border-violet-500"
                    />
                  </div>
                </div>
                <div className="flex-1 overflow-y-auto scroll-smooth p-2 pb-8 space-y-1">
                  <EventLogItem
                    type="EntitySpawned"
                    time="10:42:01"
                    data={{ id: "skel_01" }}
                    active={selectedEventId === "1"}
                    onClick={() => setSelectedEventId("1")}
                  />
                  <EventLogItem
                    type="RoomGenerated"
                    time="10:42:05"
                    data={{ prefab: "Hallway_L" }}
                    active={selectedEventId === "2"}
                    onClick={() => setSelectedEventId("2")}
                  />
                  <EventLogItem
                    type="PlayerMoved"
                    time="10:42:15"
                    data={{ user: "MBRound18" }}
                    active={selectedEventId === "3"}
                    onClick={() => setSelectedEventId("3")}
                  />
                </div>
                <div className="p-2 border-t border-[#30223f] bg-black/20">
                  <button className="w-full py-2 text-[10px] uppercase font-bold text-[#9f8cc9] hover:text-white transition-colors">
                    <Activity size={12} className="inline mr-2" /> Live Stream
                  </button>
                </div>
              </div>

              <div className="flex-1 bg-[#08060d]/90 backdrop-blur flex flex-col p-8 pb-12 overflow-y-auto scroll-smooth relative">
                <div className="absolute top-0 right-0 p-4 opacity-10">
                  <Code size={120} />
                </div>

                <div className="relative z-10 max-w-2xl mx-auto w-full pb-8">
                  <div className="flex items-center gap-3 mb-6">
                    <div className="p-2 bg-violet-500/10 rounded-lg border border-violet-500/30 text-violet-400">
                      <Terminal size={20} />
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-white font-heading">
                        Event Details
                      </h3>
                      <p className="text-xs text-[#9f8cc9] font-mono">
                        ID: {mockEvent.timestamp}
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4 mb-6">
                    <div className="p-3 bg-white/5 rounded border border-white/5">
                      <p className="text-[10px] uppercase text-[#9f8cc9] font-bold">
                        Event Type
                      </p>
                      <p className="text-sm text-violet-300 font-mono font-bold">
                        EntitySpawnedEvent
                      </p>
                    </div>
                    <div className="p-3 bg-white/5 rounded border border-white/5">
                      <p className="text-[10px] uppercase text-[#9f8cc9] font-bold">
                        Size
                      </p>
                      <p className="text-sm text-white font-mono">248 bytes</p>
                    </div>
                  </div>

                  <div className="rounded-xl border border-violet-500/20 bg-[#0c0812] p-6 shadow-2xl relative overflow-hidden group">
                    <div className="absolute top-0 right-0 px-3 py-1 bg-violet-500/10 text-violet-400 text-[10px] font-bold rounded-bl-xl border-l border-b border-violet-500/20">
                      JSON PAYLOAD
                    </div>
                    <SyntaxHighlighter data={mockEvent.data} />
                  </div>
                </div>
              </div>
            </div>
          </Section>
        )}

        {activeTab === "devlogs" && (
          <Section title="Developer Logs">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <DevLogCard
                title="Protocol Update: Vector Synchronization"
                date="2026-02-14"
                author="VexMaster"
                tags={["Network", "Optimization", "v2.1.0"]}
                excerpt="We've overhauled the entity interpolation logic to handle high-latency packet loss in generated sub-chunks. This results in 40% smoother movement for connected clients."
              />
              <DevLogCard
                title="New Prefab: The Obsidian Hall"
                date="2026-02-12"
                author="LevelDesigner"
                tags={["Content", "Art", "v2.0.5"]}
                excerpt="Added a new rare room type that spawns in the deep lava zones. Contains 3 elite spawners and a unique loot chest trigger."
              />
              <DevLogCard
                title="Telemetry Pipeline Hotfix"
                date="2026-02-10"
                author="DevOps"
                tags={["Backend", "Hotfix"]}
                excerpt="Fixed a serialization issue where 'RoomEnteredEvent' was dropping the Z-coordinate for negative values."
              />
            </div>
          </Section>
        )}

        {activeTab === "library" && (
          <Section title="Knowledge Base">
            <div className="flex flex-col lg:flex-row gap-8">
              <div className="lg:w-1/3 space-y-4">
                <div className="p-4 rounded-xl bg-white/5 border border-white/10">
                  <div className="relative mb-4">
                    <Search
                      className="absolute left-3 top-2.5 text-[#9f8cc9]"
                      size={14}
                    />
                    <input
                      type="text"
                      placeholder="Search knowledge..."
                      className="w-full bg-black/40 border border-[#30223f] rounded-lg py-2 pl-9 pr-4 text-xs text-white focus:outline-none focus:border-violet-500"
                    />
                  </div>
                  <div className="space-y-1">
                    <div className="flex items-center justify-between p-2 rounded hover:bg-white/5 cursor-pointer text-violet-300 font-bold bg-white/5 border-l-2 border-violet-500">
                      <span className="text-xs">Entity Codex</span>
                      <span className="text-[10px] bg-violet-500/20 px-2 py-0.5 rounded">
                        12
                      </span>
                    </div>
                    <div className="flex items-center justify-between p-2 rounded hover:bg-white/5 cursor-pointer text-[#9f8cc9]">
                      <span className="text-xs">Room Prefabs</span>
                      <span className="text-[10px] bg-white/5 px-2 py-0.5 rounded">
                        45
                      </span>
                    </div>
                    <div className="flex items-center justify-between p-2 rounded hover:bg-white/5 cursor-pointer text-[#9f8cc9]">
                      <span className="text-xs">Event Schema</span>
                      <span className="text-[10px] bg-white/5 px-2 py-0.5 rounded">
                        8
                      </span>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <WikiCard
                    title="Bestiary"
                    description="Enemy stats & AI"
                    icon={Ghost}
                    color="text-rose-400"
                  />
                  <WikiCard
                    title="Artifacts"
                    description="Loot table data"
                    icon={Box}
                    color="text-amber-400"
                  />
                </div>
              </div>

              <div className="lg:w-2/3">
                <div className="mb-4 flex items-center justify-between">
                  <h3 className="text-sm font-heading font-bold text-white uppercase tracking-widest">
                    <BookOpen
                      size={14}
                      className="inline mr-2 text-violet-500"
                    />{" "}
                    Entity Codex
                  </h3>
                  <div className="flex gap-2">
                    <button className="p-2 rounded bg-white/5 hover:bg-white/10 text-[#9f8cc9]">
                      <Filter size={14} />
                    </button>
                    <button className="p-2 rounded bg-white/5 hover:bg-white/10 text-[#9f8cc9]">
                      <Layers size={14} />
                    </button>
                  </div>
                </div>

                <div className="bg-[#161025] border border-[#30223f] rounded-2xl overflow-hidden">
                  <div className="flex items-center justify-between p-3 border-b border-[#30223f] bg-black/20 text-[10px] font-bold text-[#9f8cc9] uppercase tracking-wider">
                    <span className="pl-2">Entity ID / Name</span>
                    <span className="pr-4">Status</span>
                  </div>
                  <div>
                    <IndexRow
                      id="E-001"
                      title="Skeleton Archer"
                      type="Hostile / Ranged"
                      status="Stable"
                      modified="2d ago"
                    />
                    <IndexRow
                      id="E-002"
                      title="Ghoul"
                      type="Hostile / Melee"
                      status="Stable"
                      modified="5d ago"
                    />
                    <IndexRow
                      id="E-003"
                      title="Necromancer"
                      type="Hostile / Magic"
                      status="Beta"
                      modified="12h ago"
                    />
                    <IndexRow
                      id="E-004"
                      title="Lich King"
                      type="Boss / Hybrid"
                      status="Deprecated"
                      modified="1mo ago"
                    />
                    <IndexRow
                      id="E-005"
                      title="Vex Spirit"
                      type="Passive / Ambient"
                      status="Stable"
                      modified="1wk ago"
                    />
                  </div>
                </div>
              </div>
            </div>
          </Section>
        )}
      </div>
    </div>
  );
}
