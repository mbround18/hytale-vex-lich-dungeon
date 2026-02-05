package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;

/**
 * Dynamically discovers and categorizes prefabs from a ZIP asset bundle.
 * Loads prefabs from a ZIP file alongside the plugin JAR.
 */
public class PrefabDiscovery {

  private final @Nonnull LoggingHelper log;
  private final List<String> rooms = new ArrayList<>();
  private final List<String> hallways = new ArrayList<>();
  private final List<String> gates = new ArrayList<>();
  private final List<String> stitches = new ArrayList<>();
  private final List<String> dungeonPrefabs = new ArrayList<>();
  private final List<String> eventPrefabs = new ArrayList<>();
  private final @Nonnull ZipFile zipFile;
  private final @Nonnull Path assetsZipPath;
  private final @Nullable Path unpackedRoot;

  /**
   * Creates a new prefab discovery system that loads from a ZIP file.
   * 
   * @param log     Logger for discovery events
   * @param jarPath Path to the plugin JAR file (e.g., VexLichDungeon-0.1.0.jar)
   */
  public PrefabDiscovery(@Nonnull LoggingHelper log, @Nonnull Path jarPath) {
    this(log, jarPath, null);
  }

  public PrefabDiscovery(@Nonnull LoggingHelper log, @Nonnull Path jarPath, @Nullable Path unpackedRoot) {
    this.log = Objects.requireNonNull(log, "log");
    this.zipFile = Objects.requireNonNull(openAssetsZip(jarPath), "zipFile");
    this.assetsZipPath = Objects.requireNonNull(resolveAssetsZipPath(jarPath), "assetsZipPath");
    this.unpackedRoot = unpackedRoot;
    discoverPrefabs();
  }

  /**
   * Locates and opens the assets ZIP file.
   * Looks for a ZIP with the same base name as the JAR.
   * For example: VexLichDungeon-0.1.0.jar -> VexLichDungeon.zip
   */
  private ZipFile openAssetsZip(@Nonnull Path jarPath) {
    try {
      // Get the directory containing the JAR
      File jarFile = jarPath.toFile();
      File jarDir = jarFile.getParentFile();

      String jarName = jarFile.getName();
      String baseFull = jarName.endsWith(".jar")
          ? jarName.substring(0, jarName.length() - 4)
          : jarName;
      String versionedZip = baseFull + ".zip";
      File zipFile = new File(jarDir, versionedZip);
      if (!zipFile.exists()) {
        String legacyBase = baseFull.split("-")[0];
        String legacyZip = legacyBase + ".zip";
        File legacyFile = new File(jarDir, legacyZip);
        if (legacyFile.exists()) {
          log.warn("Assets ZIP not found at %s, falling back to %s", versionedZip, legacyZip);
          zipFile = legacyFile;
        } else {
          log.warn("Assets ZIP not found at %s; falling back to plugin JAR", versionedZip);
          zipFile = jarFile;
        }
      }

      log.info("Opening assets ZIP: %s", zipFile.getAbsolutePath());
      return new ZipFile(zipFile);

    } catch (Exception e) {
      log.error("Failed to open assets ZIP: %s", e.getMessage());
      throw new RuntimeException("Cannot load assets ZIP", e);
    }
  }

  private Path resolveAssetsZipPath(@Nonnull Path jarPath) {
    // Mirrors openAssetsZip selection logic for StitchIndexBuilder
    File jarFile = jarPath.toFile();
    File jarDir = jarFile.getParentFile();
    String jarName = jarFile.getName();
    String baseFull = jarName.endsWith(".jar")
        ? jarName.substring(0, jarName.length() - 4)
        : jarName;
    String versionedZip = baseFull + ".zip";
    File zipFile = new File(jarDir, versionedZip);
    if (!zipFile.exists()) {
      String legacyBase = baseFull.split("-")[0];
      String legacyZip = legacyBase + ".zip";
      File legacyFile = new File(jarDir, legacyZip);
      if (legacyFile.exists()) {
        zipFile = legacyFile;
      } else {
        zipFile = jarFile;
      }
    }
    return zipFile.toPath();
  }

  /**
   * Scans the ZIP file and discovers all available prefabs.
   */
  private void discoverPrefabs() {
    try {
      Set<String> seen = new HashSet<>();
      // Enumerate all entries in the ZIP
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String entryName = entry.getName();

        // Skip directories and non-prefab files
        if (entry.isDirectory() || !entryName.endsWith(".prefab.json")) {
          continue;
        }

        // Check if this is in Server/Prefabs/
        if (!entryName.startsWith("Server/Prefabs/")) {
          continue;
        }

        if (entryName.startsWith("Server/Prefabs/Gates/")) {
          String prefabPath = Objects.requireNonNull(toPrefabPath(entryName), "prefabPath");
          registerPrefab(prefabPath, PrefabCategory.GATE, PrefabSource.ZIP, seen);
          continue;
        }

        if (entryName.startsWith("Server/Prefabs/Stitch/")) {
          String prefabPath = Objects.requireNonNull(toPrefabPath(entryName), "prefabPath");
          registerPrefab(prefabPath, PrefabCategory.STITCH, PrefabSource.ZIP, seen);
          continue;
        }

        if (entryName.startsWith("Server/Prefabs/Dungeon/")) {
          String prefabPath = Objects.requireNonNull(toPrefabPath(entryName), "prefabPath");
          if (entryName.contains("/Hallways/")) {
            registerPrefab(prefabPath, PrefabCategory.HALLWAY, PrefabSource.ZIP, seen);
          } else if (entryName.contains("/Rooms/")) {
            registerPrefab(prefabPath, PrefabCategory.ROOM, PrefabSource.ZIP, seen);
          } else {
            registerPrefab(prefabPath, PrefabCategory.DUNGEON, PrefabSource.ZIP, seen);
          }
          continue;
        }

        if (entryName.startsWith("Server/Prefabs/Event/")) {
          String prefabPath = Objects.requireNonNull(toPrefabPath(entryName), "prefabPath");
          registerPrefab(prefabPath, PrefabCategory.EVENT, PrefabSource.ZIP, seen);
        }
      }

      discoverUnpackedPrefabs(seen);

      log.info(
          "Discovered %d dungeon prefabs (%d hallways, %d rooms), %d event prefabs, %d gates, %d stitches from ZIP",
          dungeonPrefabs.size(), hallways.size(), rooms.size(), eventPrefabs.size(), gates.size(), stitches.size());

      if (dungeonPrefabs.isEmpty()) {
        log.warn("No dungeon prefabs found under Server/Prefabs/Dungeon/");
      }
      if (eventPrefabs.isEmpty()) {
        log.warn("No event prefabs found under Server/Prefabs/Event/");
      }
      if (gates.isEmpty()) {
        log.warn("No gate prefabs found under Server/Prefabs/Gates/");
      }
      if (stitches.isEmpty()) {
        log.warn("No stitch prefabs found under Server/Prefabs/Stitch/");
      }

    } catch (Exception e) {
      log.error("Failed to discover prefabs from ZIP: %s", e.getMessage());
    }
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
  public void close() {
    if (zipFile != null) {
      try {
        zipFile.close();
      } catch (Exception e) {
        log.warn("Failed to close ZIP file: %s", e.getMessage());
      }
    }
  }

  /**
   * Gets the underlying ZipFile for direct access to prefab resources.
   * 
   * @return The ZipFile containing the prefabs
   */
  @Nonnull
  public ZipFile getZipFile() {
    return Objects.requireNonNull(zipFile, "zipFile");
  }

  @Nonnull
  public Path getAssetsZipPath() {
    return Objects.requireNonNull(assetsZipPath, "assetsZipPath");
  }
}
