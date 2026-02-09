export type Metrics = {
  totalEntities: number;
  roomsGenerated: number;
  entityTypes: Record<string, number>;
  prefabs: Record<string, number>;
  portalStats: {
    total: number;
    active: number;
    closed: number;
    expired: number;
    entered: number;
  };
  playerStats: {
    total: number;
    inInstances: number;
    inHub: number;
  };
  instanceStats: {
    active: number;
    total: number;
    recentEvents: number;
  };
};

export type ServerWorldStat = {
  name: string;
  loaded_chunks?: number;
  players: number;
};

export type ServerStats = {
  system: {
    uptime_ms: number;
    memory_free: number;
    memory_max: number;
    memory_total: number;
    threads_active: number;
  };
  events: {
    buffer_size: number;
    clients_connected: number;
    registered_types: number;
  };
  worlds: ServerWorldStat[];
};

export type TelemetryEvent = {
  id?: number | string;
  internalId?: string;
  timestamp?: number | string;
  type?: string;
  payload?: any;
  data?: any;
};
