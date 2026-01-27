package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.vexlichdungeon.dungeon.CardinalDirection;
import MBRound18.hytale.vexlichdungeon.logging.PluginLog;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Inspects prefab JSON files to determine their dimensions and optimal
 * placement.
 */
public class PrefabInspector {

  private final PluginLog log;
  private final ZipFile zipFile;

  /**
   * Creates a new prefab inspector.
   * 
   * @param log Logger for inspection events
   */
  public PrefabInspector(@Nonnull PluginLog log, @Nonnull ZipFile zipFile) {
    this.log = log;
    this.zipFile = zipFile;
  }

  /**
   * Data class for prefab dimensions.
   */
  public static class PrefabDimensions {
    public int minX, maxX, minY, maxY, minZ, maxZ;
    public int width; // X dimension
    public int depth; // Z dimension

    /**
     * Determine which dimension is larger (width or depth of the door).
     */
    public boolean isWiderInX() {
      return width >= depth;
    }

    @Override
    public String toString() {
      return String.format("Bounds: X[%d,%d] Y[%d,%d] Z[%d,%d] (Width=%d, Depth=%d)",
          minX, maxX, minY, maxY, minZ, maxZ, width, depth);
    }
  }

  /**
   * Loads a prefab JSON and analyzes its dimensions.
   * 
   * @param modRelativePath Path relative to Mods/VexLichDungeon/ (e.g.,
   *                        "Prefabs/Gates/MyGate.prefab")
   * @return PrefabDimensions with width and depth of the door
   */
  @Nonnull
  public PrefabDimensions getPrefabDimensions(@Nonnull String modRelativePath) {
    try {
      // Normalize path: strip leading "Prefabs/" and ensure .prefab.json suffix
      String normalizedPath = modRelativePath.startsWith("Prefabs/")
          ? modRelativePath.substring("Prefabs/".length())
          : modRelativePath;

      if (normalizedPath.endsWith(".prefab.json")) {
        // already correct
      } else if (normalizedPath.endsWith(".prefab")) {
        normalizedPath = normalizedPath + ".json";
      } else {
        normalizedPath = normalizedPath + ".prefab.json";
      }

      // Build path inside ZIP: Server/Prefabs/<path>.prefab.json
      String entryPath = "Server/Prefabs/" + normalizedPath;

      ZipEntry entry = zipFile.getEntry(entryPath);
      if (entry == null) {
        log.warn("Prefab file not found in ZIP: %s", entryPath);
        return createDefaultDimensions();
      }

      // Parse JSON directly from ZIP entry
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray blocks = json.getAsJsonArray("blocks");

        if (blocks == null || blocks.isEmpty()) {
          log.warn("Prefab %s has no blocks", modRelativePath);
          return createDefaultDimensions();
        }

        // Calculate bounds
        PrefabDimensions dims = new PrefabDimensions();
        dims.minX = Integer.MAX_VALUE;
        dims.maxX = Integer.MIN_VALUE;
        dims.minY = Integer.MAX_VALUE;
        dims.maxY = Integer.MIN_VALUE;
        dims.minZ = Integer.MAX_VALUE;
        dims.maxZ = Integer.MIN_VALUE;

        for (JsonElement elem : blocks) {
          JsonObject block = elem.getAsJsonObject();
          int x = block.get("x").getAsInt();
          int y = block.get("y").getAsInt();
          int z = block.get("z").getAsInt();

          dims.minX = Math.min(dims.minX, x);
          dims.maxX = Math.max(dims.maxX, x);
          dims.minY = Math.min(dims.minY, y);
          dims.maxY = Math.max(dims.maxY, y);
          dims.minZ = Math.min(dims.minZ, z);
          dims.maxZ = Math.max(dims.maxZ, z);
        }

        // Handle edge case: single block or no blocks parsed
        if (dims.minX == Integer.MAX_VALUE || dims.minY == Integer.MAX_VALUE || dims.minZ == Integer.MAX_VALUE) {
          log.warn("Prefab %s has invalid bounds", modRelativePath);
          return createDefaultDimensions();
        }

        dims.width = dims.maxX - dims.minX + 1;
        dims.depth = dims.maxZ - dims.minZ + 1;

        // Ensure dimensions are at least 1
        dims.width = Math.max(1, dims.width);
        dims.depth = Math.max(1, dims.depth);

        log.info("Inspected prefab %s: %s", modRelativePath, dims);
        return dims;

      }
    } catch (Exception e) {
      log.error("Failed to inspect prefab %s: %s", modRelativePath, e.getMessage());
      return createDefaultDimensions();
    }
  }

  /**
   * Calculates the optimal rotation and offset for a gate based on its
   * dimensions.
   * 
   * @param direction The direction the gate is facing (which wall it's on)
   * @param dims      The prefab dimensions
   * @return int array [rotationDegrees, offsetX, offsetZ] for gate placement
   */
  @Nonnull
  public int[] calculateGatePlacement(@Nonnull CardinalDirection direction, @Nonnull PrefabDimensions dims) {
    // Doors should be placed so:
    // - Wider dimension spans the room (19 blocks)
    // - Narrower dimension fills the 1-block gap

    int rotationDegrees = 0;
    int offsetX = 0;
    int offsetZ = 0;
    final int tileSize = 19;
    final int gapSize = 1;
    final int halfTile = tileSize / 2;
    final int edgeOffset = halfTile + gapSize;

    boolean doorWiderInX = dims.isWiderInX();

    switch (direction) {
      case EAST -> {
        // Door connects rooms along the X axis (east-west)
        // Need door to span N-S (Z dimension = wider)
        if (doorWiderInX) {
          // Door is wider in X, need to rotate 90°
          rotationDegrees = 90;
        } else {
          // Door is wider in Z, already correct
          rotationDegrees = 0;
        }
        offsetX = edgeOffset; // Place at east edge
        offsetZ = 0; // Center in Z (tile origin is centered)
      }
      case WEST -> {
        // Door connects rooms along the X axis (going west)
        if (doorWiderInX) {
          // Door is wider in X, rotate 270°
          rotationDegrees = 270;
        } else {
          // Door is wider in Z, rotate 180°
          rotationDegrees = 180;
        }
        offsetX = -edgeOffset; // Place at west edge
        offsetZ = 0; // Center in Z (tile origin is centered)
      }
      case SOUTH -> {
        // Door connects rooms along the Z axis (south-north)
        // Need door to span E-W (X dimension = wider)
        if (doorWiderInX) {
          // Door is wider in X, already correct
          rotationDegrees = 0;
        } else {
          // Door is wider in Z, rotate 90°
          rotationDegrees = 90;
        }
        offsetX = 0; // Center in X (tile origin is centered)
        offsetZ = edgeOffset; // Place at south edge
      }
      case NORTH -> {
        // Door connects rooms along the Z axis (going north)
        if (doorWiderInX) {
          // Door is wider in X, rotate 180°
          rotationDegrees = 180;
        } else {
          // Door is wider in Z, rotate 270°
          rotationDegrees = 270;
        }
        offsetX = 0; // Center in X (tile origin is centered)
        offsetZ = -edgeOffset; // Place at north edge
      }
    }

    log.info("Gate placement for %s: rotation=%d°, offset=(%d,%d), doorWiderInX=%s",
        direction, rotationDegrees, offsetX, offsetZ, doorWiderInX);

    return new int[] { rotationDegrees, offsetX, offsetZ };
  }

  /**
   * Creates default dimensions (9 wide x 6 deep - a common door size).
   */
  private PrefabDimensions createDefaultDimensions() {
    PrefabDimensions dims = new PrefabDimensions();
    dims.minX = 0;
    dims.maxX = 8;
    dims.minY = 0;
    dims.maxY = 7;
    dims.minZ = 0;
    dims.maxZ = 5;
    dims.width = 9;
    dims.depth = 6;
    return dims;
  }
}
