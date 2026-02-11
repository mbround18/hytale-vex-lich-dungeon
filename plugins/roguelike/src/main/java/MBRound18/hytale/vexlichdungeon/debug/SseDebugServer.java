package MBRound18.hytale.vexlichdungeon.debug;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.events.RoomCoordinate;
import MBRound18.hytale.vexlichdungeon.data.ArchiveRecord;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.ImmortalEngine.api.prefab.PrefabInspector;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.IBaseEvent;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;
import java.io.IOException;
import java.io.File;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.lang.reflect.Modifier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SseDebugServer {
  private static final int PORT = 3390;
  private static final String BIND_ADDRESS = "0.0.0.0";
  private static final int MAX_FIELDS = 20;
  private static final int MAX_COLLECTION = 12;
  private static final int MAX_STRING = 512;
  private static final int MAX_RECENT_EVENTS = 200;

  private final LoggingHelper log;
  private final PrefabSpawner prefabSpawner;
  private final DataStore dataStore;
  private final Gson gson = new Gson();
  private volatile Predicate<String> instanceFilter;
  private volatile boolean minimalPayload;
  private final GraphQL graphQL;
  private final Path unpackedRoot;
  private final List<Client> clients = new CopyOnWriteArrayList<>();
  private final Deque<String> recentEvents = new ArrayDeque<>();
  private final ExecutorService broadcastExecutor;
  private final ScheduledExecutorService scheduler;
  private final Map<String, List<Map<String, Object>>> playerSnapshots = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> playerSnapshotUpdatedAt = new ConcurrentHashMap<>();
  private final Map<String, AtomicBoolean> playerSnapshotInFlight = new ConcurrentHashMap<>();
  private final Map<String, AtomicBoolean> prefabInFlight = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> prefabUpdatedAt = new ConcurrentHashMap<>();
  private volatile Javalin app;
  private volatile Thread serverThread;
  private final AtomicBoolean started = new AtomicBoolean(false);

  public SseDebugServer(@Nonnull LoggingHelper log, @Nonnull PrefabSpawner prefabSpawner,
      @Nonnull DataStore dataStore) {
    this.log = Objects.requireNonNull(log, "log");
    this.prefabSpawner = Objects.requireNonNull(prefabSpawner, "prefabSpawner");
    this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
    this.unpackedRoot = Path.of("data", "unpacked").toAbsolutePath().normalize();
    GraphQL gql = null;
    try {
      gql = buildGraphQL();
    } catch (Exception e) {
      log.error("[SSE] GraphQL initialization failed: %s", e.getMessage());
    }
    this.graphQL = gql;
    this.broadcastExecutor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "VexSseBroadcaster");
      t.setDaemon(true);
      return t;
    });
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "VexSseScheduler");
      t.setDaemon(true);
      return t;
    });
  }

  public synchronized void start(@Nonnull EventBus eventBus) {
    if (!started.compareAndSet(false, true)) {
      return;
    }
    serverThread = new Thread(() -> {
      ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(SseDebugServer.class.getClassLoader());
      try {
        app = Javalin.create(config -> {
          config.showJavalinBanner = false;
        });

        app.exception(Exception.class, (e, ctx) -> {
          log.error("[SSE] Request failed: %s", e.getMessage());
          ctx.status(500);
          ctx.contentType("application/json; charset=utf-8");
          ctx.result("{\"error\":\"internal error\"}");
        });

        app.before(ctx -> ctx.header("Access-Control-Allow-Origin", "*"));
        app.options("/*", ctx -> {
          ctx.header("Access-Control-Allow-Origin", "*");
          ctx.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
          ctx.header("Access-Control-Allow-Headers", "*");
          ctx.status(204);
        });

        app.get("/api/health", this::handleHealth);
        app.get("/api/metadata/players", this::handlePlayerMetadata);
        app.get("/api/metadata/prefab/{id}", this::handlePrefabMetadata);
        app.get("/api/archives", this::handleArchivesList);
        app.get("/api/archives/{id}", this::handleArchiveGet);
        app.post("/api/archives", this::handleArchiveSave);
        app.delete("/api/archives", this::handleArchivesClear);
        app.delete("/api/archives/{id}", this::handleArchiveDelete);
        app.post("/api/graphql", this::handleGraphQL);
        app.sse("/api/events", this::handleEventStream);

        app.get("/", this::serveDebugResource);
        app.get("/*", this::serveDebugResource);

        app.start(BIND_ADDRESS, PORT);
      } catch (Exception e) {
        log.error("[SSE] Failed to start debug server on port %d: %s", PORT, e.getMessage());
        app = null;
        started.set(false);
      } finally {
        Thread.currentThread().setContextClassLoader(originalLoader);
      }
    }, "Vex-Javalin-Web-Server");
    serverThread.setDaemon(true);
    serverThread.start();
    broadcastExecutor.execute(() -> {
      try {
        registerEvents(eventBus);
      } catch (Exception e) {
        log.error("[SSE] Event registration failed: %s", e.getMessage());
      }
      try {
        scheduler.scheduleAtFixedRate(this::refreshPlayerSnapshots, 10, 2, TimeUnit.SECONDS);
      } catch (Exception e) {
        log.error("[SSE] Snapshot scheduler failed: %s", e.getMessage());
      }
    });
  }

  public synchronized void stop() {
    if (!started.compareAndSet(true, false)) {
      return;
    }
    scheduler.shutdownNow();
    broadcastExecutor.shutdownNow();
    if (app != null) {
      app.stop();
      app = null;
    }
    if (serverThread != null) {
      serverThread.interrupt();
      serverThread = null;
    }
  }

  public void setInstanceFilter(@Nullable Predicate<String> instanceFilter) {
    this.instanceFilter = instanceFilter;
  }

  public void setMinimalPayload(boolean minimalPayload) {
    this.minimalPayload = minimalPayload;
  }

  private void registerEvents(@Nonnull EventBus eventBus) {
    int registered = registerAllEvents(eventBus,
        "MBRound18.ImmortalEngine.api.events",
        "MBRound18.hytale.vexlichdungeon.events",
        "com.hypixel.hytale.assetstore.event");
    log.info("[SSE] Registered %d events", registered);
  }

  private int registerAllEvents(@Nonnull EventBus eventBus, @Nonnull String... packages) {
    Set<Class<?>> eventClasses = new HashSet<>();
    for (String pkg : packages) {
      if (pkg == null) {
        continue;
      }
      eventClasses.addAll(findEventClasses(pkg));
    }
    for (Class<?> eventClass : eventClasses) {
      if (eventClass != null) {
        register(eventBus, eventClass);
      }
    }
    return eventClasses.size();
  }

  private Set<Class<?>> findEventClasses(@Nonnull String packageName) {
    Set<Class<?>> results = new HashSet<>();
    String path = packageName.replace('.', '/');
    ClassLoader classLoader = SseDebugServer.class.getClassLoader();
    try {
      Enumeration<URL> resources = classLoader.getResources(path);
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
                    addIfEvent(results, className, classLoader);
                  });
            }
          } catch (Exception e) {
            log.debug("[SSE] Failed to scan directory for %s: %s", packageName, e.getMessage());
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
                addIfEvent(results, className, classLoader);
              }
            }
          } catch (IOException e) {
            log.debug("[SSE] Failed to scan jar for %s: %s", packageName, e.getMessage());
          }
        }
      }
    } catch (IOException e) {
      log.debug("[SSE] Failed to find resources for %s: %s", packageName, e.getMessage());
    }
    return results;
  }

  private String toClassName(Path root, Path file, String packageName) {
    Path relative = root.relativize(file);
    String suffix = relative.toString().replace(File.separatorChar, '.');
    if (suffix.endsWith(".class")) {
      suffix = suffix.substring(0, suffix.length() - 6);
    }
    return packageName + "." + suffix;
  }

  private void addIfEvent(Set<Class<?>> results, String className, ClassLoader classLoader) {
    if (className.contains("$")) {
      return;
    }
    try {
      Class<?> clazz = Class.forName(className, false, classLoader);
      if (!IBaseEvent.class.isAssignableFrom(clazz) && !IEvent.class.isAssignableFrom(clazz)) {
        return;
      }
      if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
        return;
      }
      results.add((Class<?>) clazz);
    } catch (ClassNotFoundException e) {
      // ignore
    } catch (LinkageError e) {
      // ignore
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void register(@Nonnull EventBus eventBus, @Nonnull Class<?> eventClass) {
    eventBus.register((Class) eventClass, (java.util.function.Consumer) (Object e) -> publishEvent(eventClass, e));
  }

  private void publishEvent(@Nonnull Class<?> eventClass, Object event) {
    Map<String, Object> payload = buildPayload(eventClass, event);
    String worldName = extractWorldName(payload);
    if (!isWorldAllowed(worldName)) {
      return;
    }
    String json = toJson(payload);
    cacheRecentEvent(json);
    broadcastEvent("message", json);
    maybeSendPrefabMetadata(payload);
  }

  private void cacheRecentEvent(String message) {
    if (message == null) {
      return;
    }
    synchronized (recentEvents) {
      recentEvents.addLast(message);
      while (recentEvents.size() > MAX_RECENT_EVENTS) {
        recentEvents.removeFirst();
      }
    }
  }

  private Map<String, Object> buildPayload(@Nonnull Class<?> eventClass, Object event) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("type", eventClass.getSimpleName());
    payload.put("class", eventClass.getName());
    payload.put("timestamp", Instant.now().toString());
    Map<String, Object> fields = extractFields(event);
    if (event instanceof DebugEvent debugEvent) {
      fields.putIfAbsent("correlationId", debugEvent.getCorrelationId());
    }
    payload.put("fields", fields);
    return payload;
  }

  private String extractWorldName(Map<String, Object> payload) {
    if (payload == null) {
      return null;
    }
    Object fieldsObj = payload.get("fields");
    if (!(fieldsObj instanceof Map<?, ?>)) {
      return null;
    }
    Map<?, ?> fields = (Map<?, ?>) fieldsObj;
    Object worldObj = fields.get("world");
    if (worldObj instanceof Map<?, ?>) {
      Object name = ((Map<?, ?>) worldObj).get("name");
      return name == null ? null : String.valueOf(name);
    }
    Object worldName = fields.get("worldName");
    return worldName == null ? null : String.valueOf(worldName);
  }

  private boolean isWorldAllowed(String worldName) {
    Predicate<String> filter = instanceFilter;
    if (filter == null) {
      return true;
    }
    if (worldName == null) {
      return true;
    }
    return filter.test(worldName);
  }

  private Map<String, Object> extractFields(Object event) {
    if (event == null) {
      return new LinkedHashMap<>();
    }
    Class<?> eventClass = event.getClass();
    Map<String, Object> fields = new LinkedHashMap<>();
    int added = 0;
    for (java.lang.reflect.Method method : eventClass.getMethods()) {
      if (added >= MAX_FIELDS) {
        break;
      }
      String name = method.getName();
      if (!name.startsWith("get") && !name.startsWith("is")) {
        continue;
      }
      if (method.getParameterCount() != 0 || method.getReturnType() == Void.TYPE) {
        continue;
      }
      if ("getClass".equals(name)) {
        continue;
      }
      try {
        Object value = method.invoke(event);
        if (value == null) {
          continue;
        }
        String key = name.startsWith("get") ? lowerFirst(name.substring(3)) : lowerFirst(name.substring(2));
        if (!shouldIncludeField(key, eventClass)) {
          continue;
        }
        Object normalized = normalizeValue(value);
        if (normalized == null) {
          continue;
        }
        fields.put(key, normalized);
        added++;
      } catch (Exception ignored) {
        // ignore
      }
    }
    return fields;
  }

  private Object normalizeValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Class<?>) {
      return ((Class<?>) value).getName();
    }
    if (value instanceof String) {
      return trimString((String) value);
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value;
    }
    if (value instanceof UUID) {
      return value.toString();
    }
    if (value instanceof AssetMap<?, ?>) {
      AssetMap<?, ?> assetMap = (AssetMap<?, ?>) value;
      Map<String, Object> meta = new LinkedHashMap<>();
      meta.put("assetCount", assetMap.getAssetCount());
      meta.put("tagCount", assetMap.getTagCount());
      return meta;
    }
    if (value instanceof World) {
      World world = (World) value;
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("name", world.getName());
      return map;
    }
    if (value instanceof PlayerRef) {
      PlayerRef player = (PlayerRef) value;
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("uuid", player.getUuid() == null ? null : player.getUuid().toString());
      map.put("name", player.getUsername());
      return map;
    }
    if (value instanceof Vector3i) {
      Vector3i vec = (Vector3i) value;
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("x", vec.x);
      map.put("y", vec.y);
      map.put("z", vec.z);
      return map;
    }
    if (value instanceof Vector3f) {
      Vector3f vec = (Vector3f) value;
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("x", vec.x);
      map.put("y", vec.y);
      map.put("z", vec.z);
      return map;
    }
    if (value instanceof RoomCoordinate) {
      RoomCoordinate room = (RoomCoordinate) value;
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("x", room.getX());
      map.put("z", room.getZ());
      return map;
    }
    if (value instanceof Enum<?>) {
      return ((Enum<?>) value).name();
    }
    if (value instanceof Collection<?>) {
      return normalizeCollection((Collection<?>) value);
    }
    if (value instanceof Map<?, ?> && minimalPayload) {
      Map<?, ?> mapValue = (Map<?, ?>) value;
      Map<String, Object> meta = new LinkedHashMap<>();
      meta.put("count", mapValue.size());
      return meta;
    }
    if (minimalPayload) {
      return null;
    }
    return trimString(String.valueOf(value));
  }

  private boolean shouldIncludeField(String key, @Nullable Class<?> eventClass) {
    if (!minimalPayload) {
      return true;
    }
    if (eventClass != null && eventClass.getName().startsWith("com.hypixel.hytale.assetstore.event")) {
      return true;
    }
    String lower = key == null ? "" : key.toLowerCase();
    return lower.contains("world")
        || lower.contains("instance")
        || lower.contains("player")
        || lower.contains("room")
        || lower.contains("position")
        || lower.contains("location");
  }

  private List<Object> normalizeCollection(Collection<?> collection) {
    if (collection == null) {
      return List.of();
    }
    List<Object> list = new ArrayList<>();
    int count = 0;
    for (Object item : collection) {
      if (count >= MAX_COLLECTION) {
        list.add("..." + (collection.size() - MAX_COLLECTION) + " more");
        break;
      }
      if (item != null) {
        list.add(normalizeValue(item));
      }
      count++;
    }
    return list;
  }

  private String trimString(String value) {
    if (value == null) {
      return "";
    }
    if (value.length() <= MAX_STRING) {
      return value;
    }
    return value.substring(0, MAX_STRING) + "...";
  }

  private String lowerFirst(String value) {
    if (value == null) {
      return "";
    }
    if (value.isEmpty()) {
      return value;
    }
    return Character.toLowerCase(value.charAt(0)) + value.substring(1);
  }

  private void broadcastEvent(String event, String data) {
    if (data == null || event == null) {
      return;
    }
    if (clients.isEmpty()) {
      return;
    }
    broadcastExecutor.execute(() -> {
      for (Client client : clients) {
        if (!client.send(event, data)) {
          client.close();
        }
      }
    });
  }

  private String toJson(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof String) {
      return "\"" + escapeJson((String) value) + "\"";
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    if (value instanceof Map<?, ?>) {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        if (!first) {
          sb.append(',');
        }
        first = false;
        sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
        sb.append(toJson(entry.getValue()));
      }
      sb.append('}');
      return sb.toString();
    }
    if (value instanceof List<?>) {
      StringBuilder sb = new StringBuilder();
      sb.append('[');
      boolean first = true;
      for (Object item : (List<?>) value) {
        if (!first) {
          sb.append(',');
        }
        first = false;
        sb.append(toJson(item));
      }
      sb.append(']');
      return sb.toString();
    }
    return "\"" + escapeJson(trimString(String.valueOf(value))) + "\"";
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(value.length() + 16);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        default:
          if (c < 32) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
          break;
      }
    }
    return sb.toString();
  }

  private void handleHealth(@Nonnull Context ctx) {
    String body = "VexLichDungeon SSE debug server is running";
    ctx.contentType("text/plain; charset=utf-8");
    ctx.result(body);
  }

  @SuppressWarnings("unchecked")
  private void handleGraphQL(@Nonnull Context ctx) {
    if (graphQL == null) {
      ctx.status(503);
      ctx.result("GraphQL unavailable");
      return;
    }
    String body = ctx.body();
    if (body == null || body.isBlank()) {
      ctx.status(400);
      return;
    }
    Map<String, Object> payload;
    try {
      payload = gson.fromJson(body, Map.class);
    } catch (JsonSyntaxException e) {
      ctx.status(400);
      return;
    }
    if (payload == null) {
      ctx.status(400);
      return;
    }
    Object queryObj = payload.get("query");
    if (!(queryObj instanceof String)) {
      ctx.status(400);
      return;
    }
    String query = (String) queryObj;
    Map<String, Object> variables = Map.of();
    Object vars = payload.get("variables");
    if (vars instanceof Map<?, ?>) {
      variables = (Map<String, Object>) vars;
    }
    ExecutionInput input = ExecutionInput.newExecutionInput()
        .query(query)
        .variables(variables)
        .build();
    ExecutionResult result = graphQL.execute(input);
    ctx.contentType("application/json; charset=utf-8");
    ctx.result(toJson(result.toSpecification()));
  }

  private GraphQL buildGraphQL() {
    String schema = """
        type Query {
          health: String!
          unpackedRoot: String!
          listDir(path: String!, depth: Int = 1): [FileEntry!]!
        }

        type FileEntry {
          name: String!
          path: String!
          type: String!
          size: String
          modified: String
        }
        """;

    TypeDefinitionRegistry registry = new SchemaParser().parse(schema);
    RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
        .type(TypeRuntimeWiring.newTypeWiring("Query")
            .dataFetcher("health", env -> "ok")
            .dataFetcher("unpackedRoot", env -> unpackedRoot.toString())
            .dataFetcher("listDir", env -> {
              String path = env.getArgument("path");
              Integer depth = env.getArgument("depth");
              int safeDepth = depth == null ? 1 : Math.max(0, Math.min(depth, 6));
              return listDirSafe(path, safeDepth);
            }))
        .build();
    return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(registry, wiring)).build();
  }

  private List<Map<String, Object>> listDirSafe(String path, int depth) {
    if (path == null) {
      return List.of();
    }
    Path target = unpackedRoot.resolve(path).normalize();
    if (!target.startsWith(unpackedRoot)) {
      return List.of();
    }
    if (!Files.exists(target)) {
      return List.of();
    }
    List<Map<String, Object>> results = new ArrayList<>();
    try {
      if (Files.isDirectory(target)) {
        try (var stream = Files.list(target)) {
          stream.forEach(child -> {
            results.add(toFileEntry(child));
            if (depth > 1 && Files.isDirectory(child)) {
              results.addAll(listDirSafe(unpackedRoot.relativize(child).toString(), depth - 1));
            }
          });
        }
      } else {
        results.add(toFileEntry(target));
      }
    } catch (IOException e) {
      log.debug("[SSE] listDir failed: %s", e.getMessage());
    }
    return results;
  }

  private Map<String, Object> toFileEntry(Path path) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("name", path.getFileName().toString());
    entry.put("path", unpackedRoot.relativize(path).toString().replace('\\', '/'));
    entry.put("type", Files.isDirectory(path) ? "directory" : "file");
    try {
      entry.put("size", Files.isDirectory(path) ? null : String.valueOf(Files.size(path)));
      entry.put("modified", Files.getLastModifiedTime(path).toInstant().toString());
    } catch (IOException e) {
      entry.put("size", null);
      entry.put("modified", null);
    }
    return entry;
  }

  private String resolveResourcePath(String requestPath) {
    if (requestPath == null || "/".equals(requestPath) || "/index.html".equalsIgnoreCase(requestPath)) {
      return "Debug/index.html";
    }
    if (requestPath.startsWith("/css/")) {
      return "Debug" + requestPath;
    }
    if (requestPath.startsWith("/js/")) {
      return "Debug" + requestPath;
    }
    if (requestPath.startsWith("/assets/")) {
      return "Debug" + requestPath;
    }
    if ("/favicon.ico".equalsIgnoreCase(requestPath)) {
      return "Debug/favicon.ico";
    }
    if (requestPath.endsWith(".json")) {
      return "Debug" + requestPath;
    }
    return null;
  }

  private String resolveContentType(String resourcePath) {
    if (resourcePath.endsWith(".css")) {
      return "text/css; charset=utf-8";
    }
    if (resourcePath.endsWith(".js")) {
      return "text/javascript; charset=utf-8";
    }
    if (resourcePath.endsWith(".json")) {
      return "application/json; charset=utf-8";
    }
    if (resourcePath.endsWith(".svg")) {
      return "image/svg+xml";
    }
    if (resourcePath.endsWith(".ico")) {
      return "image/x-icon";
    }
    if (resourcePath.endsWith(".map")) {
      return "application/json; charset=utf-8";
    }
    if (resourcePath.endsWith(".woff2")) {
      return "font/woff2";
    }
    if (resourcePath.endsWith(".woff")) {
      return "font/woff";
    }
    if (resourcePath.endsWith(".ttf")) {
      return "font/ttf";
    }
    return "text/html; charset=utf-8";
  }

  private String resolveDisplayName(@Nonnull PlayerRef playerRef) {
    String username = playerRef.getUsername();
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return username == null ? "" : username;
    }
    Store<EntityStore> store = ref.getStore();
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      return username == null ? "" : username;
    }
    String displayName = player.getDisplayName();
    if (displayName == null || displayName.isBlank()) {
      return username == null ? "" : username;
    }
    return displayName;
  }

  private void handlePlayerMetadata(@Nonnull Context ctx) {
    try {
      List<Map<String, Object>> players = new ArrayList<>();
      Map<String, World> worlds = Universe.get().getWorlds();
      if (worlds != null) {
        for (World world : worlds.values()) {
          if (world == null) {
            continue;
          }
          if (!isWorldAllowed(world.getName())) {
            continue;
          }
          List<Map<String, Object>> cached = playerSnapshots.get(world.getName());
          if (cached != null && !cached.isEmpty()) {
            players.addAll(new ArrayList<>(cached));
          }
        }
      }

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("timestamp", Instant.now().toString());
      response.put("players", players);

      String json = toJson(response);
      ctx.contentType("application/json; charset=utf-8");
      ctx.result(json);
    } catch (Exception e) {
      log.error("[SSE] Player metadata failed: %s", e.getMessage());
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("timestamp", Instant.now().toString());
      response.put("players", new ArrayList<>());
      response.put("error", "metadata unavailable");
      String json = toJson(response);
      ctx.contentType("application/json; charset=utf-8");
      ctx.result(json);
    }
  }

  private Object parseJsonSafe(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return gson.fromJson(json, Object.class);
    } catch (JsonSyntaxException e) {
      return null;
    }
  }

  private void handleArchivesList(@Nonnull Context ctx) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (ArchiveRecord record : dataStore.getArchives()) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", record.getId());
      item.put("timestamp", record.getTimestamp());
      item.put("data", parseJsonSafe(record.getDataJson()));
      list.add(item);
    }
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("archives", list);
    response.put("timestamp", Instant.now().toString());
    ctx.contentType("application/json; charset=utf-8");
    ctx.result(toJson(response));
  }

  private void handleArchiveGet(@Nonnull Context ctx) {
    String id = ctx.pathParam("id");
    if (id == null || id.isBlank()) {
      ctx.status(400);
      return;
    }
    ArchiveRecord record = dataStore.getArchive(id);
    if (record == null) {
      ctx.status(404);
      return;
    }
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", record.getId());
    response.put("timestamp", record.getTimestamp());
    response.put("data", parseJsonSafe(record.getDataJson()));
    ctx.contentType("application/json; charset=utf-8");
    ctx.result(toJson(response));
  }

  @SuppressWarnings("unchecked")
  private void handleArchiveSave(@Nonnull Context ctx) {
    String body = ctx.body();
    if (body == null || body.isBlank()) {
      ctx.status(400);
      return;
    }
    Map<String, Object> payload;
    try {
      payload = gson.fromJson(body, Map.class);
    } catch (JsonSyntaxException e) {
      ctx.status(400);
      return;
    }
    if (payload == null) {
      ctx.status(400);
      return;
    }
    Object idObj = payload.get("id");
    if (!(idObj instanceof String) || ((String) idObj).isBlank()) {
      ctx.status(400);
      return;
    }
    String id = Objects.requireNonNull((String) idObj, "id");
    String timestamp = Objects.requireNonNull(
        String.valueOf(payload.getOrDefault("timestamp", Instant.now().toString())),
        "timestamp");
    Object dataObj = payload.get("data");
    if (dataObj == null) {
      ctx.status(400);
      return;
    }
    String dataJson = Objects.requireNonNull(gson.toJson(dataObj), "dataJson");
    dataStore.putArchive(new ArchiveRecord(id, timestamp, dataJson));
    ctx.status(204);
  }

  private void handleArchivesClear(@Nonnull Context ctx) {
    dataStore.clearArchives();
    ctx.status(204);
  }

  private void handleArchiveDelete(@Nonnull Context ctx) {
    String id = ctx.pathParam("id");
    if (id == null || id.isBlank()) {
      ctx.status(400);
      return;
    }
    dataStore.removeArchive(id);
    ctx.status(204);
  }

  private void enqueuePlayersSnapshot(@Nonnull World world) {
    String worldName = world.getName();
    long now = System.currentTimeMillis();
    AtomicLong lastRef = playerSnapshotUpdatedAt.computeIfAbsent(worldName, k -> new AtomicLong(0));
    long last = lastRef.get();
    if (now - last < 500) {
      return;
    }
    if (!lastRef.compareAndSet(last, now)) {
      return;
    }
    AtomicBoolean inFlight = playerSnapshotInFlight.computeIfAbsent(worldName, k -> new AtomicBoolean(false));
    if (!inFlight.compareAndSet(false, true)) {
      return;
    }
    world.execute(() -> {
      try {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (PlayerRef playerRef : world.getPlayerRefs()) {
          if (playerRef == null || !playerRef.isValid()) {
            continue;
          }
          Ref<EntityStore> ref = playerRef.getReference();
          if (ref == null || !ref.isValid()) {
            continue;
          }
          Store<EntityStore> store = ref.getStore();
          TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
          if (transform == null) {
            continue;
          }
          Vector3d pos = transform.getPosition();
          Map<String, Object> position = new LinkedHashMap<>();
          position.put("x", pos.x);
          position.put("y", pos.y);
          position.put("z", pos.z);

          Map<String, Object> payload = new LinkedHashMap<>();
          payload.put("playerId", playerRef.getUuid().toString());
          payload.put("name", resolveDisplayName(playerRef));
          payload.put("world", worldName);
          payload.put("position", Collections.unmodifiableMap(position));
          snapshot.add(Collections.unmodifiableMap(payload));
        }
        playerSnapshots.put(worldName, Collections.unmodifiableList(snapshot));
      } finally {
        inFlight.set(false);
      }
    });
  }

  private void refreshPlayerSnapshots() {
    Map<String, World> worlds = Universe.get().getWorlds();
    if (worlds == null || worlds.isEmpty()) {
      return;
    }
    for (World world : worlds.values()) {
      if (world == null) {
        continue;
      }
      enqueuePlayersSnapshot(world);
    }
  }

  private void maybeSendPrefabMetadata(Map<String, Object> payload) {
    if (payload == null) {
      return;
    }
    Object fieldsObj = payload.get("fields");
    if (!(fieldsObj instanceof Map<?, ?>)) {
      return;
    }
    Map<?, ?> fields = (Map<?, ?>) fieldsObj;
    Object prefabPath = fields.get("prefabPath");
    if (prefabPath == null) {
      prefabPath = fields.get("prefab");
    }
    if (!(prefabPath instanceof String)) {
      return;
    }
    sendPrefabPayload((String) prefabPath);
  }

  private void sendPrefabPayload(@Nonnull String prefabPath) {
    if (prefabPath.isBlank()) {
      return;
    }
    AtomicLong lastRef = prefabUpdatedAt.computeIfAbsent(prefabPath, k -> new AtomicLong(0));
    long now = System.currentTimeMillis();
    long last = lastRef.get();
    if (now - last < 1000) {
      return;
    }
    if (!lastRef.compareAndSet(last, now)) {
      return;
    }
    AtomicBoolean inFlight = prefabInFlight.computeIfAbsent(prefabPath, k -> new AtomicBoolean(false));
    if (!inFlight.compareAndSet(false, true)) {
      return;
    }
    scheduler.execute(() -> {
      try {
        PrefabInspector.PrefabDimensions dims = prefabSpawner.getPrefabDimensions(prefabPath);
        Map<String, Object> size = new LinkedHashMap<>();
        size.put("w", dims.width);
        size.put("h", dims.depth);

        Map<String, Object> bounds = new LinkedHashMap<>();
        bounds.put("minX", dims.minX);
        bounds.put("maxX", dims.maxX);
        bounds.put("minY", dims.minY);
        bounds.put("maxY", dims.maxY);
        bounds.put("minZ", dims.minZ);
        bounds.put("maxZ", dims.maxZ);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prefabPath", prefabPath);
        payload.put("roomSize", size);
        payload.put("bounds", bounds);

        String json = toJson(payload);
        broadcastEvent("prefab", json);
      } catch (Exception e) {
        log.debug("[SSE] Prefab snapshot failed: %s", e.getMessage());
      } finally {
        inFlight.set(false);
      }
    });
  }

  private void handlePrefabMetadata(@Nonnull Context ctx) {
    String rawId;
    try {
      rawId = ctx.pathParam("id");
    } catch (Exception e) {
      ctx.status(400);
      return;
    }
    if (rawId == null || rawId.isEmpty()) {
      ctx.status(400);
      return;
    }

    String prefabPath = rawId;
    PrefabInspector.PrefabDimensions dims = prefabSpawner.getPrefabDimensions(prefabPath);

    Map<String, Object> size = new LinkedHashMap<>();
    size.put("w", dims.width);
    size.put("h", dims.depth);

    Map<String, Object> bounds = new LinkedHashMap<>();
    bounds.put("minX", dims.minX);
    bounds.put("maxX", dims.maxX);
    bounds.put("minY", dims.minY);
    bounds.put("maxY", dims.maxY);
    bounds.put("minZ", dims.minZ);
    bounds.put("maxZ", dims.maxZ);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("prefabPath", prefabPath);
    payload.put("roomSize", size);
    payload.put("bounds", bounds);

    String json = toJson(payload);
    ctx.contentType("application/json; charset=utf-8");
    ctx.result(json);
  }

  private void handleEventStream(@Nonnull SseClient sseClient) {
    Client client = new Client(sseClient);
    clients.add(client);
    log.info("[SSE] Client connected (%d total)", clients.size());
    client.send("connected", "{\"timestamp\":\"" + escapeJson(Instant.now().toString()) + "\"}");
    replayRecentEvents(client);
    sseClient.onClose(client::close);
  }

  private void serveDebugResource(@Nonnull Context ctx) {
    String path = ctx.path();
    if (path.startsWith("/api")) {
      ctx.status(404);
      return;
    }
    String resourcePath = resolveResourcePath(path);
    if (resourcePath == null) {
      ctx.status(404);
      return;
    }

    byte[] bytes = loadResource(resourcePath);
    if (bytes == null) {
      ctx.status(404);
      return;
    }

    ctx.contentType(resolveContentType(resourcePath));
    ctx.result(bytes);
  }

  private byte[] loadResource(@Nonnull String resourcePath) {
    try (var input = SseDebugServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (input == null) {
        return null;
      }
      return input.readAllBytes();
    } catch (IOException e) {
      log.error("[SSE] Failed to read resource %s: %s", resourcePath, e.getMessage());
      return null;
    }
  }

  private final class Client {
    private final SseClient client;
    private volatile boolean open = true;

    private Client(@Nonnull SseClient client) {
      this.client = client;
    }

    private boolean send(String event, String data) {
      if (!open || event == null || data == null) {
        return false;
      }
      try {
        client.sendEvent(event, data);
        return true;
      } catch (Exception e) {
        return false;
      }
    }

    private void close() {
      if (!open) {
        return;
      }
      open = false;
      clients.remove(this);
      log.info("[SSE] Client disconnected (%d total)", clients.size());
      try {
        client.close();
      } catch (Exception ignored) {
        // ignore
      }
    }
  }

  private void replayRecentEvents(@Nonnull Client client) {
    List<String> snapshot = new ArrayList<>();
    synchronized (recentEvents) {
      snapshot.addAll(recentEvents);
    }
    for (String message : snapshot) {
      if (!client.send("message", message)) {
        client.close();
        return;
      }
    }
  }
}
