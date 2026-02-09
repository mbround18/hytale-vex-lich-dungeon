package MBRound18.ImmortalEngine.api.prefab;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Inspects prefab JSON files to determine their dimensions and optimal
 * placement.
 */
public class PrefabInspector {

  private final @Nonnull LoggingHelper log;
  private final @Nullable Path unpackedRoot;
  private final Map<String, PrefabDimensions> dimensionsCache;

  /**
   * Creates a new prefab inspector.
   *
   * @param log Logger for inspection events
   */
  public PrefabInspector(@Nonnull LoggingHelper log, @Nullable Path unpackedRoot) {
    this.log = Objects.requireNonNull(log, "log");
    this.unpackedRoot = unpackedRoot;
    this.dimensionsCache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, PrefabDimensions> eldest) {
        return size() > 256;
      }
    });
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
   * @param modRelativePath Path relative to Server/Prefabs/ (e.g.,
   *                        "Dungeon/Rooms/Vex_Room_S_Lava_B")
   * @return PrefabDimensions with width and depth of the door
   */
  @Nonnull
  public PrefabDimensions getPrefabDimensions(@Nonnull String modRelativePath) {
    try {
      PrefabDimensions cached = dimensionsCache.get(modRelativePath);
      if (cached != null) {
        return cached;
      }

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

      String entryPath = "Server/Prefabs/" + normalizedPath;
      Path assetPath = resolveAssetPrefab(entryPath);
      if (assetPath != null && Files.exists(assetPath)) {
        try (BufferedReader reader = Files.newBufferedReader(assetPath, StandardCharsets.UTF_8)) {
          return parseDimensions(modRelativePath, Objects.requireNonNull(reader, "reader"));
        }
      }

      Path unpackedFile = resolveUnpackedPrefab(entryPath);
      if (unpackedFile != null && Files.exists(unpackedFile)) {
        try (BufferedReader reader = Files.newBufferedReader(unpackedFile, StandardCharsets.UTF_8)) {
          return parseDimensions(modRelativePath, Objects.requireNonNull(reader, "reader"));
        }
      }

      log.fine("Prefab file not found in assets or unpacked path: %s", entryPath);
      PrefabDimensions dims = createDefaultDimensions();
      dimensionsCache.put(modRelativePath, dims);
      return dims;

    } catch (Exception e) {
      log.fine("Failed to inspect prefab %s: %s", modRelativePath, e.getMessage());
      PrefabDimensions dims = createDefaultDimensions();
      dimensionsCache.put(modRelativePath, dims);
      return dims;
    }
  }

  @Nonnull
  @SuppressWarnings("null")
  private PrefabDimensions parseDimensions(@Nonnull String modRelativePath, @Nonnull BufferedReader reader) {
    try {
      JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
      JsonArray blocks = json.getAsJsonArray("blocks");

      if (blocks == null || blocks.isEmpty()) {
        PrefabDimensions dims = createDefaultDimensions();
        dimensionsCache.put(modRelativePath, dims);
        return dims;
      }

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

      if (dims.minX == Integer.MAX_VALUE || dims.minY == Integer.MAX_VALUE || dims.minZ == Integer.MAX_VALUE) {
        PrefabDimensions fallback = createDefaultDimensions();
        dimensionsCache.put(modRelativePath, fallback);
        return fallback;
      }

      dims.width = dims.maxX - dims.minX + 1;
      dims.depth = dims.maxZ - dims.minZ + 1;

      dims.width = Math.max(1, dims.width);
      dims.depth = Math.max(1, dims.depth);

      dimensionsCache.put(modRelativePath, dims);
      return dims;

    } catch (Exception e) {
      PrefabDimensions dims = createDefaultDimensions();
      dimensionsCache.put(modRelativePath, dims);
      return dims;
    }
  }

  @Nonnull
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

  public void clearCache() {
    dimensionsCache.clear();
  }

  @Nullable
  private Path resolveUnpackedPrefab(@Nonnull String entryPath) {
    Path root = unpackedRoot;
    if (root == null) {
      return null;
    }
    String trimmed = entryPath.startsWith("Server/") ? entryPath.substring("Server/".length()) : entryPath;
    return root.resolve(trimmed);
  }

  @Nullable
  private Path resolveAssetPrefab(@Nonnull String entryPath) {
    try {
      String trimmed = entryPath.startsWith("Server/Prefabs/")
          ? entryPath.substring("Server/Prefabs/".length())
          : entryPath;
      java.util.List<Path> roots = new java.util.ArrayList<>();
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
      for (Path root : roots) {
        if (root == null) {
          continue;
        }
        Path candidate = root.resolve(trimmed);
        if (Files.exists(candidate)) {
          return candidate;
        }
      }
    } catch (Exception e) {
      log.fine("Failed to resolve asset prefab path for %s: %s", entryPath, e.getMessage());
    }
    return null;
  }
}
