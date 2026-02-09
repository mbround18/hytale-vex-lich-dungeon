import React, { useEffect, useMemo, useRef, useState } from 'react';
import * as d3 from 'd3';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Globe, 
  Activity, 
  Search, 
  Cpu, 
  Users, 
  Maximize2, 
  AlertOctagon, 
  Terminal,
  Server,
  Database
} from 'lucide-react';
import api from '../api';
import { getStyledRoom } from './map/instanceHandlers';

// --- API Types (Based on provided OpenAPI Spec) ---
interface EventEnvelope {
  id: number;
  timestamp: number;
  type: string;
  payload: any;
}

interface SystemStats {
  uptime_ms: number;
  memory_free: number;
  memory_max: number;
  memory_total: number;
  threads_active: number;
}

interface EventStats {
  buffer_size: number;
  clients_connected: number;
  registered_types: number;
}

interface WorldStats {
  name: string;
  players: number;
  loaded_chunks?: number;
}

interface ServerStats {
  system: SystemStats;
  events: EventStats;
  worlds: WorldStats[];
}

// --- Component Types ---
interface Room {
  x: number;
  z: number;
  prefab: string;
  type?: string;
  size?: { w: number; h: number };
  roomKey?: string;
  stats?: {
    entities?: number;
    kills?: number;
    order?: number;
    generatedAt?: string;
  };
}

interface Instance {
  name: string;
  rooms: Room[];
  active: boolean;
  status: string;
  players: Set<string>;
  gridW: number;
  gridH: number;
  minX: number;
  minZ: number;
  renderX: number;
  renderY: number;
  isVexDungeon?: boolean;
  maxRoomOrder?: number;
  stats?: {
    entityCount?: number;
    killCount?: number;
    roomCount?: number;
  };
  roomStats?: Record<string, { entities: number; kills: number; order?: number; generatedAt?: string }>;
  playerStats?: Record<string, { kills: number; points: number }>;
}

interface Player {
  uuid: string;
  name: string;
  world: string;
  roomKey: string;
  playerId?: string;
  id?: string;
  health?: number;
  stamina?: number;
  stats?: {
    health?: number;
    stamina?: number;
  };
}

interface MapViewProps {
  worldState: {
    activeInstances?: Record<string, any>;
    archivedInstances?: any[];
    players?: Record<string, Player>;
  };
  getRoomSizeForRoom: (room: any) => { w: number; h: number };
}

// --- Constants ---
const TILE_SIZE = 24;
const GAP_TILES = 4;
const MAX_ROW_TILES = 64;

export default function MapView({ worldState, getRoomSizeForRoom }: MapViewProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  
  // State
  const [dimensions, setDimensions] = useState({ w: 0, h: 0 });
  const [transform, setTransform] = useState({ k: 1, x: 0, y: 0 });
  const [hoveredData, setHoveredData] = useState<{ x: number, y: number, content: React.ReactNode } | null>(null);
  const [selectedWorldId, setSelectedWorldId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  
  // API Data State
  const [serverStats, setServerStats] = useState<ServerStats | null>(null);
  const [knownWorlds, setKnownWorlds] = useState<WorldStats[]>([]);

  // 1. Data Polling (Stats & Metadata)
  useEffect(() => {
    const fetchData = async () => {
      try {
        const [statsRes, worldsRes] = await Promise.all([
          api.get('/stats'),
          api.get('/metadata/worlds')
        ]);
        setServerStats(statsRes.data);
        setKnownWorlds(worldsRes.data);
      } catch (err) {
        console.error("Failed to fetch debug stats", err);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, 5000); // Poll every 5s
    return () => clearInterval(interval);
  }, []);

  // 2. Handle Resize Observer
  useEffect(() => {
    if (!containerRef.current) return;
    const observer = new ResizeObserver((entries) => {
      const { width, height } = entries[0].contentRect;
      setDimensions({ w: width, h: height });
    });
    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, []);

  // 3. World Manifest (Aggregating all known worlds from API + Active Stream)
  const worldManifest = useMemo(() => {
    const manifest = new Map<string, { 
      name: string; 
      players: number; 
      type: 'dungeon' | 'world'; 
      active: boolean;
      isVex: boolean;
      source: 'api' | 'stream' | 'both';
    }>();

    // Helper to register
    const register = (name: string, active: boolean, hasRooms: boolean, source: 'api' | 'stream') => {
      if (!name) return;
      const isVex = name.toLowerCase().includes('vex_the_lich');
      const existing = manifest.get(name);
      
      const combinedSource = existing ? 'both' : source;
      
      manifest.set(name, {
        name,
        active: active || (existing?.active ?? false),
        type: hasRooms ? 'dungeon' : (existing?.type === 'dungeon' ? 'dungeon' : 'world'),
        players: existing?.players || 0,
        isVex,
        source: combinedSource
      });
    };

    // 1. Ingest API Worlds (Baseline)
    knownWorlds.forEach(w => {
      register(w.name, true, false, 'api'); // Assume API worlds are "active" in the universe sense
      const entry = manifest.get(w.name);
      if (entry) entry.players = w.players;
    });

    // 2. Ingest Active Instances (Stream)
    Object.values(worldState.activeInstances || {}).forEach((inst: any) => {
      register(inst.name, true, true, 'stream');
    });

    // 3. Ingest Archived
    (worldState.archivedInstances || []).forEach((inst: any) => {
      register(inst.name || inst.id, false, true, 'stream');
    });

    // 4. Ingest Players from Stream (Real-time override for player counts)
    const streamPlayerCounts = new Map<string, number>();
    Object.values(worldState.players || {}).forEach((p: Player) => {
      if (!manifest.has(p.world)) {
        register(p.world, true, false, 'stream');
      }
      streamPlayerCounts.set(p.world, (streamPlayerCounts.get(p.world) || 0) + 1);
    });
    
    // Update counts from stream if available (more real-time than 5s poll)
    streamPlayerCounts.forEach((count, worldName) => {
      const entry = manifest.get(worldName);
      if (entry) entry.players = count;
    });

    return Array.from(manifest.values()).sort((a, b) => {
      // Priority: Active Vex > Active Dungeon > Active World > Inactive
      if (a.isVex !== b.isVex) return a.isVex ? -1 : 1;
      if (a.active !== b.active) return a.active ? -1 : 1;
      return b.players - a.players;
    });
  }, [worldState, knownWorlds]);

  // 4. Layout Engine
  const layoutData = useMemo(() => {
    const allActive = Object.values(worldState.activeInstances || {});
    const allArchived = (worldState.archivedInstances || []).map((arch: any) => ({
      ...arch,
      rooms: Object.values(arch?.data?.rooms || {}),
      active: false,
      status: 'archived'
    }));

    let rawInstances = [...allActive];
    allArchived.forEach(arch => {
      if (!rawInstances.find(i => i.name === arch.name)) rawInstances.push(arch);
    });

    if (selectedWorldId) {
      rawInstances = rawInstances.filter(i => i.name === selectedWorldId);
    } 

    if (rawInstances.length === 0) return null;

    const processedInstances = rawInstances.map(inst => {
      const rooms: Room[] = Object.values(inst.rooms || {});
      const roomStats = inst.roomStats || {};
      const effectiveRooms = rooms.length
        ? rooms.map((room) => {
            const roomKey = `${room.x},${room.z}`;
            return {
              ...room,
              roomKey,
              stats: roomStats[roomKey]
            };
          })
        : [{ x: 0, z: 0, prefab: 'Empty', size: { w: 1, h: 1 }, roomKey: '0,0', stats: { entities: 0, kills: 0 } }];
      
      let minX = Infinity, minZ = Infinity, maxX = -Infinity, maxZ = -Infinity;
      effectiveRooms.forEach(r => {
        const s = getRoomSizeForRoom(r);
        minX = Math.min(minX, r.x);
        minZ = Math.min(minZ, r.z);
        maxX = Math.max(maxX, r.x + (s.w - 1));
        maxZ = Math.max(maxZ, r.z + (s.h - 1));
      });

      const maxRoomOrder = Math.max(0, ...effectiveRooms.map((room) => room.stats?.order || 0));

      return {
        ...inst,
        rooms: effectiveRooms,
        minX, minZ,
        gridW: maxX - minX + 1,
        gridH: maxZ - minZ + 1,
        isVexDungeon: inst.name.toLowerCase().includes('vex_the_lich'),
        maxRoomOrder
      };
    });

    const rows: { instances: Instance[], width: number, height: number }[] = [];
    let currentRow = { instances: [] as Instance[], width: 0, height: 0 };

    processedInstances.forEach((inst: any) => {
      const neededWidth = (currentRow.instances.length ? GAP_TILES : 0) + inst.gridW;
      if (currentRow.instances.length > 0 && currentRow.width + neededWidth > MAX_ROW_TILES) {
        rows.push(currentRow);
        currentRow = { instances: [], width: 0, height: 0 };
      }
      inst.renderX = currentRow.width + (currentRow.instances.length ? GAP_TILES : 0);
      currentRow.instances.push(inst);
      currentRow.width += neededWidth;
      currentRow.height = Math.max(currentRow.height, inst.gridH);
    });
    if (currentRow.instances.length) rows.push(currentRow);

    const totalGridH = rows.reduce((acc, r) => acc + r.height, 0) + (rows.length - 1) * GAP_TILES;
    const totalGridW = Math.max(...rows.map(r => r.width));
    
    let currentY = 0;
    const instancesWithCoords: Instance[] = [];
    
    rows.forEach(row => {
      row.instances.forEach(inst => {
        const rowOffsetX = (totalGridW - row.width) / 2;
        inst.renderX = (inst.renderX + rowOffsetX); 
        inst.renderY = currentY;
        instancesWithCoords.push(inst);
      });
      currentY += row.height + GAP_TILES;
    });

    return {
      instances: instancesWithCoords,
      totalW: totalGridW * TILE_SIZE,
      totalH: totalGridH * TILE_SIZE
    };

  }, [worldState.activeInstances, worldState.archivedInstances, getRoomSizeForRoom, selectedWorldId]);

  const selectedInstance = useMemo(() => {
    if (!selectedWorldId || !layoutData) return null;
    return layoutData.instances.find((inst) => inst.name === selectedWorldId) || null;
  }, [layoutData, selectedWorldId]);

  // 5. D3 Zoom Integration
  useEffect(() => {
    if (!svgRef.current || !layoutData) return;

    const zoom = d3.zoom<SVGSVGElement, unknown>()
      .scaleExtent([0.1, 4])
      .on('zoom', (e) => {
        setTransform(e.transform);
      });

    const svg = d3.select(svgRef.current);
    svg.call(zoom);

    if (dimensions.w && layoutData.totalW) {
      const isFocused = !!selectedWorldId;
      const margin = isFocused ? 50 : 100;
      
      const scale = Math.min(
        isFocused ? 1.5 : 1, 
        (dimensions.w - margin) / layoutData.totalW, 
        (dimensions.h - margin) / layoutData.totalH
      );
      
      const x = (dimensions.w - layoutData.totalW * scale) / 2;
      const y = (dimensions.h - layoutData.totalH * scale) / 2;
      
      svg.transition().duration(750)
         .call(zoom.transform, d3.zoomIdentity.translate(x, y).scale(scale));
    }
  }, [dimensions, layoutData, selectedWorldId]);

  // Format bytes to MB
  const toMB = (bytes: number) => (bytes / 1024 / 1024).toFixed(0);

  // 6. Mouse Interactions
  const handleRoomHover = (e: React.MouseEvent, inst: Instance, room: Room) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const containerRect = containerRef.current?.getBoundingClientRect();
    if(!containerRect) return;

    setHoveredData({
      x: rect.left - containerRect.left + rect.width / 2,
      y: rect.top - containerRect.top,
      content: (
        <div className="space-y-1 font-mono text-xs">
          <div className="font-bold text-amber-400">{room.prefab.split('/').pop()}</div>
          <div className="text-gray-400">{inst.name}</div>
          <div className="text-gray-500">[{room.x}, {room.z}]</div>
          {room.size && <div className="text-gray-600">{room.size.w}x{room.size.h}</div>}
          {room.stats?.order && <div className="text-indigo-300">Spawn #{room.stats.order}</div>}
          {(room.stats?.entities || room.stats?.kills) && (
            <div className="text-gray-500">Entities: {room.stats?.entities ?? 0} · Kills: {room.stats?.kills ?? 0}</div>
          )}
        </div>
      )
    });
  };

  return (
    <div className="flex w-full h-full bg-[#050608] font-sans overflow-hidden text-gray-200">
      
      {/* --- LEFT SIDEBAR: THE CODEX --- */}
      <div className="w-80 flex flex-col border-r border-white/10 bg-[#0b0d12]/95 backdrop-blur-sm z-20 shadow-2xl">
        <div className="p-4 border-b border-white/10 bg-black/20">
          <h2 className="text-sm font-bold text-indigo-400 flex items-center gap-2 tracking-widest uppercase mb-2">
            <Terminal size={16} />
            DungeonOS v2.4
          </h2>
          
          {/* System Vitals Display */}
          <div className="grid grid-cols-2 gap-2 text-[10px] font-mono text-gray-500">
            <div className="flex items-center gap-1.5">
               <Server size={12} className={serverStats ? "text-emerald-500" : "text-gray-700"} />
               <span>MEM: {serverStats ? `${toMB(serverStats.system.memory_total - serverStats.system.memory_free)}MB` : '--'}</span>
            </div>
            <div className="flex items-center gap-1.5">
               <Activity size={12} className={serverStats ? "text-indigo-500" : "text-gray-700"} />
               <span>THR: {serverStats?.system.threads_active ?? '--'}</span>
            </div>
             <div className="flex items-center gap-1.5">
               <Database size={12} className={serverStats ? "text-amber-500" : "text-gray-700"} />
               <span>EVS: {serverStats?.events.registered_types ?? '--'}</span>
            </div>
            <div className="flex items-center gap-1.5">
               <Users size={12} className={serverStats ? "text-rose-500" : "text-gray-700"} />
               <span>CON: {serverStats?.events.clients_connected ?? '--'}</span>
            </div>
          </div>
        </div>
        
        <div className="p-3">
          <div className="relative">
            <Search className="absolute left-3 top-2.5 text-gray-500" size={14} />
            <input 
              type="text" 
              placeholder="Search frequencies..." 
              className="w-full bg-black/40 border border-white/10 rounded-md py-2 pl-9 pr-3 text-xs focus:outline-none focus:border-indigo-500 transition-colors placeholder:text-gray-700 font-mono"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {selectedInstance && (
          <div className="mx-3 mb-3 rounded-md border border-white/10 bg-black/30 p-3 text-[10px] font-mono text-gray-400">
            <div className="text-gray-500 uppercase tracking-widest mb-2">Instance Pulse</div>
            <div className="grid grid-cols-2 gap-2">
              <div>Rooms: {selectedInstance.stats?.roomCount ?? selectedInstance.rooms.length}</div>
              <div>Entities: {selectedInstance.stats?.entityCount ?? 0}</div>
              <div>Kills: {selectedInstance.stats?.killCount ?? 0}</div>
              <div>Players: {selectedInstance.players?.size ?? 0}</div>
            </div>
          </div>
        )}

        <div className="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar">
          <button
            onClick={() => setSelectedWorldId(null)}
            className={`w-full text-left px-3 py-3 rounded-md border flex items-center gap-3 transition-all ${
              selectedWorldId === null 
                ? 'bg-indigo-500/10 border-indigo-500/50 text-indigo-200' 
                : 'border-transparent hover:bg-white/5 text-gray-400'
            }`}
          >
            <div className={`p-1.5 rounded-md ${selectedWorldId === null ? 'bg-indigo-500 text-white' : 'bg-gray-800'}`}>
              <Globe size={16} />
            </div>
            <div>
              <div className="text-xs font-bold">MULTIVERSE OVERVIEW</div>
              <div className="text-[10px] opacity-60 font-mono">ALL CHANNELS OPEN</div>
            </div>
          </button>

          <div className="h-px bg-white/5 my-2 mx-2" />
          
          <div className="px-2 pb-2 text-[10px] font-bold text-gray-600 uppercase tracking-wider font-mono">Detected Signals</div>

          {worldManifest
            .filter(w => w.name.toLowerCase().includes(searchTerm.toLowerCase()))
            .map(world => (
            <button
              key={world.name}
              onClick={() => setSelectedWorldId(world.name)}
              className={`w-full text-left px-3 py-2.5 rounded-md border flex items-center justify-between gap-3 transition-all group ${
                selectedWorldId === world.name 
                  ? 'bg-indigo-900/20 border-indigo-500/40' 
                  : 'border-transparent hover:bg-white/5'
              }`}
            >
              <div className="flex items-center gap-3 overflow-hidden">
                <div className={`p-1.5 rounded-md flex-shrink-0 ${
                  world.isVex 
                    ? 'bg-rose-900/50 text-rose-400 border border-rose-500/30' 
                    : world.type === 'dungeon' 
                      ? 'bg-emerald-900/30 text-emerald-400' 
                      : 'bg-gray-800 text-gray-500'
                }`}>
                  {world.isVex ? <AlertOctagon size={14} /> : world.type === 'dungeon' ? <Cpu size={14} /> : <Globe size={14} />}
                </div>
                <div className="min-w-0">
                  <div className={`text-xs font-medium truncate ${world.active ? 'text-gray-200' : 'text-gray-600'}`}>
                    {world.name}
                  </div>
                  <div className="flex items-center gap-2 text-[10px] font-mono">
                    <span className={world.active ? 'text-emerald-500' : 'text-gray-600'}>
                      {world.active ? 'ONLINE' : 'OFFLINE'}
                    </span>
                    {world.isVex && <span className="text-rose-500 font-bold animate-pulse">SIMULATION</span>}
                  </div>
                </div>
              </div>
              
              {world.players > 0 && (
                <div className="flex items-center gap-1 bg-black/40 px-1.5 py-0.5 rounded text-[10px] text-indigo-300 border border-white/5">
                  <Users size={10} />
                  {world.players}
                </div>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* --- MAIN DISPLAY --- */}
      <div className="flex-1 flex flex-col relative bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-gray-900 to-black">
        
        {/* CRT Scanline Overlay */}
        <div className="absolute inset-0 pointer-events-none z-30 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-[0.03]" />
        <div className="absolute inset-0 pointer-events-none z-30 bg-gradient-to-b from-transparent via-indigo-500/[0.03] to-transparent bg-[length:100%_4px]" />

        {/* Header HUD */}
        <div className="absolute top-0 left-0 right-0 p-4 z-10 flex justify-between items-start pointer-events-none">
          <div>
            <h1 className="text-2xl font-bold text-white tracking-tighter flex items-center gap-3">
              {selectedWorldId ? (
                <>
                  <Maximize2 size={24} className="text-indigo-400" />
                  <span className="text-indigo-100">{selectedWorldId}</span>
                </>
              ) : (
                <>
                  <Activity size={24} className="text-emerald-400" />
                  <span className="text-emerald-100">GLOBAL MONITORING</span>
                </>
              )}
            </h1>
            <p className="text-xs text-indigo-400/60 font-mono mt-1">
               {layoutData?.instances.length || 0} SECTORS // {Object.keys(worldState.players || {}).length} ENTITIES // MEMORY: {serverStats ? toMB(serverStats.system.memory_total - serverStats.system.memory_free) : '?'}MB
            </p>
          </div>
          
          <div className="flex gap-4 bg-black/60 backdrop-blur border border-white/10 p-2 rounded-lg">
             <LegendItem color="border-amber-400" label="Start Room" />
             <LegendItem color="border-indigo-500" label="Generated Room" />
             <LegendItem color="border-emerald-400" label="Progression" />
             <LegendItem color="border-slate-500" label="Cleared" />
             <LegendItem color="bg-rose-500" label="Entity" type="dot" />
          </div>
        </div>

        {/* The Map Canvas */}
        <div ref={containerRef} className="flex-1 w-full h-full cursor-move relative z-0">
          {!layoutData ? (
             <div className="absolute inset-0 flex items-center justify-center flex-col text-gray-600 gap-4">
               <Activity size={48} className="animate-pulse opacity-20" />
               <div className="font-mono text-sm tracking-widest">AWAITING TELEMETRY DATA...</div>
             </div>
          ) : (
            <svg ref={svgRef} width="100%" height="100%">
              <defs>
                 <pattern id="grid" width={TILE_SIZE} height={TILE_SIZE} patternUnits="userSpaceOnUse">
                   <path d={`M ${TILE_SIZE} 0 L 0 0 0 ${TILE_SIZE}`} fill="none" stroke="rgba(99, 102, 241, 0.08)" strokeWidth="1"/>
                 </pattern>
              </defs>
              <rect width="100%" height="100%" fill="url(#grid)" />
              
              <g transform={`translate(${transform.x},${transform.y}) scale(${transform.k})`}>
                {layoutData.instances.map(inst => {
                  return (
                    <InstanceGroup 
                      key={inst.name} 
                      inst={inst} 
                      getRoomSize={getRoomSizeForRoom}
                      getRoomStyle={(room: Room, instance: Instance, defaults: any) => getStyledRoom(room, instance, defaults)}
                      onRoomHover={handleRoomHover}
                      onRoomLeave={() => setHoveredData(null)}
                    />
                  );
                })}
                
                {Object.values(worldState.players || {}).map(player => {
                   const inst = layoutData.instances.find(i => i.name === player.world);
                   if (!inst) return null;
                   
                   const [rx, rz] = player.roomKey ? player.roomKey.split(',').map(Number) : [0,0];
                   const room = inst.rooms.find(r => r.x === rx && r.z === rz);
                   if (!room) return null;

                   const size = getRoomSizeForRoom(room);
                   const px = (inst.renderX + (room.x - inst.minX) + (size.w/2)) * TILE_SIZE;
                   const py = (inst.renderY + (room.z - inst.minZ) + (size.h/2)) * TILE_SIZE;

                   const playerId = player.uuid || player.playerId || player.id || player.name;
                   const playerStats = playerId ? inst.playerStats?.[playerId] : null;
                   const health = player.health ?? player.stats?.health;
                   const stamina = player.stamina ?? player.stats?.stamina;

                   return (
                     <PlayerDot 
                       key={player.uuid} 
                       x={px} 
                       y={py} 
                       player={player}
                       kills={playerStats?.kills ?? 0}
                       health={health}
                       stamina={stamina}
                       scale={1 / transform.k} 
                     />
                   );
                })}
              </g>
            </svg>
          )}

          {/* Floating Tooltip */}
          <AnimatePresence>
            {hoveredData && (
              <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                transition={{ duration: 0.1 }}
                className="absolute z-50 pointer-events-none origin-bottom-left"
                style={{ left: hoveredData.x + 12, top: hoveredData.y + 12 }}
              >
                <div className="bg-[#0b0d12]/90 border border-indigo-500/30 text-white p-3 rounded-lg shadow-[0_0_30px_rgba(0,0,0,0.5)] backdrop-blur min-w-[160px]">
                  {hoveredData.content}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}

// --- Sub Components ---

const InstanceGroup = React.memo(({ inst, getRoomSize, onRoomHover, onRoomLeave, getRoomStyle }: any) => {
  const instanceW = inst.gridW * TILE_SIZE;
  const instanceH = inst.gridH * TILE_SIZE;
  const pixelX = inst.renderX * TILE_SIZE;
  const pixelY = inst.renderY * TILE_SIZE;

  return (
    <g transform={`translate(${pixelX}, ${pixelY})`}>
      <g transform="translate(0, -12)">
        <text 
          fill={inst.isVexDungeon ? "#f43f5e" : (inst.active ? "#a5b4fc" : "#4b5563")} 
          fontSize={12} 
          fontWeight="bold"
          fontFamily="monospace"
          className="uppercase tracking-widest select-none"
          style={{ textShadow: inst.isVexDungeon ? '0 0 10px rgba(244,63,94,0.5)' : 'none' }}
        >
          {inst.name}
        </text>
        {inst.isVexDungeon && (
           <text x={0} y={12} fontSize={8} fill="#f43f5e" letterSpacing={2} opacity={0.7} className="animate-pulse">SIMULATION ACTIVE</text>
        )}
      </g>
      <rect 
        width={instanceW} 
        height={instanceH} 
        fill={inst.isVexDungeon ? "rgba(244, 63, 94, 0.03)" : "rgba(99, 102, 241, 0.01)"}
        stroke={inst.isVexDungeon ? "rgba(244, 63, 94, 0.2)" : (inst.active ? "rgba(99, 102, 241, 0.1)" : "transparent")} 
        strokeDasharray={inst.isVexDungeon ? "0" : "4 4"}
        rx={4}
      />
      {inst.rooms.map((room: Room) => {
        const size = getRoomSize(room);
        const rx = (room.x - inst.minX) * TILE_SIZE;
        const rz = (room.z - inst.minZ) * TILE_SIZE;
        const rw = size.w * TILE_SIZE;
        const rh = size.h * TILE_SIZE;
        const isStart = room.x === 0 && room.z === 0;
        const fillColor = inst.isVexDungeon ? "#3f1a26" : "#1e1b2e";
        const strokeColor = isStart ? "#fbbf24" : (inst.isVexDungeon ? "#f43f5e" : "#6366f1");
        const baseStyle = {
          fillColor,
          strokeColor,
          strokeOpacity: isStart ? 1 : (inst.isVexDungeon ? 0.6 : 0.4)
        };
        const styled = typeof getRoomStyle === 'function' ? getRoomStyle(room, inst, baseStyle) : baseStyle;

        return (
          <g key={`${room.x},${room.z}`} transform={`translate(${rx}, ${rz})`}>
            <rect
              width={rw - 2}
              height={rh - 2}
              fill={styled.fillColor}
              stroke={styled.strokeColor}
              strokeWidth={isStart ? 2 : 1}
              strokeOpacity={styled.strokeOpacity}
              rx={2}
              className="transition-all duration-200 hover:brightness-150"
              onMouseEnter={(e) => onRoomHover(e, inst, room)}
              onMouseLeave={onRoomLeave}
            />
          </g>
        );
      })}
    </g>
  );
});

const PlayerDot = ({ x, y, player, scale, kills, health, stamina }: { x: number, y: number, player: Player, scale: number, kills: number, health?: number, stamina?: number }) => {
  return (
    <motion.g
      initial={{ x, y }}
      animate={{ x, y }}
      transition={{ type: "spring", stiffness: 120, damping: 15 }}
    >
      <motion.circle 
        r={10 * Math.max(0.5, scale)} 
        fill="none" 
        stroke="#f43f5e" 
        strokeWidth={1}
        initial={{ opacity: 0.8, scale: 0.8 }}
        animate={{ opacity: 0, scale: 2 }}
        transition={{ repeat: Infinity, duration: 1.5, ease: "easeOut" }}
      />
      <circle r={4 * Math.max(0.6, scale)} fill="#f43f5e" className="drop-shadow-[0_0_8px_rgba(244,63,94,1)]" />
      <text 
        y={-12 * scale} 
        textAnchor="middle" 
        fill="white" 
        fontSize={10 * Math.max(0.8, scale)}
        fontWeight="bold"
        className="select-none pointer-events-none drop-shadow-md"
        style={{ textShadow: '0 1px 2px rgba(0,0,0,1)' }}
      >
        {player.name}
      </text>
      <text
        y={2 * scale}
        textAnchor="middle"
        fill="#94a3b8"
        fontSize={8 * Math.max(0.8, scale)}
        className="select-none pointer-events-none"
      >
        Kills: {kills}
      </text>
      <text
        y={14 * scale}
        textAnchor="middle"
        fill="#64748b"
        fontSize={7 * Math.max(0.8, scale)}
        className="select-none pointer-events-none"
      >
        HP: {health ?? '--'} · ST: {stamina ?? '--'}
      </text>
    </motion.g>
  );
};

const LegendItem = ({ color, label, type = 'box' }: any) => (
  <div className="flex items-center gap-2">
    {type === 'box' ? (
      <div className={`w-3 h-3 border-2 ${color} rounded-sm bg-opacity-20`} />
    ) : (
      <div className={`w-2 h-2 rounded-full ${color} shadow-[0_0_5px_currentColor]`} />
    )}
    <span className="text-[10px] font-bold uppercase text-indigo-200 tracking-wider">{label}</span>
  </div>
);
