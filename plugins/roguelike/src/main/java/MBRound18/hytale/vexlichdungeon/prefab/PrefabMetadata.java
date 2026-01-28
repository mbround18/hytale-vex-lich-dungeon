package MBRound18.hytale.vexlichdungeon.prefab;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import MBRound18.PortalEngine.api.logging.EngineLog;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileReader;
import java.nio.file.Path;

/**
 * Reads metadata from prefab JSON files.
 * Extracts anchor point coordinates which define where the prefab "origin" is
 * relative to its blocks. This is critical because human-designed prefabs
 * don't always have their anchor at (0,0,0).
 */
public class PrefabMetadata {

  private final int anchorX;
  private final int anchorY;
  private final int anchorZ;
  private final int version;
  private final String prefabPath;

  private PrefabMetadata(
      int anchorX, int anchorY, int anchorZ,
      int version, String prefabPath) {
    this.anchorX = anchorX;
    this.anchorY = anchorY;
    this.anchorZ = anchorZ;
    this.version = version;
    this.prefabPath = prefabPath;
  }

  /**
   * Reads prefab metadata from a .prefab.json file.
   * 
   * @param prefabPath Path to the .prefab.json file
   * @param log        Logger for errors
   * @return PrefabMetadata object or null if reading fails
   */
  @Nullable
  public static PrefabMetadata fromPrefabFile(@Nonnull Path prefabPath, @Nonnull EngineLog log) {
    try (FileReader reader = new FileReader(prefabPath.toFile())) {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

      // Extract anchor coordinates
      int anchorX = root.has("anchorX") ? root.get("anchorX").getAsInt() : 0;
      int anchorY = root.has("anchorY") ? root.get("anchorY").getAsInt() : 0;
      int anchorZ = root.has("anchorZ") ? root.get("anchorZ").getAsInt() : 0;

      // Extract version
      int version = root.has("version") ? root.get("version").getAsInt() : 0;

      log.info("Read prefab metadata from %s: anchor=(%d, %d, %d), version=%d",
          prefabPath.getFileName(), anchorX, anchorY, anchorZ, version);

      return new PrefabMetadata(anchorX, anchorY, anchorZ, version, prefabPath.toString());

    } catch (Exception e) {
      log.error("Failed to read prefab metadata from %s: %s", prefabPath, e.getMessage());
      return null;
    }
  }

  /**
   * Gets the X offset that should be applied when placing this prefab.
   * The anchor point defines where the "origin" of the prefab is.
   * A positive anchor means blocks extend in the negative direction.
   */
  public int getAnchorX() {
    return anchorX;
  }

  /**
   * Gets the Y offset that should be applied when placing this prefab.
   */
  public int getAnchorY() {
    return anchorY;
  }

  /**
   * Gets the Z offset that should be applied when placing this prefab.
   */
  public int getAnchorZ() {
    return anchorZ;
  }

  public int getVersion() {
    return version;
  }

  public String getPrefabPath() {
    return prefabPath;
  }

  /**
   * Calculates the world position where this prefab should be placed
   * to align its anchor point with the target grid position.
   * 
   * @param targetX Target world X coordinate (where anchor should be)
   * @param targetY Target world Y coordinate (where anchor should be)
   * @param targetZ Target world Z coordinate (where anchor should be)
   * @return int[3] array with adjusted {x, y, z} coordinates for placement
   */
  public int[] calculatePlacementPosition(int targetX, int targetY, int targetZ) {
    return new int[] {
        targetX - anchorX,
        targetY - anchorY,
        targetZ - anchorZ
    };
  }

  @Override
  public String toString() {
    return String.format("PrefabMetadata{anchor=(%d, %d, %d), version=%d}",
        anchorX, anchorY, anchorZ, version);
  }
}
