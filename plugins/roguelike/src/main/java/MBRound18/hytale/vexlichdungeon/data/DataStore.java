package MBRound18.hytale.vexlichdungeon.data;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Manages persistent data storage for dungeon instances and configuration.
 * Uses JSON files in a dedicated plugin directory.
 */
public class DataStore {

  private final LoggingHelper log;
  private final Path dataDirectory;
  private final Gson gson;
  private DungeonConfig config;
  private SpawnPoolConfig spawnPoolConfig;
  private SpawnPool spawnPool;
  private final Map<String, DungeonInstanceData> instances;
  private final Map<UUID, PortalPlacementRecord> portalPlacements;

  public DataStore(@Nonnull LoggingHelper log, @Nonnull Path dataDirectory) {
    this.log = log;
    this.dataDirectory = dataDirectory;
    this.gson = new GsonBuilder().setPrettyPrinting().create();
    this.instances = new ConcurrentHashMap<>();
    this.portalPlacements = new ConcurrentHashMap<>();
  }

  /**
   * Initializes the data store - creates directories and loads data.
   */
  public void initialize() {
    try {
      // Create data directory if it doesn't exist
      if (!Files.exists(dataDirectory)) {
        Files.createDirectories(dataDirectory);
        log.info("Created data directory: %s", dataDirectory);
      }

      // Load or create config
      config = loadConfig();
      saveConfig(); // Save to ensure file exists with defaults

      // Load or create spawn pool
      spawnPoolConfig = loadSpawnPool();
      spawnPool = spawnPoolConfig.toSpawnPool();
      saveSpawnPool();

      // Load existing dungeon instances
      loadInstances();

      // Load portal placements
      loadPortalPlacements();

      log.info("Data store initialized with %d tracked instances and %d portal placements",
          instances.size(), portalPlacements.size());

    } catch (IOException e) {
      log.error("Failed to initialize data store: %s", e.getMessage());
      config = DungeonConfig.createDefault();
      spawnPoolConfig = SpawnPoolConfig.createDefault();
      spawnPool = spawnPoolConfig.toSpawnPool();
    }
  }

  private void loadPortalPlacements() throws IOException {
    Path portalsPath = dataDirectory.resolve("portals.db");

    if (Files.exists(portalsPath)) {
      try (InputStream inputStream = Files.newInputStream(portalsPath);
          ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
        Object loaded = objectInputStream.readObject();
        if (loaded instanceof Collection) {
          for (Object value : (Collection<?>) loaded) {
            if (value instanceof PortalPlacementRecord) {
              PortalPlacementRecord record = (PortalPlacementRecord) value;
              if (record.getPortalId() != null) {
                portalPlacements.put(record.getPortalId(), record);
              }
            }
          }
        }
        log.info("Loaded %d portal placements from %s", portalPlacements.size(), portalsPath);
      } catch (ClassNotFoundException e) {
        throw new IOException("Failed to deserialize portal placements", e);
      }
    }
  }

  public void savePortalPlacements() {
    try {
      Path portalsPath = dataDirectory.resolve("portals.db");
      try (OutputStream outputStream = Files.newOutputStream(portalsPath);
          ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
        objectOutputStream.writeObject(new ArrayList<>(portalPlacements.values()));
      }
      log.info("Saved %d portal placements to %s", portalPlacements.size(), portalsPath);
    } catch (IOException e) {
      log.error("Failed to save portal placements: %s", e.getMessage());
    }
  }

  public void recordPortalPlacement(@Nonnull PortalPlacementRecord record) {
    UUID portalId = record.getPortalId();
    if (portalId == null) {
      return;
    }
    portalPlacements.put(portalId, record);
    savePortalPlacements();
  }

  public void removePortalPlacement(@Nonnull UUID portalId) {
    if (portalPlacements.remove(portalId) != null) {
      savePortalPlacements();
    }
  }

  public Optional<PortalPlacementRecord> getPortalPlacement(@Nonnull UUID portalId) {
    return Optional.ofNullable(portalPlacements.get(portalId));
  }

  public boolean updatePortalExpiry(@Nonnull UUID portalId, long expiresAt) {
    PortalPlacementRecord record = portalPlacements.get(portalId);
    if (record == null) {
      return false;
    }
    record.setExpiresAt(expiresAt);
    savePortalPlacements();
    return true;
  }

  @Nonnull
  public Collection<PortalPlacementRecord> getPortalPlacements() {
    return Objects.requireNonNull(Collections.unmodifiableCollection(portalPlacements.values()),
        "portalPlacements");
  }

  public Optional<PortalPlacementRecord> findNearbyPortal(@Nonnull UUID worldUuid, int x, int z,
      int minDistance) {
    PortalPlacementRecord nearest = null;
    for (PortalPlacementRecord record : portalPlacements.values()) {
      if (record == null || record.getWorldUuid() == null) {
        continue;
      }
      UUID portalId = record.getPortalId();
      if (portalId == null) {
        continue;
      }
      if (record.getExpiresAt() > 0 && record.getExpiresAt() <= System.currentTimeMillis()) {
        continue;
      }
      if (!worldUuid.equals(record.getWorldUuid())) {
        continue;
      }
      int dx = Math.abs(record.getX() - x);
      int dz = Math.abs(record.getZ() - z);
      if (dx <= minDistance && dz <= minDistance) {
        nearest = record;
        break;
      }
    }
    if (nearest == null) {
      return Optional.empty();
    }
    return Optional.of(nearest);
  }

  /**
   * Loads configuration from config.json or creates default.
   */
  @Nonnull
  private DungeonConfig loadConfig() throws IOException {
    Path configPath = dataDirectory.resolve("config.json");

    if (Files.exists(configPath)) {
      try (Reader reader = Files.newBufferedReader(configPath)) {
        DungeonConfig loaded = gson.fromJson(reader, DungeonConfig.class);
        log.info("Loaded configuration from %s", configPath);
        return loaded != null ? loaded : DungeonConfig.createDefault();
      }
    }

    log.info("No existing config found, using defaults");
    return DungeonConfig.createDefault();
  }

  /**
   * Loads spawn pool configuration from spawn_pool.json or creates default.
   */
  @Nonnull
  private SpawnPoolConfig loadSpawnPool() throws IOException {
    Path spawnPoolPath = dataDirectory.resolve("spawn_pool.json");

    if (Files.exists(spawnPoolPath)) {
      try (Reader reader = Files.newBufferedReader(spawnPoolPath)) {
        JsonElement root = Objects.requireNonNull(JsonParser.parseReader(reader), "root");
        SpawnPoolConfig loaded = parseSpawnPoolConfig(root);
        log.info("Loaded spawn pool from %s", spawnPoolPath);
        return loaded != null ? loaded : SpawnPoolConfig.createDefault();
      }
    }

    log.info("No existing spawn_pool.json found, using defaults");
    return SpawnPoolConfig.createDefault();
  }

  @Nonnull
  private SpawnPoolConfig parseSpawnPoolConfig(@Nonnull JsonElement root) {
    SpawnPoolConfig config = new SpawnPoolConfig();
    if (!root.isJsonObject()) {
      return SpawnPoolConfig.createDefault();
    }

    JsonObject obj = root.getAsJsonObject();
    JsonElement rangesElement = obj.has("ranges") ? obj.get("ranges") : root;

    java.lang.reflect.Type mapType = new TypeToken<Map<String, List<SpawnPoolEntry>>>() {
    }.getType();
    Map<String, List<SpawnPoolEntry>> ranges = gson.fromJson(rangesElement, mapType);
    if (ranges != null) {
      config.setRanges(ranges);
    }
    return config;
  }

  /**
   * Saves current configuration to config.json.
   */
  public void saveConfig() {
    try {
      Path configPath = dataDirectory.resolve("config.json");
      try (Writer writer = Files.newBufferedWriter(configPath)) {
        gson.toJson(config, writer);
      }
      log.info("Saved configuration to %s", configPath);
    } catch (IOException e) {
      log.error("Failed to save config: %s", e.getMessage());
    }
  }

  /**
   * Saves spawn pool configuration to spawn_pool.json.
   */
  public void saveSpawnPool() {
    try {
      Path spawnPoolPath = dataDirectory.resolve("spawn_pool.json");
      try (Writer writer = Files.newBufferedWriter(spawnPoolPath)) {
        gson.toJson(spawnPoolConfig.getRanges(), writer);
      }
      log.info("Saved spawn pool to %s", spawnPoolPath);
    } catch (IOException e) {
      log.error("Failed to save spawn pool: %s", e.getMessage());
    }
  }

  /**
   * Loads all dungeon instance data from dungeons.json.
   */
  private void loadInstances() throws IOException {
    Path instancesPath = dataDirectory.resolve("dungeons.json");

    if (Files.exists(instancesPath)) {
      try (Reader reader = Files.newBufferedReader(instancesPath)) {
        DungeonInstanceData[] loaded = gson.fromJson(reader, DungeonInstanceData[].class);
        if (loaded != null) {
          for (DungeonInstanceData instance : loaded) {
            instances.put(instance.getWorldName(), instance);
          }
        }
        log.info("Loaded %d dungeon instances from %s", instances.size(), instancesPath);
      }
    }
  }

  /**
   * Saves all dungeon instance data to dungeons.json.
   */
  public void saveInstances() {
    try {
      Path instancesPath = dataDirectory.resolve("dungeons.json");
      try (Writer writer = Files.newBufferedWriter(instancesPath)) {
        gson.toJson(instances.values(), writer);
      }
      log.info("Saved %d dungeon instances to %s", instances.size(), instancesPath);
    } catch (IOException e) {
      log.error("Failed to save instances: %s", e.getMessage());
    }
  }

  /**
   * Gets the current configuration.
   */
  @Nonnull
  public DungeonConfig getConfig() {
    return Objects.requireNonNull(config, "config");
  }

  @Nonnull
  public Path getDataDirectory() {
    return Objects.requireNonNull(dataDirectory, "dataDirectory");
  }

  @Nonnull
  public SpawnPool getSpawnPool() {
    return Objects.requireNonNull(spawnPool, "spawnPool");
  }

  /**
   * Checks if a dungeon instance has been generated.
   */
  public boolean isGenerated(@Nonnull String worldName) {
    DungeonInstanceData data = instances.get(worldName);
    return data != null && data.isGenerated();
  }

  /**
   * Marks a dungeon instance as generated.
   */
  public void markGenerated(@Nonnull String worldName, long seed, int tileCount) {
    DungeonInstanceData data = DungeonInstanceData.create(worldName, seed, tileCount);
    instances.put(worldName, data);
    saveInstances();
  }

  @Nonnull
  @SuppressWarnings("null")
  public DungeonInstanceData getOrCreateInstance(@Nonnull String worldName) {
    return instances.computeIfAbsent(worldName, name -> {
      DungeonInstanceData data = DungeonInstanceData.create(
          Objects.requireNonNull(name, "worldName"), System.currentTimeMillis(), 0);
      data.setGenerated(true);
      data.setGeneratedTimestamp(System.currentTimeMillis());
      return Objects.requireNonNull(data, "instance");
    });
  }

  /**
   * Gets data for a specific dungeon instance.
   */
  public Optional<DungeonInstanceData> getInstance(@Nonnull String worldName) {
    return Optional.ofNullable(instances.get(worldName));
  }

  public void updateCurrentPlayers(@Nonnull String worldName, @Nonnull Map<String, String> playersByUuid) {
    DungeonInstanceData data = getOrCreateInstance(worldName);
    boolean changed = false;
    long now = System.currentTimeMillis();

    Set<String> currentUuids = new HashSet<>(playersByUuid.keySet());
    List<String> currentList = new ArrayList<>(currentUuids);
    Collections.sort(currentList);
    if (!new HashSet<>(data.getCurrentPlayers()).equals(currentUuids)) {
      data.setCurrentPlayers(currentList);
      changed = true;
    }

    Set<String> seen = new HashSet<>(data.getPlayersSeen());
    for (Map.Entry<String, String> entry : playersByUuid.entrySet()) {
      String uuid = entry.getKey();
      String name = entry.getValue();
      if (!seen.contains(uuid)) {
        data.getPlayersSeen().add(uuid);
        seen.add(uuid);
        changed = true;
      }

      DungeonInstanceData.PlayerProgress progress = data.getPlayerProgress().computeIfAbsent(uuid, id -> {
        DungeonInstanceData.PlayerProgress created = new DungeonInstanceData.PlayerProgress();
        created.setPlayerUuid(id);
        created.setStartTime(now);
        return created;
      });

      if (!Objects.equals(progress.getPlayerName(), name)) {
        progress.setPlayerName(name);
        changed = true;
      }

      if (!progress.isCurrentlyInInstance()) {
        progress.setCurrentlyInInstance(true);
        changed = true;
      }

      progress.setLastSeen(now);
    }

    for (DungeonInstanceData.PlayerProgress progress : data.getPlayerProgress().values()) {
      if (!currentUuids.contains(progress.getPlayerUuid()) && progress.isCurrentlyInInstance()) {
        progress.setCurrentlyInInstance(false);
        changed = true;
      }
    }

    if (changed) {
      saveInstances();
    }
  }

  public void clearCurrentPlayers(@Nonnull String worldName) {
    DungeonInstanceData data = getOrCreateInstance(worldName);
    boolean changed = false;
    if (!data.getCurrentPlayers().isEmpty()) {
      data.setCurrentPlayers(new ArrayList<>());
      changed = true;
    }

    for (DungeonInstanceData.PlayerProgress progress : data.getPlayerProgress().values()) {
      if (progress.isCurrentlyInInstance()) {
        progress.setCurrentlyInInstance(false);
        changed = true;
      }
    }

    if (changed) {
      saveInstances();
    }
  }

  public void recordKill(@Nonnull String worldName, String playerUuid, int points) {
    DungeonInstanceData data = getOrCreateInstance(worldName);
    data.setTotalKills(data.getTotalKills() + 1);
    data.setTotalScore(data.getTotalScore() + Math.max(0, points));

    if (playerUuid != null) {
      DungeonInstanceData.PlayerProgress progress = data.getPlayerProgress().computeIfAbsent(playerUuid, id -> {
        DungeonInstanceData.PlayerProgress created = new DungeonInstanceData.PlayerProgress();
        created.setPlayerUuid(id);
        created.setStartTime(System.currentTimeMillis());
        return created;
      });
      progress.setEnemiesKilled(progress.getEnemiesKilled() + 1);
      progress.setScore(progress.getScore() + Math.max(0, points));
      progress.setLastSeen(System.currentTimeMillis());
    }

    saveInstances();
  }

  public void recordRoomCleared(@Nonnull String worldName) {
    DungeonInstanceData data = getOrCreateInstance(worldName);
    data.setRoomsCleared(data.getRoomsCleared() + 1);
    data.setRoomsClearedThisRound(data.getRoomsClearedThisRound() + 1);
    saveInstances();
  }

  public void recordSafeRoomVisit(@Nonnull String worldName) {
    DungeonInstanceData data = getOrCreateInstance(worldName);
    data.setSafeRoomsVisited(data.getSafeRoomsVisited() + 1);
    data.setRoundsCleared(data.getRoundsCleared() + 1);
    data.setRoomsClearedThisRound(0);
    saveInstances();
  }

  public void applyRunSummary(@Nonnull String worldName, @Nonnull MBRound18.ImmortalEngine.api.RunSummary summary) {
    DungeonInstanceData data = getOrCreateInstance(worldName);
    data.setTotalScore(summary.getTotalScore());
    data.setTotalKills(summary.getTotalKills());
    data.setRoomsCleared(summary.getRoomsCleared());
    data.setRoundsCleared(summary.getRoundsCleared());
    data.setSafeRoomsVisited(summary.getSafeRoomsVisited());

    for (MBRound18.ImmortalEngine.api.RunSummary.PlayerSummary player : summary.getPlayers()) {
      String playerId = player.getPlayerId();
      if (playerId == null) {
        continue;
      }
      DungeonInstanceData.PlayerProgress progress = data.getPlayerProgress().computeIfAbsent(playerId, id -> {
        DungeonInstanceData.PlayerProgress created = new DungeonInstanceData.PlayerProgress();
        created.setPlayerUuid(id);
        created.setStartTime(System.currentTimeMillis());
        return created;
      });
      progress.setPlayerName(player.getDisplayName());
      progress.setEnemiesKilled(player.getKills());
      progress.setScore(player.getScore());
      progress.setLastSeen(System.currentTimeMillis());
    }

    saveInstances();
  }

  /**
   * Gets all tracked dungeon instances.
   */
  @Nonnull
  public Collection<DungeonInstanceData> getAllInstances() {
    return Objects.requireNonNull(Collections.unmodifiableCollection(instances.values()), "instances");
  }

  /**
   * Clears all instance data (for cleanup/reset).
   */
  public void clearAllInstances() {
    instances.clear();
    saveInstances();
    log.info("Cleared all dungeon instance data");
  }

  /**
   * Removes specific dungeon instances by world name.
   */
  public void removeInstances(@Nonnull Collection<String> worldNames) {
    boolean changed = false;
    for (String worldName : worldNames) {
      if (instances.remove(worldName) != null) {
        changed = true;
      }
    }
    if (changed) {
      saveInstances();
      log.info("Removed %d dungeon instance(s)", worldNames.size());
    }
  }
}
