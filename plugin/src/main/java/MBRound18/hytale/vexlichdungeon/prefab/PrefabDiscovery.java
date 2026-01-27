package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.vexlichdungeon.logging.PluginLog;
import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;

/**
 * Dynamically discovers and categorizes prefabs from a ZIP asset bundle.
 * Loads prefabs from a ZIP file alongside the plugin JAR.
 */
public class PrefabDiscovery {

  private final PluginLog log;
  private final List<String> rooms = new ArrayList<>();
  private final List<String> hallways = new ArrayList<>();
  private final List<String> gates = new ArrayList<>();
  private final ZipFile zipFile;
  private final String zipPath;

  /**
   * Creates a new prefab discovery system that loads from a ZIP file.
   * 
   * @param log     Logger for discovery events
   * @param jarPath Path to the plugin JAR file (e.g., VexLichDungeon-0.1.0.jar)
   */
  public PrefabDiscovery(@Nonnull PluginLog log, @Nonnull Path jarPath) {
    this.log = log;
    this.zipPath = jarPath.toString();
    this.zipFile = openAssetsZip(jarPath);
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

      // Find the ZIP file - it should have the same base name
      // E.g., VexLichDungeon-0.1.0.jar -> VexLichDungeon.zip
      String jarName = jarFile.getName();
      String baseName = jarName.split("-")[0]; // Get "VexLichDungeon" from "VexLichDungeon-0.1.0.jar"
      String zipName = baseName + ".zip";

      File zipFile = new File(jarDir, zipName);
      if (!zipFile.exists()) {
        throw new RuntimeException(
            String.format(
                "Assets ZIP not found! Expected: %s (in directory: %s)",
                zipName, jarDir.getAbsolutePath()));
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
      if (zipFile == null) {
        log.error("ZIP file is null, cannot discover prefabs");
        return;
      }

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

        // Parse the path: Server/Prefabs/Gates/Name.prefab.json
        String[] parts = entryName.split("/");
        if (parts.length < 4) {
          continue; // Invalid structure
        }

        String category = parts[2]; // "Gates", "Hallways", or "Rooms"
        String fileName = parts[3]; // "Name.prefab.json"
        String name = fileName.replace(".prefab.json", "");
        // Don't include "Prefabs/" prefix - Hytale expects just "Category/Name"
        String prefabPath = String.format("%s/%s", category, name);

        // Add to appropriate category
        switch (category) {
          case "Gates":
            gates.add(prefabPath);
            break;
          case "Hallways":
            hallways.add(prefabPath);
            break;
          case "Rooms":
            rooms.add(prefabPath);
            break;
          default:
            // Ignore unknown categories
            break;
        }
      }

      log.info("Discovered %d gates, %d hallways, %d rooms from ZIP",
          gates.size(), hallways.size(), rooms.size());

      if (gates.isEmpty() || hallways.isEmpty() || rooms.isEmpty()) {
        log.warn("Missing prefab categories! Gates: %d, Hallways: %d, Rooms: %d",
            gates.size(), hallways.size(), rooms.size());
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
    if (rooms.isEmpty()) {
      log.warn("No rooms available!");
      return null;
    }
    return rooms.get(new Random().nextInt(rooms.size()));
  }

  /**
   * Gets a random hallway prefab.
   * 
   * @return Random hallway prefab path, or null if none available
   */
  public String getRandomHallway() {
    if (hallways.isEmpty()) {
      log.warn("No hallways available!");
      return null;
    }
    return hallways.get(new Random().nextInt(hallways.size()));
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
    return gates.get(new Random().nextInt(gates.size()));
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
    return zipFile;
  }
}
