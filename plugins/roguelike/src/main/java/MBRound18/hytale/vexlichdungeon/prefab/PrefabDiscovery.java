package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
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
  private final List<String> dungeonPrefabs = new ArrayList<>();
  private final List<String> eventPrefabs = new ArrayList<>();
  private final @Nonnull ZipFile zipFile;

  /**
   * Creates a new prefab discovery system that loads from a ZIP file.
   * 
   * @param log     Logger for discovery events
   * @param jarPath Path to the plugin JAR file (e.g., VexLichDungeon-0.1.0.jar)
   */
  public PrefabDiscovery(@Nonnull LoggingHelper log, @Nonnull Path jarPath) {
    this.log = Objects.requireNonNull(log, "log");
    this.zipFile = Objects.requireNonNull(openAssetsZip(jarPath), "zipFile");
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

  /**
   * Scans the ZIP file and discovers all available prefabs.
   */
  private void discoverPrefabs() {
    try {
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
          String prefabPath = toPrefabPath(entryName);
          gates.add(prefabPath);
          continue;
        }

        if (entryName.startsWith("Server/Prefabs/Dungeon/")) {
          String prefabPath = toPrefabPath(entryName);
          dungeonPrefabs.add(prefabPath);
          if (entryName.contains("/Hallways/")) {
            hallways.add(prefabPath);
          } else if (entryName.contains("/Rooms/")) {
            rooms.add(prefabPath);
          } else {
            rooms.add(prefabPath);
          }
          continue;
        }

        if (entryName.startsWith("Server/Prefabs/Event/")) {
          String prefabPath = toPrefabPath(entryName);
          eventPrefabs.add(prefabPath);
        }
      }

      log.info("Discovered %d dungeon prefabs (%d hallways, %d rooms), %d event prefabs, and %d gates from ZIP",
          dungeonPrefabs.size(), hallways.size(), rooms.size(), eventPrefabs.size(), gates.size());

      if (dungeonPrefabs.isEmpty()) {
        log.warn("No dungeon prefabs found under Server/Prefabs/Dungeon/");
      }
      if (eventPrefabs.isEmpty()) {
        log.warn("No event prefabs found under Server/Prefabs/Event/");
      }
      if (gates.isEmpty()) {
        log.warn("No gate prefabs found under Server/Prefabs/Gates/");
      }

    } catch (Exception e) {
      log.error("Failed to discover prefabs from ZIP: %s", e.getMessage());
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
}
