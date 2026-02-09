package MBRound18.hytale.dungeonmaster;

import MBRound18.ImmortalEngine.api.prefab.PrefabInspector;
import MBRound18.hytale.dungeonmaster.handlers.EventTypesHandler;
import MBRound18.hytale.dungeonmaster.handlers.EventsPollHandler;
import MBRound18.hytale.dungeonmaster.handlers.PlayersHandler;
import MBRound18.hytale.dungeonmaster.handlers.PrefabMetadataHandler;
import MBRound18.hytale.dungeonmaster.handlers.OpenApiHandler;
import MBRound18.hytale.dungeonmaster.handlers.SseHandler;
import MBRound18.hytale.dungeonmaster.handlers.StatsHandler;
import MBRound18.hytale.dungeonmaster.handlers.WorldsHandler;
import MBRound18.hytale.dungeonmaster.helpers.EventEnvelope;
import MBRound18.hytale.dungeonmaster.helpers.EventSerializationHelper;
import MBRound18.hytale.dungeonmaster.helpers.SseClient;
import MBRound18.hytale.dungeonmaster.helpers.WebContext;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.IBaseEvent;
import com.hypixel.hytale.event.IEvent;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class DebugEventWebServer {
  private final LoggingHelper log;
  private final Gson gson;
  private final DebugServerConfig config;
  private final Deque<EventEnvelope> events;
  private final PrefabInspector prefabInspector;
  private final CopyOnWriteArrayList<SseClient> sseClients;
  private final Set<Class<?>> registeredEventClasses;
  private final ScheduledExecutorService registrar;
  private final AtomicLong lastEventId = new AtomicLong(0L);
  private final StatsHandler statsHandler;
  private final EventTypesHandler eventTypesHandler;
  private final EventsPollHandler eventsPollHandler;
  private final PlayersHandler playersHandler;
  private final WorldsHandler worldsHandler;
  private final PrefabMetadataHandler prefabMetadataHandler;
  private final OpenApiHandler openApiHandler;
  private final SseHandler sseHandler;
  private HttpServer server;

  public DebugEventWebServer(@Nonnull LoggingHelper log, @Nonnull DebugServerConfig config) {
    this.log = Objects.requireNonNull(log, "log");
    this.config = Objects.requireNonNull(config, "config");
    this.gson = EventSerializationHelper.getGson();
    this.events = new ArrayDeque<>();
    this.prefabInspector = new PrefabInspector(log, null);
    this.sseClients = new CopyOnWriteArrayList<>();
    this.registeredEventClasses = ConcurrentHashMap.newKeySet();
    this.registrar = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "dungeonmaster-event-registrar");
      t.setDaemon(true);
      return t;
    });
    this.statsHandler = new StatsHandler(gson,
        () -> registeredEventClasses.size(),
        () -> sseClients.size(),
        () -> events.size());
    this.eventTypesHandler = new EventTypesHandler(gson, registeredEventClasses);
    this.eventsPollHandler = new EventsPollHandler(gson, events);
    this.playersHandler = new PlayersHandler(gson, 50);
    this.worldsHandler = new WorldsHandler(gson);
    this.prefabMetadataHandler = new PrefabMetadataHandler(gson, prefabInspector);
    this.openApiHandler = new OpenApiHandler(gson);
    this.sseHandler = new SseHandler(gson, sseClients);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void start(@Nonnull EventBus eventBus) throws IOException {
    InetSocketAddress addr = new InetSocketAddress(config.bindHost, config.port);
    server = HttpServer.create(addr, 0);

    // Register contexts using the cleaner "WebContext" wrapper
    server.createContext("/api/health", ex -> new WebContext(ex, gson).text(200, "ok"));

    // Event Streaming & Polling
    server.createContext("/api/events", sseHandler::handle);
    server.createContext("/api/events/poll", eventsPollHandler::handle);
    server.createContext("/api/events/types", eventTypesHandler::handle);
    server.createContext("/events", sseHandler::handle);

    // Metadata & Stats
    server.createContext("/api/stats", statsHandler::handle);
    server.createContext("/api/metadata/players", playersHandler::handle);
    server.createContext("/api/metadata/worlds", worldsHandler::handle);
    server.createContext("/api/metadata/prefab", prefabMetadataHandler::handle);
    server.createContext("/api/openapi.json", openApiHandler::handle);

    server.setExecutor(Executors.newCachedThreadPool(r -> {
      Thread t = new Thread(r, "dungeonmaster-web");
      t.setDaemon(true);
      return t;
    }));

    registerAllKnown(eventBus);
    registrar.scheduleAtFixedRate(() -> registerAllKnown(eventBus), 5, 10, TimeUnit.SECONDS);

    server.start();
    log.fine("DebugEventWebServer listening on %s", getListenAddress());
  }

  public void stop() {
    if (server != null) {
      server.stop(0);
      server = null;
    }
    for (SseClient client : sseClients) {
      client.close();
    }
    sseClients.clear();
    registrar.shutdownNow();
  }

  public String getListenAddress() {
    String host = config.bindHost == null || config.bindHost.isBlank() ? "0.0.0.0" : config.bindHost;
    return "http://" + host + ":" + config.port;
  }

  // --- Helpers ---

  private void recordEvent(@Nonnull IEvent<?> event) {
    long id = lastEventId.incrementAndGet();
    EventEnvelope envelope = new EventEnvelope(id, System.currentTimeMillis(),
        Objects.requireNonNull(event.getClass().getName(), "type"),
        Objects.requireNonNull(serializeEvent(event), "payload"));

    synchronized (events) {
      events.addLast(envelope);
      while (events.size() > Math.max(1, config.maxEvents)) {
        events.removeFirst();
      }
    }
    broadcastEvent(envelope);
  }

  private void broadcastEvent(@Nonnull EventEnvelope envelope) {
    String data = EventSerializationHelper.toJson(envelope);
    String payload = "data: " + data + "\n\n";
    byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

    for (SseClient client : sseClients) {
      // Apply server-side filtering if the client requested it
      if (!client.accepts(envelope.type())) {
        continue;
      }

      if (!client.send(bytes)) {
        sseClients.remove(client);
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void registerAllKnown(@Nonnull EventBus eventBus) {
    try {
      Set<Class<? extends IBaseEvent<?>>> currentClasses = new HashSet<>(eventBus.getRegisteredEventClasses());
      for (Class<? extends IBaseEvent<?>> eventClass : currentClasses) {
        if (eventClass == null || !registeredEventClasses.add(eventClass)) {
          continue;
        }
        Consumer<IBaseEvent<Void>> listener = (IBaseEvent<Void> e) -> {
          if (e instanceof IEvent event) {
            recordEvent(event);
          }
        };
        eventBus.register((Class<? super IBaseEvent<Void>>) eventClass, listener);
      }
    } catch (Exception e) {
      log.error("Failed to register events: %s", e.getMessage());
    }
  }

  private JsonElement serializeEvent(@Nonnull IEvent<?> event) {
    try {
      return EventSerializationHelper.serializeSafe(event);
    } catch (Throwable e) {
      log.fine("Failed to serialize event %s: %s", event.getClass().getName(), e.getMessage());
      JsonObject fallback = new JsonObject();
      fallback.addProperty("_error", "Serialization failed");
      fallback.addProperty("_message", e.getMessage());
      return fallback;
    }
  }

}