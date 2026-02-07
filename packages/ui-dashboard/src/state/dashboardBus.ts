import { BehaviorSubject } from 'rxjs';

export type DashboardEvent = {
  id?: number | string;
  internalId?: string;
  timestamp?: number | string;
  type?: string;
  payload?: any;
  data?: any;
};

export type StreamStatus = {
  connected: boolean;
  lastEventAt?: number;
};

const MAX_EVENTS = 1500;
const sourceId =
  typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `tab-${Math.random().toString(36).slice(2)}`;

const channel = typeof BroadcastChannel !== 'undefined'
  ? new BroadcastChannel('vex-dashboard')
  : null;

const events$ = new BehaviorSubject<DashboardEvent[]>([]);
const streamStatus$ = new BehaviorSubject<StreamStatus>({ connected: false });

const emitEvents = (next: DashboardEvent[], broadcast: boolean) => {
  events$.next(next);
  if (broadcast && channel) {
    channel.postMessage({ type: 'events', sourceId, payload: next.slice(0, 200) });
  }
};

const emitStatus = (next: StreamStatus, broadcast: boolean) => {
  streamStatus$.next(next);
  if (broadcast && channel) {
    channel.postMessage({ type: 'status', sourceId, payload: next });
  }
};

if (channel) {
  channel.addEventListener('message', (event) => {
    const msg = event.data;
    if (!msg || msg.sourceId === sourceId) return;
    if (msg.type === 'events' && Array.isArray(msg.payload)) {
      const incoming = msg.payload as DashboardEvent[];
      const current = events$.getValue();
      const merged = [...incoming, ...current];
      emitEvents(merged.slice(0, MAX_EVENTS), false);
    }
    if (msg.type === 'event' && msg.payload) {
      const current = events$.getValue();
      const merged = [msg.payload as DashboardEvent, ...current];
      emitEvents(merged.slice(0, MAX_EVENTS), false);
    }
    if (msg.type === 'status' && msg.payload) {
      emitStatus(msg.payload as StreamStatus, false);
    }
    if (msg.type === 'clear') {
      emitEvents([], false);
    }
  });
}

export const publishEvent = (event: DashboardEvent) => {
  const current = events$.getValue();
  const merged = [event, ...current];
  emitEvents(merged.slice(0, MAX_EVENTS), false);
  emitStatus({ connected: true, lastEventAt: Date.now() }, true);
  if (channel) {
    channel.postMessage({ type: 'event', sourceId, payload: event });
  }
};

export const setStreamStatus = (connected: boolean) => {
  emitStatus({ connected, lastEventAt: streamStatus$.getValue().lastEventAt }, true);
};

export const clearEventBuffer = () => {
  emitEvents([], true);
};

export { events$, streamStatus$ };
