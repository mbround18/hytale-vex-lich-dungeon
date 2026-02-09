package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.dungeon.CardinalDirection;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class PrefabEdgeAnalyzer {
  private static final int[] ROTATIONS = new int[] { 0, 90, 180, 270 };

  private final PrefabSpawner spawner;
  private final LoggingHelper log;
  private final Map<String, List<Block>> blockCache = new ConcurrentHashMap<>();
  private final Map<String, Map<Integer, Map<CardinalDirection, EdgeSlice>>> edgeCache = new ConcurrentHashMap<>();
  private final Map<String, StitchPattern> stitchCache = new ConcurrentHashMap<>();

  PrefabEdgeAnalyzer(@Nonnull PrefabSpawner spawner, @Nonnull LoggingHelper log) {
    this.spawner = Objects.requireNonNull(spawner, "spawner");
    this.log = Objects.requireNonNull(log, "log");
  }

  @Nonnull
  Map<Integer, Map<CardinalDirection, EdgeSlice>> getEdges(@Nonnull String prefabPath) {
    return edgeCache.computeIfAbsent(prefabPath, this::buildEdges);
  }

  @Nullable
  StitchPattern getStitchPattern(@Nonnull String stitchPrefab) {
    return stitchCache.computeIfAbsent(stitchPrefab, this::buildPattern);
  }

  @Nonnull
  private Map<Integer, Map<CardinalDirection, EdgeSlice>> buildEdges(@Nonnull String prefabPath) {
    List<Block> blocks = loadBlocks(prefabPath);
    if (blocks.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Integer, Map<CardinalDirection, EdgeSlice>> rotations = new HashMap<>();
    for (int rotation : ROTATIONS) {
      RotatedBlocks rotated = rotateBlocks(blocks, rotation);
      rotations.put(rotation, buildEdgeSlices(rotated));
    }
    return Collections.unmodifiableMap(rotations);
  }

  @Nullable
  private StitchPattern buildPattern(@Nonnull String stitchPrefab) {
    List<Block> blocks = loadBlocks(stitchPrefab);
    if (blocks.isEmpty()) {
      return null;
    }
    Bounds bounds = Bounds.from(blocks);
    int spanX = bounds.maxX - bounds.minX + 1;
    int spanZ = bounds.maxZ - bounds.minZ + 1;
    boolean useX = spanX >= spanZ;
    int minH = useX ? bounds.minX : bounds.minZ;
    int maxH = useX ? bounds.maxX : bounds.maxZ;
    int minY = bounds.minY;
    int maxY = bounds.maxY;
    int width = maxH - minH + 1;
    int height = maxY - minY + 1;
    int minDepth = useX ? bounds.minZ : bounds.minX;
    int maxDepth = useX ? bounds.maxZ : bounds.maxX;
    if ((maxDepth - minDepth) > 0) {
      log.warn("Stitch prefab %s has depth %d; using first slice for pattern matching",
          stitchPrefab, maxDepth - minDepth + 1);
    }
    Map<Long, String> cells = new HashMap<>();
    for (Block block : blocks) {
      int depth = useX ? block.z : block.x;
      if (depth != minDepth) {
        continue;
      }
      int u = (useX ? block.x : block.z) - minH;
      int v = block.y - minY;
      if (u < 0 || v < 0 || u >= width || v >= height) {
        continue;
      }
      cells.put(StitchPattern.key(u, v), block.name);
    }
    return new StitchPattern(stitchPrefab, width, height, cells);
  }

  @Nonnull
  private Map<CardinalDirection, EdgeSlice> buildEdgeSlices(@Nonnull RotatedBlocks rotated) {
    Bounds bounds = rotated.bounds;
    Map<CardinalDirection, EdgeSlice> edges = new EnumMap<>(CardinalDirection.class);

    int widthX = bounds.maxX - bounds.minX + 1;
    int widthZ = bounds.maxZ - bounds.minZ + 1;
    int height = bounds.maxY - bounds.minY + 1;

    EdgeSlice north = new EdgeSlice(widthX, height);
    EdgeSlice south = new EdgeSlice(widthX, height);
    EdgeSlice west = new EdgeSlice(widthZ, height);
    EdgeSlice east = new EdgeSlice(widthZ, height);

    int northZ = bounds.minZ;
    int southZ = bounds.maxZ;
    int westX = bounds.minX;
    int eastX = bounds.maxX;

    for (Block block : rotated.blocks) {
      if (block.z == northZ) {
        north.put(block.x - bounds.minX, block.y - bounds.minY, block.name);
      }
      if (block.z == southZ) {
        south.put(block.x - bounds.minX, block.y - bounds.minY, block.name);
      }
      if (block.x == westX) {
        west.put(block.z - bounds.minZ, block.y - bounds.minY, block.name);
      }
      if (block.x == eastX) {
        east.put(block.z - bounds.minZ, block.y - bounds.minY, block.name);
      }
    }

    edges.put(CardinalDirection.NORTH, north);
    edges.put(CardinalDirection.SOUTH, south);
    edges.put(CardinalDirection.WEST, west);
    edges.put(CardinalDirection.EAST, east);
    return Collections.unmodifiableMap(edges);
  }

  @Nonnull
  private List<Block> loadBlocks(@Nonnull String prefabPath) {
    return blockCache.computeIfAbsent(prefabPath, this::parseBlocks);
  }

  @Nonnull
  private List<Block> parseBlocks(@Nonnull String prefabPath) {
    String json = spawner.getPrefabJson(prefabPath);
    if (json == null || json.isBlank()) {
      log.warn("Prefab JSON not found for %s", prefabPath);
      return List.of();
    }
    try {
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      JsonArray blocks = root.getAsJsonArray("blocks");
      if (blocks == null || blocks.isEmpty()) {
        return List.of();
      }
      List<Block> out = new ArrayList<>(blocks.size());
      for (JsonElement elem : blocks) {
        if (!elem.isJsonObject()) {
          continue;
        }
        JsonObject block = elem.getAsJsonObject();
        if (!block.has("x") || !block.has("y") || !block.has("z")) {
          continue;
        }
        String name = block.has("name") ? block.get("name").getAsString() : null;
        if (name == null || name.isBlank()) {
          continue;
        }
        out.add(new Block(block.get("x").getAsInt(), block.get("y").getAsInt(),
            block.get("z").getAsInt(), name));
      }
      return out.isEmpty() ? List.of() : Collections.unmodifiableList(out);
    } catch (Exception e) {
      log.warn("Failed to parse prefab blocks for %s: %s", prefabPath, e.getMessage());
      return List.of();
    }
  }

  private RotatedBlocks rotateBlocks(@Nonnull List<Block> blocks, int rotation) {
    List<Block> rotated = new ArrayList<>(blocks.size());
    for (Block block : blocks) {
      int[] rot = rotateXZ(block.x, block.z, rotation);
      rotated.add(new Block(rot[0], block.y, rot[1], block.name));
    }
    return new RotatedBlocks(Collections.unmodifiableList(rotated), Bounds.from(rotated));
  }

  private static int[] rotateXZ(int x, int z, int rotation) {
    int normalized = ((rotation % 360) + 360) % 360;
    return switch (normalized) {
      case 0 -> new int[] { x, z };
      case 90 -> new int[] { z, -x };
      case 180 -> new int[] { -x, -z };
      case 270 -> new int[] { -z, x };
      default -> new int[] { x, z };
    };
  }

  private record Block(int x, int y, int z, @Nonnull String name) {
  }

  private record RotatedBlocks(@Nonnull List<Block> blocks, @Nonnull Bounds bounds) {
  }

  private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    static Bounds from(@Nonnull List<Block> blocks) {
      int minX = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxY = Integer.MIN_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxZ = Integer.MIN_VALUE;
      for (Block block : blocks) {
        minX = Math.min(minX, block.x);
        maxX = Math.max(maxX, block.x);
        minY = Math.min(minY, block.y);
        maxY = Math.max(maxY, block.y);
        minZ = Math.min(minZ, block.z);
        maxZ = Math.max(maxZ, block.z);
      }
      if (minX == Integer.MAX_VALUE) {
        minX = maxX = minY = maxY = minZ = maxZ = 0;
      }
      return new Bounds(minX, maxX, minY, maxY, minZ, maxZ);
    }
  }
}
