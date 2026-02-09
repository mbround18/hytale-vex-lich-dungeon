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
import MBRound18.hytale.dungeonmaster.generated.EventClassCatalog;
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
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;
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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
    PrefabInspector inspector = null;
    try {
      inspector = new PrefabInspector(log, null);
    } catch (LinkageError | Exception e) {
      log.warn("PrefabInspector unavailable: %s", e.getMessage());
    }
    this.prefabInspector = inspector;
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
    this.sseHandler = new SseHandler(gson, sseClients, events);
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

    // Ensure custom DebugEvent/IEvent classes are registered even if no other listeners exist yet.
    registerExplicitKnownEvents(eventBus);
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

  public int getRegisteredEventCount() {
    return registeredEventClasses.size();
  }

  // --- Helpers ---

  private void recordEvent(@Nonnull IBaseEvent<?> event) {
    if (event == null) {
      log.warn("Received null event, skipping");
      return;
    }
    long id = lastEventId.incrementAndGet();
    String eventType = Objects.requireNonNull(event.getClass().getName(), "type");
    EventEnvelope envelope = new EventEnvelope(id, System.currentTimeMillis(),
        eventType,
        Objects.requireNonNull(serializeEvent(event), "payload"));

    synchronized (events) {
      events.addLast(envelope);
      while (events.size() > Math.max(1, config.maxEvents)) {
        events.removeFirst();
      }
    }
    log.debug("Recorded event [%d]: %s", id, eventType);
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

  @SuppressWarnings({ "unchecked" })
  private void registerAllKnown(@Nonnull EventBus eventBus) {
    try {
      Set<Class<? extends IBaseEvent<?>>> currentClasses = new HashSet<>(eventBus.getRegisteredEventClasses());
      int newRegistrations = 0;
      for (Class<? extends IBaseEvent<?>> eventClass : currentClasses) {
        if (eventClass == null || !registeredEventClasses.add(eventClass)) {
          continue;
        }
        Consumer<IBaseEvent<Void>> listener = (IBaseEvent<Void> e) -> {
          recordEvent(e);
        };
        eventBus.register((Class<? super IBaseEvent<Void>>) eventClass, listener);
        newRegistrations++;
      }
      if (newRegistrations > 0) {
        log.fine("Registered %d new event listener(s). Total: %d", newRegistrations, registeredEventClasses.size());
      }
    } catch (Exception e) {
      log.error("Failed to register events: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void registerExplicitKnownEvents(@Nonnull EventBus eventBus) {
    Set<Class<?>> eventClasses = resolveExplicitEventClasses();
    int registered = 0;
    int failed = 0;
    for (Class<?> eventClass : eventClasses) {
      if (eventClass == null || !IBaseEvent.class.isAssignableFrom(eventClass)) {
        continue;
      }
      try {
        registerExplicit(eventBus, eventClass);
        registered++;
      } catch (Exception e) {
        failed++;
      }
    }
    if (eventClasses.isEmpty()) {
      log.warn("No explicit event classes discovered; custom events with no listeners will not fire.");
    }
    if (registered > 0 || failed > 0) {
      log.info("Registered %d explicit event listeners (%d failed)", registered, failed);
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void registerExplicit(@Nonnull EventBus eventBus, @Nonnull Class<?> eventClass) {
    if (!registeredEventClasses.add(eventClass)) {
      return;
    }
    Consumer<IBaseEvent<Void>> listener = (IBaseEvent<Void> e) -> {
      recordEvent(e);
    };
    eventBus.register((Class<? super IBaseEvent<Void>>) eventClass, listener);
    log.debug("Registered listener for: %s", eventClass.getSimpleName());
  }

  private Set<Class<?>> resolveExplicitEventClasses() {
    Set<Class<?>> classes = new HashSet<>();
    if (EventClassCatalog.EVENT_CLASS_NAMES != null && EventClassCatalog.EVENT_CLASS_NAMES.length > 0) {
      int failed = 0;
      for (String className : EventClassCatalog.EVENT_CLASS_NAMES) {
        if (className == null || className.isBlank()) {
          continue;
        }
        ClassLoader loader = resolveEventClassLoader(className);
        Class<?> eventClass = loadEventClass(loader, className);
        if (eventClass == null && loader != DebugEventWebServer.class.getClassLoader()) {
          eventClass = loadEventClass(DebugEventWebServer.class.getClassLoader(), className);
        }
        if (eventClass == null) {
          failed++;
          continue;
        }
        classes.add(eventClass);
      }
      if (failed > 0) {
        log.warn("Event catalog resolved with %d failures.", failed);
      }
      return classes;
    }

    log.warn("Event catalog is empty; falling back to runtime classpath scan.");
    scanEventPackage(classes, resolvePluginClassLoader("ImmortalEngine"), "MBRound18.ImmortalEngine.api.events");
    scanEventPackage(classes, resolvePluginClassLoader("VexLichDungeon"), "MBRound18.hytale.vexlichdungeon.events");
    scanEventPackage(classes, DebugEventWebServer.class.getClassLoader(), "MBRound18.hytale.dungeonmaster");
    return classes;
  }

  private void scanEventPackage(@Nonnull Set<Class<?>> results, @Nonnull ClassLoader loader, @Nonnull String packageName) {
    String path = packageName.replace('.', '/');
    try {
      Enumeration<URL> resources = loader.getResources(path);
      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        String protocol = resource.getProtocol();
        if ("file".equals(protocol)) {
          try {
            URI uri = resource.toURI();
            Path dir = Paths.get(uri);
            if (Files.exists(dir)) {
              Files.walk(dir)
                  .filter(p -> p.toString().endsWith(".class"))
                  .forEach(p -> {
                    String className = toClassName(dir, p, packageName);
                    addIfEvent(results, className, loader);
                  });
            }
          } catch (Exception e) {
            log.debug("Failed to scan directory for %s: %s", packageName, e.getMessage());
          }
        } else if ("jar".equals(protocol)) {
          try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jar = connection.getJarFile()) {
              Enumeration<JarEntry> entries = jar.entries();
              while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(path) || !name.endsWith(".class") || name.contains("$")) {
                  continue;
                }
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                addIfEvent(results, className, loader);
              }
            }
          } catch (Exception e) {
            log.debug("Failed to scan jar for %s: %s", packageName, e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      log.debug("Failed to scan resources for %s: %s", packageName, e.getMessage());
    }
  }

  private String toClassName(@Nonnull Path root, @Nonnull Path file, @Nonnull String packageName) {
    Path relative = root.relativize(file);
    String suffix = relative.toString().replace(File.separatorChar, '.');
    if (suffix.endsWith(".class")) {
      suffix = suffix.substring(0, suffix.length() - 6);
    }
    return packageName + "." + suffix;
  }

  private void addIfEvent(@Nonnull Set<Class<?>> results, @Nonnull String className, @Nonnull ClassLoader loader) {
    if (className.contains("$")) {
      return;
    }
    try {
      Class<?> clazz = Class.forName(className, false, loader);
      if (!IBaseEvent.class.isAssignableFrom(clazz)) {
        return;
      }
      if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
        return;
      }
      results.add(clazz);
    } catch (ClassNotFoundException | LinkageError ignored) {
      // ignore
    } catch (Exception ignored) {
      // ignore
    }
  }

  private ClassLoader resolveEventClassLoader(@Nonnull String className) {
    try {
      if (className.startsWith("MBRound18.ImmortalEngine.")) {
        return resolvePluginClassLoader("ImmortalEngine");
      }
      if (className.startsWith("MBRound18.hytale.vexlichdungeon.")) {
        return resolvePluginClassLoader("VexLichDungeon");
      }
      if (className.startsWith("MBRound18.hytale.dungeonmaster.")) {
        return DebugEventWebServer.class.getClassLoader();
      }
      return DebugEventWebServer.class.getClassLoader();
    } catch (Exception e) {
      log.warn("Failed to resolve plugin classloader for %s: %s", className, e.getMessage());
    }
    return DebugEventWebServer.class.getClassLoader();
  }

  private ClassLoader resolvePluginClassLoader(@Nonnull String pluginName) {
    try {
      PluginManager pluginManager = PluginManager.get();
      if (pluginManager != null) {
        for (PluginBase plugin : pluginManager.getPlugins()) {
          if (plugin != null && pluginName.equals(plugin.getName()) && plugin instanceof JavaPlugin javaPlugin) {
            return javaPlugin.getClassLoader();
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to resolve plugin classloader for %s: %s", pluginName, e.getMessage());
    }
    return DebugEventWebServer.class.getClassLoader();
  }

  private Class<?> loadEventClass(ClassLoader loader, String className) {
    try {
      return Class.forName(className, false, loader);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private JsonElement serializeEvent(@Nonnull IBaseEvent<?> event) {
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
