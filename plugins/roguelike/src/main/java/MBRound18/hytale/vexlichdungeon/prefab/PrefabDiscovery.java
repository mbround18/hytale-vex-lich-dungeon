package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;
import com.hypixel.hytale.server.core.prefab.PrefabStore;

/**
 * Dynamically discovers and categorizes prefabs from the server asset store.
 * Uses PrefabStore paths instead of ZIP inspection.
 */
public class PrefabDiscovery {

  private final @Nonnull LoggingHelper log;
  private final List<String> prefabPrefixAllowList;
  private final List<String> rooms = new ArrayList<>();
  private final List<String> hallways = new ArrayList<>();
  private final List<String> gates = new ArrayList<>();
  private final List<String> stitches = new ArrayList<>();
  private final List<String> dungeonPrefabs = new ArrayList<>();
  private final List<String> eventPrefabs = new ArrayList<>();
  private final @Nullable Path unpackedRoot;

  /**
   * Creates a new prefab discovery system that loads from the server asset store.
   * 
   * @param log Logger for discovery events
   */
  public PrefabDiscovery(@Nonnull LoggingHelper log) {
    this(log, null, java.util.List.of("Vex_"));
  }

  public PrefabDiscovery(@Nonnull LoggingHelper log, @Nullable Path unpackedRoot) {
    this(log, unpackedRoot, java.util.List.of("Vex_"));
  }

  public PrefabDiscovery(@Nonnull LoggingHelper log, @Nullable Path unpackedRoot,
      @Nonnull List<String> prefabPrefixAllowList) {
    this.log = Objects.requireNonNull(log, "log");
    this.unpackedRoot = unpackedRoot;
    this.prefabPrefixAllowList = new ArrayList<>(
        Objects.requireNonNull(prefabPrefixAllowList, "prefabPrefixAllowList"));
    discoverPrefabs();
  }

  public synchronized void refresh() {
    rooms.clear();
    hallways.clear();
    gates.clear();
    stitches.clear();
    dungeonPrefabs.clear();
    eventPrefabs.clear();
    log.fine("[PREFABS] Refresh requested. unpackedRoot=%s", unpackedRoot);
    discoverPrefabs();
    log.info(
        "[PREFABS] Discovered: %d rooms, %d hallways, %d events, %d gates, %d stitches",
        rooms.size(), hallways.size(), eventPrefabs.size(), gates.size(), stitches.size());
    if (!rooms.isEmpty()) {
      log.fine("[PREFABS] Rooms: %s", rooms);
    }
    if (!hallways.isEmpty()) {
      log.fine("[PREFABS] Hallways: %s", hallways);
    }
    if (!eventPrefabs.isEmpty()) {
      log.fine("[PREFABS] Events: %s", eventPrefabs);
    }
  }

  /**
   * Scans the server asset store and discovers all available prefabs.
   */
  private void discoverPrefabs() {
    try {
      Set<String> seen = new HashSet<>();
      List<Path> roots = resolvePrefabRoots();
      log.fine("[PREFABS] Resolved prefab roots: %s", roots);
      for (Path root : roots) {
        if (root == null || !Files.exists(root)) {
          continue;
        }
        Files.walk(root)
            .filter(path -> path != null && Files.isRegularFile(path))
            .filter(path -> path.toString().endsWith(".prefab.json"))
            .forEach(path -> {
              String rel = root.relativize(path).toString().replace('\\', '/');
              String entryName = "Server/Prefabs/" + rel;
              if (!entryName.startsWith("Server/Prefabs/")) {
                return;
              }

              if (entryName.startsWith("Server/Prefabs/Gates/")) {
                String prefabPath = Objects.requireNonNull(toPrefabPath(entryName), "prefabPath");
                registerPrefab(prefabPath, PrefabCategory.GATE, PrefabSource.ASSET, seen);
                return;
              }

              if (entryName.startsWith("Server/Prefabs/Stitch/")) {
                String prefabPath = Objects.requireNonNull(toPrefabPath(entryName), "prefabPath");
                registerPrefab(prefabPath, PrefabCategory.STITCH, PrefabSource.ASSET, seen);
                return;
              }

              if (entryName.startsWith("Server/Prefabs/Dungeon/")) {
                String prefabPath = Objects.requireNonNull(toPrefabPath(entryName), "prefabPath");
                if (entryName.contains("/Hallways/")) {
                  registerPrefab(prefabPath, PrefabCategory.HALLWAY, PrefabSource.ASSET, seen);
                } else if (entryName.contains("/Rooms/")) {
                  registerPrefab(prefabPath, PrefabCategory.ROOM, PrefabSource.ASSET, seen);
                } else {
                  registerPrefab(prefabPath, PrefabCategory.DUNGEON, PrefabSource.ASSET, seen);
                }
                return;
              }

              if (entryName.startsWith("Server/Prefabs/Event/")) {
                String prefabPath = Objects.requireNonNull(toPrefabPath(entryName), "prefabPath");
                registerPrefab(prefabPath, PrefabCategory.EVENT, PrefabSource.ASSET, seen);
              }
            });
      }

      discoverUnpackedPrefabs(seen);

      log.fine(
          "Discovered %d dungeon prefabs (%d hallways, %d rooms), %d event prefabs, %d gates, %d stitches from asset store",
          dungeonPrefabs.size(), hallways.size(), rooms.size(), eventPrefabs.size(), gates.size(), stitches.size());

      if (dungeonPrefabs.isEmpty()) {
        log.fine("No dungeon prefabs found under Server/Prefabs/Dungeon/");
      }
      if (eventPrefabs.isEmpty()) {
        log.fine("No event prefabs found under Server/Prefabs/Event/");
      }
      if (gates.isEmpty()) {
        log.fine("No gate prefabs found under Server/Prefabs/Gates/");
      }
      if (stitches.isEmpty()) {
        log.fine("No stitch prefabs found under Server/Prefabs/Stitch/");
      }

    } catch (Exception e) {
      log.error("Failed to discover prefabs from asset store: %s", e.getMessage());
    }
  }

  @Nonnull
  private List<Path> resolvePrefabRoots() {
    List<Path> roots = new ArrayList<>();
    try {
      PrefabStore store = PrefabStore.get();
      Path baseRoot = store.getAssetPrefabsPath();
      if (baseRoot != null) {
        roots.add(baseRoot);
      }
      for (PrefabStore.AssetPackPrefabPath packPath : store.getAllAssetPrefabPaths()) {
        if (packPath == null) {
          continue;
        }
        Path prefabsPath = packPath.prefabsPath();
        if (prefabsPath != null) {
          roots.add(prefabsPath);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to resolve prefab roots from PrefabStore: %s", e.getMessage());
    }
    LinkedHashSet<Path> unique = new LinkedHashSet<>();
    for (Path root : roots) {
      if (root != null) {
        unique.add(root);
      }
    }
    return new ArrayList<>(unique);
  }

  private void discoverUnpackedPrefabs(@Nonnull Set<String> seen) {
    if (unpackedRoot == null) {
      return;
    }
    try {
      Path prefabRoot = unpackedRoot.resolve("Server").resolve("Prefabs");
      if (!Files.exists(prefabRoot)) {
        return;
      }
      Files.walk(prefabRoot)
          .filter(path -> path != null && Files.isRegularFile(path))
          .filter(path -> path.toString().endsWith(".prefab.json"))
          .forEach(path -> {
            String rel = prefabRoot.relativize(path).toString().replace('\\', '/');
            String entryName = "Server/Prefabs/" + rel;
            String prefabPath = Objects.requireNonNull(toPrefabPath(entryName), "prefabPath");
            if (entryName.startsWith("Server/Prefabs/Gates/")) {
              registerPrefab(prefabPath, PrefabCategory.GATE, PrefabSource.UNPACKED, seen);
              return;
            }
            if (entryName.startsWith("Server/Prefabs/Stitch/")) {
              registerPrefab(prefabPath, PrefabCategory.STITCH, PrefabSource.UNPACKED, seen);
              return;
            }
            if (entryName.startsWith("Server/Prefabs/Dungeon/")) {
              if (entryName.contains("/Hallways/")) {
                registerPrefab(prefabPath, PrefabCategory.HALLWAY, PrefabSource.UNPACKED, seen);
              } else if (entryName.contains("/Rooms/")) {
                registerPrefab(prefabPath, PrefabCategory.ROOM, PrefabSource.UNPACKED, seen);
              } else {
                registerPrefab(prefabPath, PrefabCategory.DUNGEON, PrefabSource.UNPACKED, seen);
              }
              return;
            }
            if (entryName.startsWith("Server/Prefabs/Event/")) {
              registerPrefab(prefabPath, PrefabCategory.EVENT, PrefabSource.UNPACKED, seen);
            }
          });
    } catch (Exception e) {
      log.warn("Failed to scan unpacked prefabs: %s", e.getMessage());
    }
  }

  private void registerPrefab(@Nonnull String prefabPath, @Nonnull PrefabCategory category,
      @Nonnull PrefabSource source, @Nonnull Set<String> seen) {
    if (!seen.add(prefabPath)) {
      return;
    }
    if (!isAllowedPrefab(prefabPath)) {
      return;
    }
    switch (category) {
      case GATE -> gates.add(prefabPath);
      case STITCH -> stitches.add(prefabPath);
      case HALLWAY -> {
        hallways.add(prefabPath);
        dungeonPrefabs.add(prefabPath);
      }
      case ROOM -> {
        rooms.add(prefabPath);
        dungeonPrefabs.add(prefabPath);
      }
      case EVENT -> eventPrefabs.add(prefabPath);
      case DUNGEON, UNKNOWN -> dungeonPrefabs.add(prefabPath);
    }
    PrefabDiscovered discovered = new PrefabDiscovered(prefabPath, category, source);
    for (PrefabHook hook : PrefabHookRegistry.getHooks()) {
      hook.onPrefabDiscovered(discovered);
    }
  }

  private boolean isAllowedPrefab(@Nonnull String prefabPath) {
    String name = prefabPath;
    int slash = prefabPath.lastIndexOf('/');
    if (slash >= 0 && slash < prefabPath.length() - 1) {
      name = prefabPath.substring(slash + 1);
    }
    if (prefabPrefixAllowList.isEmpty()) {
      return true;
    }
    for (String prefix : prefabPrefixAllowList) {
      if (prefix == null || prefix.isBlank()) {
        continue;
      }
      if (name.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets a random room prefab.
   * 
   * @return Random room prefab path, or null if none available
   */
  public String getRandomRoom() {
    List<String> source = rooms.isEmpty() ? dungeonPrefabs : rooms;
    if (source.isEmpty()) {
      log.warn("No dungeon prefabs available!");
      return null;
    }
    return source.get(new Random().nextInt(source.size()));
  }

  /**
   * Gets a random hallway prefab.
   * 
   * @return Random hallway prefab path, or null if none available
   */
  public String getRandomHallway() {
    List<String> source = hallways.isEmpty() ? dungeonPrefabs : hallways;
    if (source.isEmpty()) {
      log.warn("No dungeon prefabs available!");
      return null;
    }
    return source.get(new Random().nextInt(source.size()));
  }

  /**
   * Gets a random gate prefab.
   * 
   * @return Random gate prefab path, or null if none available
   */
  public String getRandomGate() {
    if (gates.isEmpty()) {
      log.warn("No gates available!");
      return null;
    }
    List<String> filtered = gates.stream()
        .filter(g -> !g.contains("Blocked") && !g.contains("Clear"))
        .collect(Collectors.toList());
    if (filtered.isEmpty()) {
      return null;
    }
    return filtered.get(new Random().nextInt(filtered.size()));
  }

  /**
   * Gets the blocked gate prefab.
   * 
   * @return Blocked gate prefab path, or null if not found
   */
  public String getBlockedGate() {
    return gates.stream()
        .filter(g -> g.contains("Blocked"))
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets all available rooms.
   */
  public List<String> getAllRooms() {
    return new ArrayList<>(rooms);
  }

  /**
   * Gets all available hallways.
   */
  public List<String> getAllHallways() {
    return new ArrayList<>(hallways);
  }

  /**
   * Gets all available gates.
   */
  public List<String> getAllGates() {
    return new ArrayList<>(gates);
  }

  /**
   * Gets all available stitch prefabs (recursive under Server/Prefabs/Stitch).
   */
  public List<String> getAllStitchPrefabs() {
    return new ArrayList<>(stitches);
  }

  /**
   * Gets all available dungeon prefabs (recursive under Server/Prefabs/Dungeon).
   */
  public List<String> getAllDungeonPrefabs() {
    return new ArrayList<>(dungeonPrefabs);
  }

  /**
   * Gets all available event prefabs (recursive under Server/Prefabs/Event).
   */
  public List<String> getAllEventPrefabs() {
    return new ArrayList<>(eventPrefabs);
  }

  public List<String> getAllPrefabs() {
    LinkedHashSet<String> all = new LinkedHashSet<>();
    all.addAll(dungeonPrefabs);
    all.addAll(rooms);
    all.addAll(hallways);
    all.addAll(gates);
    all.addAll(stitches);
    all.addAll(eventPrefabs);
    return new ArrayList<>(all);
  }

  public List<String> getPrefabsByPathPrefix(@Nonnull String prefix) {
    String needle = Objects.requireNonNull(prefix, "prefix");
    return getAllPrefabs().stream()
        .filter(path -> path != null && path.startsWith(needle))
        .collect(Collectors.toList());
  }

  private String toPrefabPath(String entryName) {
    String trimmed = entryName.substring("Server/Prefabs/".length());
    if (trimmed.endsWith(".prefab.json")) {
      trimmed = trimmed.substring(0, trimmed.length() - ".prefab.json".length());
    }
    return trimmed;
  }

  /**
   * Closes the ZIP file when done.
   */
  @Nullable
  public Path getUnpackedRoot() {
    return unpackedRoot;
  }
}
