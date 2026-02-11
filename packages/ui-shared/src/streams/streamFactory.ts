import { Observable, defer, from, interval, merge, of, timer } from "rxjs";
import {
  catchError,
  ignoreElements,
  retryWhen,
  scan,
  switchMap,
  tap,
} from "rxjs/operators";

export type StreamStatus = {
  connected: boolean;
  retryIn?: number;
  lastEventAt?: number;
};

type RetryBackoffOptions = {
  baseDelayMs?: number;
  maxDelayMs?: number;
  maxExponent?: number;
  jitterMs?: number;
  healthCheck?: () => Promise<boolean>;
  onStatus?: (status: StreamStatus) => void;
  getLastEventAt?: () => number;
};

const withRetryBackoff = <T>(
  sourceFactory: () => Observable<T>,
  options: RetryBackoffOptions,
): Observable<T> => {
  const {
    baseDelayMs = 1000,
    maxDelayMs = 30000,
    maxExponent = 5,
    jitterMs = 500,
    healthCheck,
    onStatus,
    getLastEventAt,
  } = options;

  return defer(sourceFactory).pipe(
    retryWhen((errors) =>
      errors.pipe(
        tap(() => {
          onStatus?.({ connected: false, lastEventAt: getLastEventAt?.() });
        }),
        scan((attempt) => attempt + 1, 0),
        switchMap((attempt) =>
          from(healthCheck ? healthCheck() : Promise.resolve(false)).pipe(
            catchError(() => of(false)),
            switchMap((healthy) => {
              const cappedAttempt = Math.min(attempt, maxExponent);
              const baseDelay = Math.min(
                maxDelayMs,
                baseDelayMs * Math.pow(2, cappedAttempt),
              );
              const jitter = Math.floor(Math.random() * jitterMs);
              const delayMs = healthy ? 0 : baseDelay + jitter;
              if (delayMs > 0) {
                onStatus?.({
                  connected: false,
                  retryIn: Math.max(1, Math.round(delayMs / 1000)),
                  lastEventAt: getLastEventAt?.(),
                });
              }
              return timer(delayMs);
            }),
          ),
        ),
      ),
    ),
  );
};

export type EventSourceStreamOptions<T> = {
  url: string | (() => string);
  eventTypes?: string[];
  parse?: (event: MessageEvent, type: string) => T;
  onOpen?: () => void;
  onEvent?: (event: MessageEvent, type: string) => void;
  onStatus?: (status: StreamStatus) => void;
  healthCheck?: () => Promise<boolean>;
  heartbeatMs?: number;
  staleMs?: number;
};

export const createEventSourceStream = <T = { type: string; data: string }>(
  options: EventSourceStreamOptions<T>,
): Observable<T> => {
  const {
    url,
    eventTypes = ["message"],
    parse = (event, type) => ({ type, data: event.data }) as T,
    onOpen,
    onEvent,
    onStatus,
    healthCheck,
    heartbeatMs = 5000,
    staleMs = 15000,
  } = options;

  let lastEventAt = 0;

  const getUrl = () => (typeof url === "function" ? url() : url);

  const sourceFactory = () =>
    merge(
      new Observable<T>((subscriber) => {
        const source = new EventSource(getUrl());
        let closed = false;

        const handleOpen = () => {
          lastEventAt = Date.now();
          onStatus?.({ connected: true, lastEventAt });
          onOpen?.();
        };

        const handleMessage = (type: string) => (event: Event) => {
          lastEventAt = Date.now();
          const msg = event as MessageEvent;
          onEvent?.(msg, type);
          subscriber.next(parse(msg, type));
        };

        const handleError = () => {
          if (closed) return;
          onStatus?.({ connected: false, lastEventAt });
          subscriber.error(new Error("sse_error"));
        };

        source.addEventListener("open", handleOpen);
        eventTypes.forEach((type) =>
          source.addEventListener(type, handleMessage(type)),
        );
        source.addEventListener("error", handleError);

        return () => {
          closed = true;
          source.close();
        };
      }),
      interval(heartbeatMs).pipe(
        tap(() => {
          if (lastEventAt > 0 && Date.now() - lastEventAt > staleMs) {
            onStatus?.({ connected: false, lastEventAt });
            throw new Error("sse_stale");
          }
        }),
        ignoreElements(),
      ),
    );

  return withRetryBackoff(sourceFactory, {
    onStatus,
    healthCheck,
    getLastEventAt: () => lastEventAt,
  });
};

export type WebSocketStreamOptions<T> = {
  url: string | (() => string);
  parse?: (event: MessageEvent) => T;
  onOpen?: () => void;
  onStatus?: (status: StreamStatus) => void;
  healthCheck?: () => Promise<boolean>;
};

export const createWebSocketStream = <T = string>(
  options: WebSocketStreamOptions<T>,
): Observable<T> => {
  const { url, parse, onOpen, onStatus, healthCheck } = options;
  let lastEventAt = 0;

  const getUrl = () => (typeof url === "function" ? url() : url);

  const sourceFactory = () =>
    new Observable<T>((subscriber) => {
      const socket = new WebSocket(getUrl());
      let closed = false;

      socket.onopen = () => {
        lastEventAt = Date.now();
        onStatus?.({ connected: true, lastEventAt });
        onOpen?.();
      };

      socket.onmessage = (event) => {
        lastEventAt = Date.now();
        subscriber.next(parse ? parse(event) : (event.data as T));
      };

      socket.onclose = () => {
        if (closed) return;
        onStatus?.({ connected: false, lastEventAt });
        subscriber.error(new Error("ws_close"));
      };

      socket.onerror = () => {
        if (closed) return;
        onStatus?.({ connected: false, lastEventAt });
        subscriber.error(new Error("ws_error"));
      };

      return () => {
        closed = true;
        socket.close();
      };
    });

  return withRetryBackoff(sourceFactory, {
    onStatus,
    healthCheck,
    getLastEventAt: () => lastEventAt,
  });
};
