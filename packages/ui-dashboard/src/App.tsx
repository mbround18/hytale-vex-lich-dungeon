import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Header from './components/Header';
import { VexGlobalStyles } from 'ui-shared/components';
import StatsView from './components/StatsView';
import MapView from './components/MapView';
import ArchivesView from './components/ArchivesView';
import TelemetryView from './components/TelemetryView';
import type { Metrics } from './types';
import api, { apiBaseUrl } from './api';

const isPreviewMode = () => window.location.protocol === 'file:' || window.location.protocol === 'blob:';

type VexEvent = {
  internalId?: string;
  timestamp?: string;
  type?: string;
  data?: any;
};

type WorldState = {
  activeInstances: Record<string, any>;
  archivedInstances: any[];
  players: Record<string, any>;
  portals: Record<string, any>;
};

const defaultMetrics: Metrics = {
  totalEntities: 0,
  roomsGenerated: 0,
  entityTypes: {},
  prefabs: {},
  portalStats: { total: 0, active: 0, closed: 0, expired: 0, entered: 0 },
  playerStats: { total: 0, inInstances: 0, inHub: 0 },
  instanceStats: { active: 0, total: 0, recentEvents: 0 }
};

export default function App() {
  const [view, setView] = useState<'stats' | 'map' | 'archives' | 'logs'>('stats');
  const [status, setStatus] = useState('connecting');
  const [events, setEvents] = useState<VexEvent[]>([]);
  const [search, setSearch] = useState('');
  const [selectedEvent, setSelectedEvent] = useState<VexEvent | null>(null);
  const [selectedArchive, setSelectedArchive] = useState<any | null>(null);
  const [worldState, setWorldState] = useState<WorldState>({
    activeInstances: {},
    archivedInstances: [],
    players: {},
    portals: {}
  });
  const [metrics, setMetrics] = useState<Metrics>(defaultMetrics);
  const [prefabMetadata, setPrefabMetadata] = useState<Record<string, { w: number; h: number }>>({});

  const dbRef = useRef<IDBDatabase | null>(null);
  const pendingPrefabFetches = useRef<Set<string>>(new Set());
  const replayTimerRef = useRef<number | null>(null);
  const archivedInstancesRef = useRef<any[]>([]);
  const prefabMetadataRef = useRef<Record<string, { w: number; h: number }>>({});
  const sseRef = useRef<EventSource | null>(null);
  const sawLiveEventRef = useRef(false);
  const recentEventFingerprintsRef = useRef<Map<string, number>>(new Map());

  const [replay, setReplay] = useState({
    active: false,
    playing: false,
    cursor: 0,
    events: [] as VexEvent[]
  });

  const filteredEvents = useMemo(() => {
    if (!search) return events;
    const term = search.toLowerCase();
    return events.filter(e =>
      (e.type || '').toLowerCase().includes(term) || JSON.stringify(e.data || {}).toLowerCase().includes(term)
    );
  }, [events, search]);

  const playerRoster = useMemo(() => {
    return Object.values(worldState.players).sort((a: any, b: any) => (a.name || '').localeCompare(b.name || ''));
  }, [worldState.players]);

  const instanceList = useMemo(() => {
    return Object.values(worldState.activeInstances)
      .filter((i: any) => i.name !== 'default')
      .sort((a: any, b: any) => (a.name || '').localeCompare(b.name || ''));
  }, [worldState.activeInstances]);

  const portalList = useMemo(() => {
    return Object.values(worldState.portals)
      .filter((p: any) => (p.world || '').startsWith('instance-'))
      .sort((a: any, b: any) => {
        if (a.status === b.status) return (b.lastEnteredAt || '').localeCompare(a.lastEnteredAt || '');
        return a.status === 'active' ? -1 : 1;
      });
  }, [worldState.portals]);

  const replayRangeMax = useMemo(() => Math.max(replay.events.length - 1, 0), [replay.events.length]);
  const replayCurrentEvent = useMemo(() => replay.events[replay.cursor] || null, [replay.events, replay.cursor]);

  const formatTimestamp = (ts?: string) => {
    if (!ts) return '—';
    const date = new Date(ts);
    if (Number.isNaN(date.getTime())) return '—';
    return date.toLocaleTimeString();
  };

  const replayCurrentTime = formatTimestamp(replayCurrentEvent?.timestamp);
  const replayStartTime = formatTimestamp(replay.events[0]?.timestamp);
  const replayEndTime = formatTimestamp(replay.events[replay.events.length - 1]?.timestamp);

  useEffect(() => {
    archivedInstancesRef.current = worldState.archivedInstances;
  }, [worldState.archivedInstances]);

  useEffect(() => {
    prefabMetadataRef.current = prefabMetadata;
  }, [prefabMetadata]);

  const initDB = useCallback(() => {
    return new Promise<void>((resolve) => {
      const request = indexedDB.open('VexDungeonDB', 2);
      request.onupgradeneeded = (e) => {
        const db = (e.target as IDBOpenDBRequest).result;
        if (!db.objectStoreNames.contains('archives')) {
          db.createObjectStore('archives', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('eventLogs')) {
          db.createObjectStore('eventLogs', { keyPath: 'id' });
        }
      };
      request.onsuccess = (e) => {
        dbRef.current = (e.target as IDBOpenDBRequest).result;
        resolve();
      };
    });
  }, []);

  const loadArchives = useCallback(() => {
    if (!isPreviewMode()) {
      api.get('/archives')
        .then(res => res.data)
        .then((payload) => {
          const list = (payload?.archives || payload || []).sort((a: any, b: any) =>
            new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
          setWorldState(prev => ({ ...prev, archivedInstances: list }));
        })
        .catch(() => null);
      return;
    }
    const db = dbRef.current;
    if (!db) return;
    const tx = db.transaction('archives', 'readonly');
    const req = tx.objectStore('archives').getAll();
    req.onsuccess = () => {
      const list = (req.result || []).sort((a: any, b: any) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
      setWorldState(prev => ({ ...prev, archivedInstances: list }));
    };
  }, []);

  const saveArchive = useCallback((instanceData: any) => {
    const serializable = { ...instanceData, players: Array.from(instanceData.players || []) };
    if (!isPreviewMode()) {
      api.post('/archives', {
        id: instanceData.name,
        timestamp: new Date().toISOString(),
        data: JSON.parse(JSON.stringify(serializable))
      })
        .then(() => loadArchives())
        .catch(() => null);
      return;
    }
    const db = dbRef.current;
    if (!db) return;
    const tx = db.transaction('archives', 'readwrite');
    tx.objectStore('archives').put({
      id: instanceData.name,
      timestamp: new Date().toISOString(),
      data: JSON.parse(JSON.stringify(serializable))
    });
    loadArchives();
  }, [loadArchives]);

  const clearArchives = useCallback(() => {
    if (!isPreviewMode()) {
      api.delete('/archives')
        .then(() => {
          setSelectedArchive(null);
          setReplay({ active: false, playing: false, cursor: 0, events: [] });
          loadArchives();
        })
        .catch(() => null);
      return;
    }
    const db = dbRef.current;
    if (!db) return;
    const tx = db.transaction(['archives', 'eventLogs'], 'readwrite');
    tx.objectStore('archives').clear();
    tx.objectStore('eventLogs').clear();
    tx.oncomplete = () => {
      setSelectedArchive(null);
      setReplay({ active: false, playing: false, cursor: 0, events: [] });
      loadArchives();
    };
  }, [loadArchives]);

  const deleteArchive = useCallback((archiveId: string) => {
    if (!isPreviewMode()) {
      api.delete(`/archives/${encodeURIComponent(archiveId)}`)
        .then(() => {
          setSelectedArchive((prev: any | null) => (prev?.id === archiveId ? null : prev));
          loadArchives();
        })
        .catch(() => null);
      return;
    }
    const db = dbRef.current;
    if (!db) return;
    const tx = db.transaction(['archives', 'eventLogs'], 'readwrite');
    tx.objectStore('archives').delete(archiveId);
    tx.objectStore('eventLogs').delete(archiveId);
    tx.oncomplete = () => {
      setSelectedArchive((prev: any | null) => (prev?.id === archiveId ? null : prev));
      loadArchives();
    };
  }, [loadArchives]);

  const getEventFields = useCallback((ev: VexEvent) => ev.data?.fields || ev.data?.fields || {}, []);
  const getEventWorld = useCallback((fields: any) => fields.world?.name || fields.worldName || fields.world?.worldName || null, []);
  const getEventTimestamp = useCallback((ev: VexEvent) => ev.timestamp || ev.data?.timestamp || new Date().toISOString(), []);
  const getPlayerIdFromRef = useCallback((playerRef: any) => {
    if (!playerRef) return null;
    return playerRef.uuid || playerRef.playerId || playerRef.id || playerRef.name || playerRef.username || null;
  }, []);
  const getPlayerNameFromRef = useCallback((playerRef: any, fallbackId?: string | null) => {
    if (!playerRef) return fallbackId || 'Unknown';
    return playerRef.name || playerRef.username || fallbackId || 'Unknown';
  }, []);
  const buildEventFingerprint = useCallback((event: VexEvent) => {
    const fields = getEventFields(event);
    const worldName = getEventWorld(fields) || '';
    const playerId = getPlayerIdFromRef(fields.playerRef) || '';
    const room = fields.room ? `${fields.room.x},${fields.room.z}` : '';
    const portalId = fields.portalId || '';
    const prefab = fields.prefabPath || fields.prefab || '';
    const timestamp = getEventTimestamp(event);
    return [event.type || 'unknown', timestamp, worldName, playerId, room, portalId, prefab].join('|');
  }, [getEventFields, getEventTimestamp, getEventWorld, getPlayerIdFromRef]);

  const saveEventToLog = useCallback((event: VexEvent) => {
    const db = dbRef.current;
    if (!db || replay.active) return;
    const fields = getEventFields(event);
    const worldName = getEventWorld(fields);
    if (!worldName || !worldName.startsWith('instance-')) return;
    const tx = db.transaction('eventLogs', 'readwrite');
    const store = tx.objectStore('eventLogs');
    const req = store.get(worldName);
    req.onsuccess = () => {
      const existing = req.result || { id: worldName, events: [] };
      existing.events.push(event);
      store.put(existing);
    };
  }, [getEventFields, getEventWorld, replay.active]);

  const loadEventLog = useCallback((instanceName: string) => {
    const db = dbRef.current;
    if (!db || !instanceName) return Promise.resolve(null);
    return new Promise<VexEvent[] | null>((resolve) => {
      const tx = db.transaction('eventLogs', 'readonly');
      const req = tx.objectStore('eventLogs').get(instanceName);
      req.onsuccess = () => resolve(req.result?.events || null);
      req.onerror = () => resolve(null);
    });
  }, []);

  const checkHealth = useCallback(async () => {
    if (isPreviewMode()) {
      setStatus('demo mode');
      return;
    }
    try {
      await api.get('/health');
      setStatus('online');
    } catch {
      setStatus('severed');
    }
  }, []);

  const extractRoomSizeFromFields = (fields: any) => {
    const direct = fields?.roomSize || fields?.size || fields?.dimensions;
    if (!direct) return null;
    const w = direct.width ?? direct.w ?? direct.x;
    const h = direct.height ?? direct.h ?? direct.z;
    if (Number.isFinite(w) && Number.isFinite(h)) return { w: Math.max(1, w), h: Math.max(1, h) };
    return null;
  };

  const getRoomSizeForRoom = (room: any) => {
    if (room?.size?.w && room?.size?.h) return room.size;
    const cached = room?.prefab ? prefabMetadata[room.prefab] : null;
    if (cached?.w && cached?.h) return { w: cached.w, h: cached.h };
    return { w: 1, h: 1 };
  };

  const ensurePrefabMetadata = useCallback((prefabPath?: string) => {
    if (!prefabPath) return;
    if (prefabMetadataRef.current[prefabPath] || pendingPrefabFetches.current.has(prefabPath)) return;
    pendingPrefabFetches.current.add(prefabPath);
    api.get(`/metadata/prefab/${encodeURIComponent(prefabPath)}`)
      .then(res => res.data)
      .then((data) => {
        if (!data) return;
        const size = data.roomSize || data.size || data.dimensions || data.tiles || data;
        const w = size?.width ?? size?.w ?? size?.x;
        const h = size?.height ?? size?.h ?? size?.z;
        if (Number.isFinite(w) && Number.isFinite(h)) {
          setPrefabMetadata(prev => ({ ...prev, [prefabPath]: { w: Math.max(1, w), h: Math.max(1, h) } }));
        }
      })
      .catch(() => null)
      .finally(() => pendingPrefabFetches.current.delete(prefabPath));
  }, []);

  const recalculateState = useCallback((eventsOverride?: VexEvent[], skipArchiveSave?: boolean) => {
    const activeInstances: Record<string, any> = {
      default: { name: 'default', rooms: {}, players: new Set(), active: true, status: 'hub' }
    };
    const players: Record<string, any> = {};
    const portals: Record<string, any> = {};
    let entities = 0;
    let rooms = 0;
    const types: Record<string, number> = {};
    const prefabs: Record<string, number> = {};

    const now = Date.now();
    const eventWindowStart = now - 60000;
    let recentEvents = 0;

    const sourceEvents = eventsOverride || events;
    const chronological = [...sourceEvents].reverse();

    chronological.forEach(ev => {
      const fields = getEventFields(ev);
      const worldName = getEventWorld(fields);
      const playerRef = fields.playerRef;
      const timestamp = getEventTimestamp(ev);
      const tsMs = Date.parse(timestamp);

      if (!Number.isNaN(tsMs) && tsMs >= eventWindowStart) {
        recentEvents += 1;
      }

      if (ev.type === 'InstanceInitializedEvent' && worldName) {
        if (!activeInstances[worldName]) {
          activeInstances[worldName] = {
            name: worldName,
            rooms: {},
            players: new Set(),
            active: true,
            status: 'active',
            startedAt: timestamp
          };
        }
      }

      if ((ev.type === 'InstanceTeardownStartedEvent' || ev.type === 'InstanceTeardownCompletedEvent') && worldName) {
        if (!activeInstances[worldName]) {
          activeInstances[worldName] = { name: worldName, rooms: {}, players: new Set(), active: true, status: 'active' };
        }
        if (ev.type === 'InstanceTeardownStartedEvent') {
          activeInstances[worldName].status = 'teardown';
          activeInstances[worldName].teardownStartedAt = timestamp;
        }
        if (ev.type === 'InstanceTeardownCompletedEvent') {
          activeInstances[worldName].status = 'closed';
          activeInstances[worldName].teardownCompletedAt = timestamp;
          activeInstances[worldName].active = false;
        }
      }

      if (ev.type === 'RoomGeneratedEvent' && worldName && fields.room) {
        rooms += 1;
        if (!activeInstances[worldName]) {
          activeInstances[worldName] = { name: worldName, rooms: {}, players: new Set(), active: true, status: 'active' };
        }
        if (fields.prefabPath) ensurePrefabMetadata(fields.prefabPath);
        const size = extractRoomSizeFromFields(fields);
        const roomKey = `${fields.room.x},${fields.room.z}`;
        activeInstances[worldName].rooms[roomKey] = {
          x: fields.room.x,
          z: fields.room.z,
          prefab: fields.prefabPath,
          type: 'room',
          size
        };
        prefabs[fields.prefabPath] = (prefabs[fields.prefabPath] || 0) + 1;
      }

      if ((ev.type === 'WorldEnteredEvent' || ev.type === 'PlayerJoinedServerEvent' || ev.type === 'InstanceEnteredEvent') && playerRef && worldName) {
        const pid = getPlayerIdFromRef(playerRef);
        if (!pid) return;
        if (players[pid] && players[pid].world && activeInstances[players[pid].world]) {
          activeInstances[players[pid].world].players.delete(pid);
        }
        players[pid] = {
          ...players[pid],
          name: getPlayerNameFromRef(playerRef, pid),
          uuid: playerRef.uuid || players[pid]?.uuid,
          world: worldName,
          roomKey: players[pid]?.roomKey || '0,0',
          lastSeenAt: timestamp
        };
        if (!activeInstances[worldName]) {
          activeInstances[worldName] = { name: worldName, rooms: {}, players: new Set(), active: true, status: 'active' };
        }
        activeInstances[worldName].players.add(pid);
      }

      if (ev.type === 'RoomEnteredEvent' && playerRef && worldName && fields.room) {
        const pid = getPlayerIdFromRef(playerRef);
        if (!pid) return;
        if (!players[pid]) return;
        players[pid].roomKey = `${fields.room.x},${fields.room.z}`;
        players[pid].lastSeenAt = timestamp;
      }

      if (ev.type === 'PortalCreatedEvent') {
        const portalId = fields.portalId;
        if (portalId) {
          portals[portalId] = {
            id: portalId,
            world: worldName || 'unknown',
            createdAt: timestamp,
            expiresAt: fields.expiresAt,
            placement: fields.placement,
            enteredBy: [],
            enterCount: 0,
            status: 'active'
          };
        }
      }

      if (ev.type === 'PortalEnteredEvent') {
        const portalId = fields.portalId;
        if (portalId) {
          if (!portals[portalId]) {
            portals[portalId] = {
              id: portalId,
              world: worldName || 'unknown',
              createdAt: null,
              expiresAt: null,
              placement: fields.placement,
              enteredBy: [],
              enterCount: 0,
              status: 'active'
            };
          }
          portals[portalId].enterCount += 1;
          if (playerRef?.name) portals[portalId].enteredBy.push(playerRef.name);
          portals[portalId].lastEnteredAt = timestamp;
          const pid = getPlayerIdFromRef(playerRef);
          if (pid && players[pid]) {
            players[pid].lastPortalId = portalId;
            if (players[pid].world?.startsWith('instance-')) {
              portals[portalId].world = players[pid].world;
            }
          }
        }
      }

      if (ev.type === 'PortalClosedEvent') {
        const portalId = fields.portalId;
        if (portalId) {
          if (!portals[portalId]) {
            portals[portalId] = { id: portalId, status: 'closed', enteredBy: [], enterCount: 0 };
          }
          portals[portalId].status = 'closed';
          portals[portalId].closedAt = timestamp;
        }
      }

      if (ev.type === 'EntitySpawnedEvent' || ev.type === 'PrefabEntitySpawnedEvent') {
        entities += 1;
        const type = fields.entityType || fields.modelId || 'Unknown';
        types[type] = (types[type] || 0) + 1;
      }
    });

    Object.values(portals).forEach((p: any) => {
      if (p.status !== 'closed' && p.expiresAt && p.expiresAt < now) {
        p.status = 'expired';
      }
    });

    const activeInstancesCount = Object.values(activeInstances).filter((i: any) => i.active && i.name !== 'default').length;
    const totalInstancesCount = Object.values(activeInstances).filter((i: any) => i.name !== 'default').length;
    const activePortals = Object.values(portals).filter((p: any) => p.status === 'active').length;
    const closedPortals = Object.values(portals).filter((p: any) => p.status === 'closed').length;
    const expiredPortals = Object.values(portals).filter((p: any) => p.status === 'expired').length;
    const totalPortalEntries = Object.values(portals).reduce((sum: number, p: any) => sum + (p.enterCount || 0), 0);
    const playersInInstances = Object.values(players).filter((p: any) => p.world && p.world.startsWith('instance-')).length;

    setWorldState(prev => ({
      ...prev,
      activeInstances,
      players,
      portals
    }));

    setMetrics({
      totalEntities: entities,
      roomsGenerated: rooms,
      entityTypes: types,
      prefabs,
      portalStats: {
        total: Object.keys(portals).length,
        active: activePortals,
        closed: closedPortals,
        expired: expiredPortals,
        entered: totalPortalEntries
      },
      playerStats: {
        total: Object.keys(players).length,
        inInstances: playersInInstances,
        inHub: Object.keys(players).length - playersInInstances
      },
      instanceStats: {
        active: activeInstancesCount,
        total: totalInstancesCount,
        recentEvents
      }
    });

    if (!skipArchiveSave) {
      Object.values(activeInstances).forEach((inst: any) => {
        if (!inst.active && !archivedInstancesRef.current.find(a => a.id === inst.name)) {
          saveArchive(inst);
        }
      });
    }
  }, [events, ensurePrefabMetadata, getEventFields, getEventTimestamp, getEventWorld, getPlayerIdFromRef, getPlayerNameFromRef, saveArchive]);

  const addEvent = useCallback((payload: any) => {
    const normalized: VexEvent = payload && payload.internalId ? payload : {
      internalId: Math.random().toString(36).substr(2, 7).toUpperCase(),
      timestamp: payload.timestamp || payload.data?.timestamp || new Date().toISOString(),
      type: payload.type || payload.data?.type || 'unknown.packet',
      data: payload.data || payload
    };

    const fingerprint = buildEventFingerprint(normalized);
    const now = Date.now();
    const lastSeen = recentEventFingerprintsRef.current.get(fingerprint);
    if (lastSeen && now - lastSeen < 1200) {
      return;
    }
    recentEventFingerprintsRef.current.set(fingerprint, now);
    if (recentEventFingerprintsRef.current.size > 1000) {
      const entries = Array.from(recentEventFingerprintsRef.current.entries())
        .sort((a, b) => a[1] - b[1])
        .slice(0, 300);
      entries.forEach(([key]) => recentEventFingerprintsRef.current.delete(key));
    }

    setEvents(prev => {
      const next = [normalized, ...prev];
      return next.length > 1200 ? next.slice(0, 1200) : next;
    });
    saveEventToLog(normalized);
  }, [buildEventFingerprint, saveEventToLog]);

  const tryLoadJsonFiles = useCallback(async (files: string[]) => {
    if (isPreviewMode()) return false;
    try {
      const responses = await Promise.all(
        files.map(f => fetch(f).then(res => (res.ok ? res.json() : null)).catch(() => null))
      );
      const payloads = responses.filter(Boolean).flat();
      if (!payloads.length) return false;
      payloads.forEach(p => addEvent(p));
      return true;
    } catch {
      return false;
    }
  }, [addEvent]);

  const injectDemoData = useCallback(async (softLoad = false) => {
    const sources = [
      'vex_session_1770320858404.json',
      'vex_telemetry_2026-02-05T19-35-27.json',
      'vex_telemetry_2026-02-06T01-52-40-324Z.json',
      'vex_telemetry_2026-02-06T01-54-57-383Z.json',
      'vex_telemetry_2026-02-06T06-11-23-935Z.json'
    ];

    const loaded = await tryLoadJsonFiles(sources);
    if (loaded || softLoad) return;

    const demoData = [
      { type: 'WorldEnteredEvent', data: { fields: { playerRef: { name: 'MBRound18' }, world: { name: 'default' } } } },
      { type: 'InstanceInitializedEvent', data: { fields: { world: { name: 'instance-Lava-A' } } } },
      { type: 'PortalCreatedEvent', data: { fields: { world: { name: 'default' }, portalId: 'portal-001', expiresAt: Date.now() + 60000 } } },
      { type: 'RoomGeneratedEvent', data: { fields: { prefabPath: 'Start', world: { name: 'instance-Lava-A' }, room: { x: 0, z: 0 } } } },
      { type: 'RoomGeneratedEvent', data: { fields: { prefabPath: 'Hallway', world: { name: 'instance-Lava-A' }, room: { x: 0, z: 1 } } } },
      { type: 'PortalEnteredEvent', data: { fields: { playerRef: { name: 'MBRound18' }, world: { name: 'instance-Lava-A' }, portalId: 'portal-001' } } },
      { type: 'RoomEnteredEvent', data: { fields: { playerRef: { name: 'MBRound18' }, world: { name: 'instance-Lava-A' }, room: { x: 0, z: 0 } } } }
    ];
    setTimeout(() => demoData.forEach(d => addEvent(d)), 500);
  }, [addEvent, tryLoadJsonFiles]);

  const connectSSE = useCallback(() => {
    if (isPreviewMode()) {
      injectDemoData();
      return;
    }
    try {
      if (sseRef.current) {
        sseRef.current.close();
        sseRef.current = null;
      }
      sawLiveEventRef.current = false;
      const source = new EventSource(`${apiBaseUrl.replace(/\/$/, '')}/events`);
      sseRef.current = source;
      source.addEventListener('message', (e) => {
        sawLiveEventRef.current = true;
        addEvent(JSON.parse((e as MessageEvent).data));
      });
      source.addEventListener('prefab', (e) => {
        sawLiveEventRef.current = true;
        try {
          const data = JSON.parse((e as MessageEvent).data);
          if (data?.prefabPath && data?.roomSize) {
            const w = data.roomSize?.w ?? data.roomSize?.width;
            const h = data.roomSize?.h ?? data.roomSize?.height;
            if (Number.isFinite(w) && Number.isFinite(h)) {
              setPrefabMetadata(prev => ({ ...prev, [data.prefabPath]: { w: Math.max(1, w), h: Math.max(1, h) } }));
            }
          }
        } catch {
          // ignore malformed prefab payloads
        }
      });
      source.onerror = () => console.warn('SSE disconnected');
      window.setTimeout(() => {
        if (!sawLiveEventRef.current) {
          injectDemoData(true);
        }
      }, 1500);
    } catch {
      injectDemoData();
    }
  }, [addEvent, injectDemoData]);

  const pollPlayerMetadata = useCallback(async () => {
    if (isPreviewMode()) return;
    try {
      const res = await api.get('/metadata/players');
      const payload = res.data;
      const players = payload?.players || [];
      if (!players.length) return;

      setWorldState(prev => {
        const nextPlayers = { ...prev.players };
        players.forEach((p: any) => {
          const pid = p.uuid || p.playerId || p.name;
          if (!pid) return;
          const existing = nextPlayers[pid] || {};
          nextPlayers[pid] = {
            ...existing,
            name: p.name || existing.name || pid,
            uuid: p.uuid || p.playerId || existing.uuid,
            world: p.world || existing.world,
            position: p.position || existing.position,
            lastSeenAt: payload.timestamp || new Date().toISOString()
          };
        });
        return { ...prev, players: nextPlayers };
      });
    } catch {
      // ignore polling errors
    }
  }, []);

  useEffect(() => {
    void initDB().then(() => loadArchives());
    connectSSE();
    checkHealth();
    const healthTimer = window.setInterval(checkHealth, 10000);
    pollPlayerMetadata();
    const playerTimer = window.setInterval(pollPlayerMetadata, 5000);

    return () => {
      window.clearInterval(healthTimer);
      window.clearInterval(playerTimer);
      if (sseRef.current) {
        sseRef.current.close();
        sseRef.current = null;
      }
    };
  }, [checkHealth, connectSSE, initDB, loadArchives, pollPlayerMetadata]);

  useEffect(() => {
    if (!replay.active) {
      recalculateState();
    }
  }, [events, replay.active, recalculateState]);

  const downloadJson = (filename: string, payload: any) => {
    try {
      const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download telemetry', err);
    }
  };

  const downloadFullTelemetry = () => {
    const stamp = new Date().toISOString().replace(/[:.]/g, '-');
    downloadJson(`vex_telemetry_${stamp}.json`, events);
  };

  const downloadInstanceLog = async (instanceName: string) => {
    if (!instanceName) return;
    const stored = await loadEventLog(instanceName);
    const filtered = stored && stored.length ? stored : events.filter(ev => {
      const fields = getEventFields(ev);
      const worldName = getEventWorld(fields);
      return worldName === instanceName;
    });
    const stamp = new Date().toISOString().replace(/[:.]/g, '-');
    const safeName = instanceName.replace(/[^a-zA-Z0-9_-]+/g, '_');
    downloadJson(`vex_instance_${safeName}_${stamp}.json`, filtered);
  };

  const buildReplayEvents = (instanceName: string) => {
    const filtered = events.filter(ev => {
      const fields = getEventFields(ev);
      const worldName = getEventWorld(fields);
      return worldName === instanceName;
    });
    return filtered.sort((a, b) => {
      const at = Date.parse(a.timestamp || a.data?.timestamp || '');
      const bt = Date.parse(b.timestamp || b.data?.timestamp || '');
      if (Number.isNaN(at) || Number.isNaN(bt)) return 0;
      return at - bt;
    });
  };

  const startReplay = async (instanceName: string) => {
    const stored = await loadEventLog(instanceName);
    const replayEvents = stored && stored.length ? stored : buildReplayEvents(instanceName);
    setReplay({ active: true, playing: false, cursor: 0, events: replayEvents });
    if (replayTimerRef.current) {
      window.clearInterval(replayTimerRef.current);
      replayTimerRef.current = null;
    }
    recalculateState(replayEvents.slice(0, 1), true);
  };

  const stopReplay = () => {
    setReplay({ active: false, playing: false, cursor: 0, events: [] });
    if (replayTimerRef.current) {
      window.clearInterval(replayTimerRef.current);
      replayTimerRef.current = null;
    }
    recalculateState();
  };

  const applyReplayCursor = (nextCursor: number) => {
    if (!replay.active || replay.events.length === 0) return;
    const slice = replay.events.slice(0, nextCursor + 1);
    recalculateState(slice, true);
  };

  const toggleReplay = () => {
    if (!replay.active || replay.events.length === 0) return;
    if (replay.playing) {
      setReplay(prev => ({ ...prev, playing: false }));
      if (replayTimerRef.current) {
        window.clearInterval(replayTimerRef.current);
        replayTimerRef.current = null;
      }
      return;
    }

    setReplay(prev => ({ ...prev, playing: true }));
    replayTimerRef.current = window.setInterval(() => {
      setReplay(prev => {
        if (!prev.playing) return prev;
        if (prev.cursor >= prev.events.length - 1) {
          if (replayTimerRef.current) {
            window.clearInterval(replayTimerRef.current);
            replayTimerRef.current = null;
          }
          return { ...prev, playing: false };
        }
        const nextCursor = prev.cursor + 1;
        applyReplayCursor(nextCursor);
        return { ...prev, cursor: nextCursor };
      });
    }, 450);
  };

  const seekReplay = (value: string) => {
    const next = Number.parseInt(value, 10);
    const clamped = Number.isNaN(next) ? 0 : Math.min(Math.max(next, 0), replayRangeMax);
    setReplay(prev => ({ ...prev, cursor: clamped }));
    applyReplayCursor(clamped);
  };

  return (
    <div className="h-screen flex flex-col vex-grid">
      <VexGlobalStyles />
      <Header status={status} view={view} onViewChange={setView} />
      <main className="flex-1 flex overflow-hidden">
        {view === 'stats' && (
          <StatsView
            metrics={metrics}
            playerRoster={playerRoster}
            instanceList={instanceList}
            portalList={portalList}
          />
        )}

        {view === 'map' && (
          <MapView worldState={worldState} getRoomSizeForRoom={getRoomSizeForRoom} />
        )}

        {view === 'archives' && (
          <ArchivesView
            archives={worldState.archivedInstances}
            selectedArchive={selectedArchive}
            onSelectArchive={setSelectedArchive}
            onClearArchives={clearArchives}
            onDeleteArchive={deleteArchive}
            onDownloadInstanceLog={downloadInstanceLog}
            replay={replay}
            replayRangeMax={replayRangeMax}
            replayStartTime={replayStartTime}
            replayCurrentTime={replayCurrentTime}
            replayEndTime={replayEndTime}
            onStartReplay={startReplay}
            onToggleReplay={toggleReplay}
            onStopReplay={stopReplay}
            onSeekReplay={seekReplay}
          />
        )}

        {view === 'logs' && (
          <TelemetryView
            search={search}
            onSearchChange={setSearch}
            events={filteredEvents}
            selectedEvent={selectedEvent}
            onSelectEvent={setSelectedEvent}
            onPurge={() => {
              setEvents([]);
              setSelectedEvent(null);
              recalculateState();
            }}
            onDownload={downloadFullTelemetry}
          />
        )}
      </main>
    </div>
  );
}
