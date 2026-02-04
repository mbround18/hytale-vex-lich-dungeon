package MBRound18.hytale.vexlichdungeon.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileReader;
import java.nio.file.Path;

/**
 * Reads metadata from instance config.json files.
 * Extracts spawn point coordinates and other instance configuration.
 */
public class InstanceMetadata {

  private final double spawnX;
  private final double spawnY;
  private final double spawnZ;
  private final double spawnYaw;
  private final double spawnPitch;
  private final String displayName;
  private final String worldStructure;

  private InstanceMetadata(
      double spawnX, double spawnY, double spawnZ,
      double spawnYaw, double spawnPitch,
      String displayName, String worldStructure) {
    this.spawnX = spawnX;
    this.spawnY = spawnY;
    this.spawnZ = spawnZ;
    this.spawnYaw = spawnYaw;
    this.spawnPitch = spawnPitch;
    this.displayName = displayName;
    this.worldStructure = worldStructure;
  }

  /**
   * Reads instance metadata from a config.json file.
   * 
   * @param configPath Path to the config.json file
   * @param log        Logger for errors
   * @return InstanceMetadata object or null if reading fails
   */
  @Nullable
  public static InstanceMetadata fromConfigFile(@Nonnull Path configPath, @Nonnull LoggingHelper log) {
    try (FileReader reader = new FileReader(configPath.toFile())) {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

      // Extract spawn point from SpawnProvider.SpawnPoint
      JsonObject spawnProvider = root.getAsJsonObject("SpawnProvider");
      if (spawnProvider == null) {
        log.error("No SpawnProvider found in config.json");
        return null;
      }

      JsonObject spawnPoint = spawnProvider.getAsJsonObject("SpawnPoint");
      if (spawnPoint == null) {
        log.error("No SpawnPoint found in SpawnProvider");
        return null;
      }

      double x = spawnPoint.get("X").getAsDouble();
      double y = spawnPoint.get("Y").getAsDouble();
      double z = spawnPoint.get("Z").getAsDouble();
      double yaw = spawnPoint.has("Yaw") ? spawnPoint.get("Yaw").getAsDouble() : 0.0;
      double pitch = spawnPoint.has("Pitch") ? spawnPoint.get("Pitch").getAsDouble() : 0.0;

      // Extract display name
      String displayName = root.has("DisplayName") ? root.get("DisplayName").getAsString() : "Unknown";

      // Extract world structure
      String worldStructure = "Unknown";
      if (root.has("WorldGen")) {
        JsonObject worldGen = root.getAsJsonObject("WorldGen");
        if (worldGen.has("WorldStructure")) {
          worldStructure = worldGen.get("WorldStructure").getAsString();
        }
      }

      log.info("Read instance metadata: %s at spawn (%.1f, %.1f, %.1f)", displayName, x, y, z);
      return new InstanceMetadata(x, y, z, yaw, pitch, displayName, worldStructure);

    } catch (Exception e) {
      log.error("Failed to read instance metadata from %s: %s", configPath, e.getMessage());
      return null;
    }
  }

  public double getSpawnX() {
    return spawnX;
  }

  public double getSpawnY() {
    return spawnY;
  }

  public double getSpawnZ() {
    return spawnZ;
  }

  public double getSpawnYaw() {
    return spawnYaw;
  }

  public double getSpawnPitch() {
    return spawnPitch;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getWorldStructure() {
    return worldStructure;
  }

  @Override
  public String toString() {
    return String.format("InstanceMetadata{name='%s', spawn=(%.1f, %.1f, %.1f)}",
        displayName, spawnX, spawnY, spawnZ);
  }
}
