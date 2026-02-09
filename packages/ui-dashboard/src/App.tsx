import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom';
import Header from './components/Header';
import { VexGlobalStyles } from 'ui-shared/components';
import StatsView from './components/StatsView';
import MapView from './components/MapView';
import ArchivesView from './components/ArchivesView';
import TelemetryView from './components/TelemetryView';
import type { Metrics, ServerStats, TelemetryEvent } from './types';
import api, { apiBaseUrl } from './api';
import { events$, publishEvent, setStreamStatus, streamStatus$ } from './state/dashboardBus';
import {
  clearArchiveStorageDb,
  deleteArchiveStorageDb,
  initArchiveDb,
  loadArchivesFromDb,
  loadEventLogFromDb,
  saveArchiveToDb,
  saveEventLogToDb
} from './state/eventArchive';

const isPreviewMode = () => window.location.protocol === 'file:' || window.location.protocol === 'blob:';
const VEX_WORLD_FRAGMENT = 'vex_the_lich_dungeon';

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

function DashboardApp() {
  const [status, setStatus] = useState('connecting');
  const [events, setEvents] = useState<VexEvent[]>([]);
  const [selectedArchive, setSelectedArchive] = useState<any | null>(null);
  const [worldState, setWorldState] = useState<WorldState>({
    activeInstances: {},
    archivedInstances: [],
    players: {},
    portals: {}
  });
  const [metrics, setMetrics] = useState<Metrics>(defaultMetrics);
  const [serverStats, setServerStats] = useState<ServerStats | null>(null);
  const [worldMetadata, setWorldMetadata] = useState<Array<{ name: string; playerCount?: number }> | null>(null);
  const [prefabMetadata, setPrefabMetadata] = useState<Record<string, { w: number; h: number }>>({});

  const pendingPrefabFetches = useRef<Set<string>>(new Set());
  const replayTimerRef = useRef<number | null>(null);
  const archivedInstancesRef = useRef<any[]>([]);
  const prefabMetadataRef = useRef<Record<string, { w: number; h: number }>>({});
  const sseRef = useRef<EventSource | null>(null);
  const sseReconnectTimerRef = useRef<number | null>(null);
  const sseHealthTimerRef = useRef<number | null>(null);
  const sseAttemptRef = useRef(0);
  const sseLastEventAtRef = useRef<number>(0);
  const sawLiveEventRef = useRef(false);
  const recentEventFingerprintsRef = useRef<Map<string, number>>(new Map());
  const knownEventIdsRef = useRef<Set<string>>(new Set());

  const [replay, setReplay] = useState({
    active: false,
    playing: false,
    cursor: 0,
    events: [] as VexEvent[],
    source: null as null | 'archive' | 'telemetry'
  });

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
    void loadArchivesFromDb().then((list) => {
      const sorted = (list || []).sort((a: any, b: any) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
      setWorldState(prev => ({ ...prev, archivedInstances: sorted }));
    });
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
    void saveArchiveToDb({
      id: instanceData.name,
      timestamp: new Date().toISOString(),
      data: JSON.parse(JSON.stringify(serializable))
    }).then(loadArchives);
  }, [loadArchives]);

  const clearArchives = useCallback(() => {
    if (!isPreviewMode()) {
      api.delete('/archives')
        .then(() => {
          setSelectedArchive(null);
          setReplay({ active: false, playing: false, cursor: 0, events: [], source: null });
          loadArchives();
        })
        .catch(() => null);
      return;
    }
    void clearArchiveStorageDb().then(() => {
      setSelectedArchive(null);
      setReplay({ active: false, playing: false, cursor: 0, events: [], source: null });
      loadArchives();
    });
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
    void deleteArchiveStorageDb(archiveId).then(() => {
      setSelectedArchive((prev: any | null) => (prev?.id === archiveId ? null : prev));
      loadArchives();
    });
  }, [loadArchives]);

  const getEventFields = useCallback((ev: VexEvent) => ev.data?.fields || ev.data?.payload || ev.data || {}, []);
  const getEventWorld = useCallback((fields: any) => {
    if (!fields) return null;
    const world = fields.world || fields.worldMeta || fields.worldRef;
    return world?.name || world?.worldName || world?.DEFAULT || fields.worldName || fields.worldId || null;
  }, []);
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
    if (replay.active) return;
    const fields = getEventFields(event);
    const worldName = getEventWorld(fields);
    if (!worldName || !worldName.startsWith('instance-')) return;
    void saveEventLogToDb(worldName, event);
  }, [getEventFields, getEventWorld, replay.active]);

  const loadEventLog = useCallback((instanceName: string) => {
    if (!instanceName) return Promise.resolve(null);
    return loadEventLogFromDb(instanceName).catch(() => null);
  }, []);

  const checkHealth = useCallback(async () => {
    if (isPreviewMode()) {
      return true;
    }
    try {
      await api.get('/health');
      return true;
    } catch {
      return false;
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
    const instanceMetrics: Record<string, { entityCount: number; killCount: number; roomCount: number }> = {};
    const instanceRoomStats: Record<string, Record<string, { entities: number; kills: number; order?: number; generatedAt?: string }>> = {};
    const instancePlayerStats: Record<string, Record<string, { kills: number; points: number }>> = {};
    const instanceRoomOrder: Record<string, number> = {};
    let entities = 0;
    let rooms = 0;
    const types: Record<string, number> = {};
    const prefabs: Record<string, number> = {};

    const now = Date.now();
    const eventWindowStart = now - 60000;
    let recentEvents = 0;

    const sourceEvents = eventsOverride || events;
    const chronological = [...sourceEvents].reverse();

    const ensureInstanceState = (worldName: string) => {
      if (!activeInstances[worldName]) {
        activeInstances[worldName] = { name: worldName, rooms: {}, players: new Set(), active: true, status: 'active' };
      }
      if (!instanceMetrics[worldName]) {
        instanceMetrics[worldName] = { entityCount: 0, killCount: 0, roomCount: 0 };
      }
      if (!instanceRoomStats[worldName]) {
        instanceRoomStats[worldName] = {};
      }
      if (!instancePlayerStats[worldName]) {
        instancePlayerStats[worldName] = {};
      }
    };

    chronological.forEach(ev => {
      const fields = getEventFields(ev);
      const worldName = getEventWorld(fields);
      const playerRef = fields.playerRef || fields.player;
      const timestamp = getEventTimestamp(ev);
      const tsMs = Date.parse(timestamp);

      if (!Number.isNaN(tsMs) && tsMs >= eventWindowStart) {
        recentEvents += 1;
      }

      if (ev.type === 'InstanceInitializedEvent' && worldName) {
        ensureInstanceState(worldName);
        activeInstances[worldName].startedAt = timestamp;
      }

      if ((ev.type === 'InstanceTeardownStartedEvent' || ev.type === 'InstanceTeardownCompletedEvent') && worldName) {
        ensureInstanceState(worldName);
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
        ensureInstanceState(worldName);
        if (fields.prefabPath) ensurePrefabMetadata(fields.prefabPath);
        const size = extractRoomSizeFromFields(fields);
        const roomKey = `${fields.room.x},${fields.room.z}`;
        if (!activeInstances[worldName].rooms[roomKey]) {
          instanceMetrics[worldName].roomCount += 1;
          const order = (instanceRoomOrder[worldName] || 0) + 1;
          instanceRoomOrder[worldName] = order;
          const existingStats = instanceRoomStats[worldName][roomKey] || { entities: 0, kills: 0 };
          instanceRoomStats[worldName][roomKey] = {
            ...existingStats,
            order: existingStats.order ?? order,
            generatedAt: existingStats.generatedAt ?? timestamp
          };
        }
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
        ensureInstanceState(worldName);
        activeInstances[worldName].players.add(pid);
      }

      if (ev.type === 'RoomEnteredEvent' && playerRef && worldName && fields.room) {
        const pid = getPlayerIdFromRef(playerRef);
        if (!pid) return;
        if (!players[pid]) return;
        players[pid].roomKey = `${fields.room.x},${fields.room.z}`;
        players[pid].lastSeenAt = timestamp;
        ensureInstanceState(worldName);
        const roomKey = `${fields.room.x},${fields.room.z}`;
        const existingStats = instanceRoomStats[worldName][roomKey] || { entities: 0, kills: 0 };
        instanceRoomStats[worldName][roomKey] = existingStats;
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
        if (worldName) {
          ensureInstanceState(worldName);
          instanceMetrics[worldName].entityCount += 1;
          if (fields.room) {
            const roomKey = `${fields.room.x},${fields.room.z}`;
            const existingStats = instanceRoomStats[worldName][roomKey] || { entities: 0, kills: 0 };
            existingStats.entities += 1;
            instanceRoomStats[worldName][roomKey] = existingStats;
          }
        }
      }

      if (ev.type === 'EntityEliminatedEvent' && worldName) {
        ensureInstanceState(worldName);
        instanceMetrics[worldName].killCount += 1;
        if (fields.room) {
          const roomKey = `${fields.room.x},${fields.room.z}`;
          const existingStats = instanceRoomStats[worldName][roomKey] || { entities: 0, kills: 0 };
          existingStats.kills += 1;
          instanceRoomStats[worldName][roomKey] = existingStats;
        }
        const killerId = fields.killerId?.toString ? fields.killerId.toString() : fields.killerId;
        if (killerId) {
          const playerStats = instancePlayerStats[worldName][killerId] || { kills: 0, points: 0 };
          playerStats.kills += 1;
          playerStats.points += Number(fields.points || 0);
          instancePlayerStats[worldName][killerId] = playerStats;
        }
      }
    });

    Object.keys(instanceMetrics).forEach((worldName) => {
      if (!activeInstances[worldName]) return;
      activeInstances[worldName].stats = instanceMetrics[worldName];
      activeInstances[worldName].roomStats = instanceRoomStats[worldName] || {};
      activeInstances[worldName].playerStats = instancePlayerStats[worldName] || {};
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

  const ingestEvent = useCallback((payload: any, options?: { broadcast?: boolean }) => {
    const normalized: VexEvent = payload && payload.internalId ? payload : {
      internalId: Math.random().toString(36).substr(2, 7).toUpperCase(),
      timestamp: payload.timestamp || payload.data?.timestamp || new Date().toISOString(),
      type: payload.type || payload.data?.type || 'unknown.packet',
      data: payload.data || payload
    };

    const eventId = normalized.internalId || (normalized as any).id;
    if (eventId && knownEventIdsRef.current.has(eventId.toString())) {
      return;
    }
    if (eventId) {
      knownEventIdsRef.current.add(eventId.toString());
      if (knownEventIdsRef.current.size > 3000) {
        knownEventIdsRef.current.clear();
      }
    }

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
    if (options?.broadcast) {
      publishEvent(normalized);
    }
  }, [buildEventFingerprint, saveEventToLog]);

  const addEvent = useCallback((payload: any) => {
    ingestEvent(payload, { broadcast: true });
  }, [ingestEvent]);

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
      setStreamStatus(true);
      return () => undefined;
    }

    const clearReconnectTimer = () => {
      if (sseReconnectTimerRef.current != null) {
        window.clearTimeout(sseReconnectTimerRef.current);
        sseReconnectTimerRef.current = null;
      }
    };

    const clearHealthTimer = () => {
      if (sseHealthTimerRef.current != null) {
        window.clearInterval(sseHealthTimerRef.current);
        sseHealthTimerRef.current = null;
      }
    };

    const closeSource = () => {
      if (sseRef.current) {
        sseRef.current.close();
        sseRef.current = null;
      }
    };

    const scheduleReconnect = () => {
      clearReconnectTimer();
      const attempt = sseAttemptRef.current + 1;
      sseAttemptRef.current = attempt;
      const baseDelay = Math.min(30000, 1000 * Math.pow(2, Math.min(attempt, 5)));
      const jitter = Math.floor(Math.random() * 500);
      sseReconnectTimerRef.current = window.setTimeout(() => {
        connect();
      }, baseDelay + jitter);
    };

    const ensureHealth = async () => {
      const healthy = await checkHealth();
      if (healthy) {
        connect();
        return true;
      }
      return false;
    };

    const connect = () => {
      clearReconnectTimer();
      closeSource();
      sawLiveEventRef.current = false;
      const source = new EventSource(`${apiBaseUrl.replace(/\/$/, '')}/events`);
      sseRef.current = source;

      source.onopen = () => {
        sseAttemptRef.current = 0;
        sseLastEventAtRef.current = Date.now();
        setStreamStatus(true);
      };

      source.addEventListener('message', (e) => {
        sseLastEventAtRef.current = Date.now();
        sawLiveEventRef.current = true;
        addEvent(JSON.parse((e as MessageEvent).data));
      });

      source.addEventListener('prefab', (e) => {
        sseLastEventAtRef.current = Date.now();
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

      source.onerror = async () => {
        setStreamStatus(false);
        closeSource();
        const healthy = await ensureHealth();
        if (!healthy) {
          scheduleReconnect();
        }
      };

      window.setTimeout(() => {
        if (!sawLiveEventRef.current) {
          injectDemoData(true);
        }
      }, 1500);
    };

    const livenessTimer = window.setInterval(async () => {
      if (!sseRef.current) {
        return;
      }
      const last = sseLastEventAtRef.current;
      if (last > 0 && Date.now() - last > 15000) {
        setStreamStatus(false);
        closeSource();
        const healthy = await ensureHealth();
        if (!healthy) {
          scheduleReconnect();
        }
      }
    }, 5000);

    connect();
    clearHealthTimer();
    sseHealthTimerRef.current = window.setInterval(() => {
      if (sseRef.current) return;
      void ensureHealth();
    }, 5000);

    return () => {
      window.clearInterval(livenessTimer);
      clearHealthTimer();
      clearReconnectTimer();
      closeSource();
    };
  }, [addEvent, checkHealth, injectDemoData]);

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
    if (isPreviewMode()) {
      void initArchiveDb().then(() => loadArchives());
    } else {
      loadArchives();
    }
    const disconnect = connectSSE();
    pollPlayerMetadata();
    const playerTimer = window.setInterval(pollPlayerMetadata, 5000);

    return () => {
      window.clearInterval(playerTimer);
      if (typeof disconnect === 'function') {
        disconnect();
      }
      if (sseRef.current) {
        sseRef.current.close();
        sseRef.current = null;
      }
    };
  }, [connectSSE, loadArchives, pollPlayerMetadata]);

  useEffect(() => {
    let mounted = true;
    const loadStats = async () => {
      try {
        const res = await api.get('/stats');
        if (mounted) {
          setServerStats(res.data);
        }
      } catch {
        if (mounted) {
          setServerStats(null);
        }
      }
    };

    const loadWorlds = async () => {
      try {
        const res = await api.get('/metadata/worlds');
        if (mounted) {
          setWorldMetadata(res.data || []);
        }
      } catch {
        if (mounted) {
          setWorldMetadata(null);
        }
      }
    };

    loadStats();
    loadWorlds();
    const statsInterval = window.setInterval(loadStats, 5000);
    const worldsInterval = window.setInterval(loadWorlds, 10000);
    return () => {
      mounted = false;
      window.clearInterval(statsInterval);
      window.clearInterval(worldsInterval);
    };
  }, []);

  useEffect(() => {
    if (!replay.active) {
      recalculateState();
    }
  }, [events, replay.active, recalculateState]);

  useEffect(() => {
    const sub = streamStatus$.subscribe((statusUpdate) => {
      if (isPreviewMode()) {
        setStatus('demo mode');
        return;
      }
      setStatus(statusUpdate.connected ? 'online' : 'severed');
    });
    return () => sub.unsubscribe();
  }, []);

  useEffect(() => {
    const sub = events$.subscribe((busEvents) => {
      if (!Array.isArray(busEvents) || busEvents.length === 0) return;
      busEvents.forEach((ev) => {
        const normalized = (ev && (ev as any).internalId) ? ev : { data: (ev as any)?.data || ev };
        const fields = getEventFields(normalized as VexEvent);
        const worldName = getEventWorld(fields);
        if (!worldName) return;
        if (!worldName.toLowerCase().includes(VEX_WORLD_FRAGMENT)) return;
        ingestEvent(ev, { broadcast: false });
      });
    });
    return () => sub.unsubscribe();
  }, [getEventFields, getEventWorld, ingestEvent]);

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
    setReplay({ active: true, playing: false, cursor: 0, events: replayEvents, source: 'archive' });
    if (replayTimerRef.current) {
      window.clearInterval(replayTimerRef.current);
      replayTimerRef.current = null;
    }
    recalculateState(replayEvents.slice(0, 1), true);
  };

  const stopReplay = () => {
    setReplay({ active: false, playing: false, cursor: 0, events: [], source: null });
    if (replayTimerRef.current) {
      window.clearInterval(replayTimerRef.current);
      replayTimerRef.current = null;
    }
    recalculateState();
  };

  const applyReplayCursor = useCallback((nextCursor: number, sourceEvents?: VexEvent[]) => {
    const eventsToUse = sourceEvents || replay.events;
    if (!eventsToUse.length) return;
    if (!sourceEvents && !replay.active) return;
    const slice = eventsToUse.slice(0, nextCursor + 1);
    recalculateState(slice, true);
  }, [recalculateState, replay.active, replay.events]);

  const startReplayPlayback = useCallback(() => {
    if (replayTimerRef.current) {
      window.clearInterval(replayTimerRef.current);
      replayTimerRef.current = null;
    }
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
        applyReplayCursor(nextCursor, prev.events);
        return { ...prev, cursor: nextCursor };
      });
    }, 450);
  }, [applyReplayCursor]);

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
    startReplayPlayback();
  };

  const seekReplay = (value: string) => {
    const next = Number.parseInt(value, 10);
    const clamped = Number.isNaN(next) ? 0 : Math.min(Math.max(next, 0), replayRangeMax);
    setReplay(prev => ({ ...prev, cursor: clamped }));
    applyReplayCursor(clamped);
  };

  const replayLabel = replay.source === 'telemetry' ? 'Telemetry' : replay.source === 'archive' ? 'Archive' : undefined;

  const toReplayTimestamp = (ts?: number | string) => {
    if (!ts) return new Date().toISOString();
    if (typeof ts === 'number') return new Date(ts).toISOString();
    return ts;
  };

  const getTelemetryId = (ev: TelemetryEvent | VexEvent) => {
    const raw = (ev as any).internalId ?? (ev as any).id;
    if (raw == null) return '';
    return typeof raw === 'string' ? raw : String(raw);
  };

  const normalizeTelemetryEvent = (ev: TelemetryEvent): VexEvent => {
    const timestamp = toReplayTimestamp(ev.timestamp);
    const id = getTelemetryId(ev) || `${ev.type || ev.data?.type || 'event'}-${timestamp}`;
    return {
      internalId: id,
      timestamp,
      type: ev.type || ev.data?.type || 'unknown.packet',
      data: ev.payload ?? ev.data ?? ev
    };
  };

  const startTelemetryReplay = useCallback((sourceEvents: TelemetryEvent[], anchor: TelemetryEvent, autoplay: boolean) => {
    if (!sourceEvents.length) return;
    const normalized = sourceEvents.map(normalizeTelemetryEvent);
    const ordered = normalized
      .map((ev, index) => ({
        ev,
        index,
        ts: Date.parse(ev.timestamp || '')
      }))
      .sort((a, b) => {
        const aNaN = Number.isNaN(a.ts);
        const bNaN = Number.isNaN(b.ts);
        if (aNaN || bNaN) return a.index - b.index;
        if (a.ts === b.ts) return a.index - b.index;
        return a.ts - b.ts;
      })
      .map(({ ev }) => ev);

    const anchorId = getTelemetryId(anchor);
    let cursor = anchorId ? ordered.findIndex(ev => getTelemetryId(ev) === anchorId) : -1;
    if (cursor < 0 && anchor.timestamp) {
      const anchorStamp = toReplayTimestamp(anchor.timestamp);
      cursor = ordered.findIndex(ev => ev.type === anchor.type && ev.timestamp === anchorStamp);
    }
    if (cursor < 0) cursor = 0;

    if (replayTimerRef.current) {
      window.clearInterval(replayTimerRef.current);
      replayTimerRef.current = null;
    }
    setReplay({ active: true, playing: false, cursor, events: ordered, source: 'telemetry' });
    applyReplayCursor(cursor, ordered);

    if (autoplay) {
      setReplay(prev => ({ ...prev, playing: true }));
      startReplayPlayback();
    }
  }, [applyReplayCursor, startReplayPlayback]);

  const replayToEvent = useCallback((sourceEvents: TelemetryEvent[], anchor: TelemetryEvent) => {
    startTelemetryReplay(sourceEvents, anchor, false);
  }, [startTelemetryReplay]);

  const replayFromEvent = useCallback((sourceEvents: TelemetryEvent[], anchor: TelemetryEvent) => {
    startTelemetryReplay(sourceEvents, anchor, true);
  }, [startTelemetryReplay]);

  return (
    <div className="h-screen flex flex-col vex-grid">
      <VexGlobalStyles />
      <Header status={status} />
      <main className="flex-1 flex overflow-hidden">
        <Routes>
          <Route path="/" element={<Navigate to="/stats" replace />} />
          <Route
            path="/stats"
            element={
              <StatsView
                metrics={metrics}
                playerRoster={playerRoster}
                instanceList={instanceList}
                portalList={portalList}
                serverStats={serverStats}
                worldMetadata={worldMetadata}
              />
            }
          />
          <Route
            path="/map"
            element={<MapView worldState={worldState} getRoomSizeForRoom={getRoomSizeForRoom} />}
          />
          <Route
            path="/archives"
            element={
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
            }
          />
          <Route
            path="/logs"
            element={
              <TelemetryView
                replayActive={replay.active}
                replayLabel={replayLabel}
                onReplayToEvent={replayToEvent}
                onReplayFromEvent={replayFromEvent}
                onExitReplay={stopReplay}
              />
            }
          />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </div>
  );
}

function NotFound() {
  return (
    <div className="min-h-screen bg-[#0c0812] text-slate-100 flex items-center justify-center">
      <div className="text-center space-y-3">
        <p className="text-2xl font-semibold">Page not found</p>
        <Link
          to="/stats"
          className="inline-flex items-center justify-center px-4 py-2 rounded-lg border border-vex-purple/40 text-vex-purple hover:bg-vex-purple/10 transition"
        >
          Back to dashboard
        </Link>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <DashboardApp />
    </BrowserRouter>
  );
}
