import { createBroadcastEventBus } from "ui-shared/streams";
import type { StreamStatus } from "ui-shared/streams";

export type DashboardEvent = {
  id?: number | string;
  internalId?: string;
  timestamp?: number | string;
  type?: string;
  payload?: any;
  data?: any;
};

const {
  events$,
  eventStream$,
  streamStatus$,
  publishEvent,
  setStreamStatus,
  clearEventBuffer,
} = createBroadcastEventBus<DashboardEvent>({
  channelName: "vex-dashboard",
  maxEvents: 1500,
});

export type { StreamStatus };

export { eventStream$, events$, streamStatus$, publishEvent, setStreamStatus, clearEventBuffer };
