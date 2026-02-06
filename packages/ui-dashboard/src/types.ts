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
