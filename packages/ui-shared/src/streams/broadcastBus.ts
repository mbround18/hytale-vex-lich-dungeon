import {
  BehaviorSubject,
  Subject,
  concatMap,
  from,
  shareReplay,
  share,
  startWith,
  scan,
  EMPTY,
  type Observable,
} from "rxjs";
import type { StreamStatus } from "./streamFactory";

type EventAction<T> =
  | { type: "event"; event: T }
  | { type: "batch"; events: T[] }
  | { type: "clear" };

export type BroadcastBus<T> = {
  events$: Observable<T[]>;
  eventStream$: Observable<T>;
  streamStatus$: BehaviorSubject<StreamStatus>;
  publishEvent: (event: T) => void;
  setStreamStatus: (connected: boolean) => void;
  clearEventBuffer: () => void;
};

export type BroadcastBusOptions = {
  channelName: string;
  maxEvents?: number;
};

export const createBroadcastEventBus = <T>(
  options: BroadcastBusOptions,
): BroadcastBus<T> => {
  const { channelName, maxEvents = 1500 } = options;

  const sourceId =
    typeof crypto !== "undefined" && "randomUUID" in crypto
      ? crypto.randomUUID()
      : `tab-${Math.random().toString(36).slice(2)}`;

  const channel =
    typeof BroadcastChannel !== "undefined"
      ? new BroadcastChannel(channelName)
      : null;

  const eventActions$ = new Subject<EventAction<T>>();

  const events$ = eventActions$.pipe(
    scan((acc, action) => {
      switch (action.type) {
        case "event":
          return [action.event, ...acc].slice(0, maxEvents);
        case "batch":
          return [...action.events, ...acc].slice(0, maxEvents);
        case "clear":
          return [];
        default:
          return acc;
      }
    }, [] as T[]),
    startWith([] as T[]),
    shareReplay({ bufferSize: 1, refCount: false }),
  );

  const eventStream$ = eventActions$.pipe(
    concatMap((action) => {
      if (action.type === "event") {
        return from([action.event]);
      }
      if (action.type === "batch") {
        return from(action.events);
      }
      return EMPTY;
    }),
    share(),
  );

  const streamStatus$ = new BehaviorSubject<StreamStatus>({ connected: false });

  const emitStatus = (next: StreamStatus, broadcast: boolean) => {
    streamStatus$.next(next);
    if (broadcast && channel) {
      channel.postMessage({ type: "status", sourceId, payload: next });
    }
  };

  if (channel) {
    channel.addEventListener("message", (event) => {
      const msg = event.data;
      if (!msg || msg.sourceId === sourceId) return;
      if (msg.type === "events" && Array.isArray(msg.payload)) {
        eventActions$.next({
          type: "batch",
          events: msg.payload as T[],
        });
      }
      if (msg.type === "event" && msg.payload) {
        eventActions$.next({ type: "event", event: msg.payload as T });
      }
      if (msg.type === "status" && msg.payload) {
        emitStatus(msg.payload as StreamStatus, false);
      }
      if (msg.type === "clear") {
        eventActions$.next({ type: "clear" });
      }
    });
  }

  const publishEvent = (event: T) => {
    eventActions$.next({ type: "event", event });
    emitStatus({ connected: true, lastEventAt: Date.now() }, true);
    if (channel) {
      channel.postMessage({ type: "event", sourceId, payload: event });
    }
  };

  const setStreamStatus = (connected: boolean) => {
    emitStatus(
      { connected, lastEventAt: streamStatus$.getValue().lastEventAt },
      true,
    );
  };

  const clearEventBuffer = () => {
    eventActions$.next({ type: "clear" });
    if (channel) {
      channel.postMessage({ type: "clear", sourceId });
    }
  };

  return {
    events$,
    eventStream$,
    streamStatus$,
    publishEvent,
    setStreamStatus,
    clearEventBuffer,
  };
};
