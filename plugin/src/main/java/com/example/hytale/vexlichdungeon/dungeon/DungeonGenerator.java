package com.example.hytale.vexlichdungeon.dungeon;

import com.example.hytale.vexlichdungeon.logging.PluginLog;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Core dungeon generation logic.
 * Manages tile placement, gate assignment, and procedural layout generation.
 */
public class DungeonGenerator {

  private final PluginLog log;
  private final GenerationConfig config;
  private final PrefabSelector selector;
  private final Map<TilePosition, DungeonTile> tileMap;

  /**
   * Creates a new dungeon generator.
   * 
   * @param config Generation configuration
   * @param log    Logger for generation events
   */
  public DungeonGenerator(@Nonnull GenerationConfig config, @Nonnull PluginLog log) {
    this.config = config;
    this.log = log;
    this.selector = new PrefabSelector(config.getSeed());
    this.tileMap = new HashMap<>();
  }

  /**
   * Generates the complete dungeon layout.
   * This creates the tile map but does NOT spawn prefabs in the world yet.
   * 
   * @return CompletableFuture that completes when generation is done
   */
  @Nonnull
  public CompletableFuture<GenerationResult> generateLayout() {
    log.info("Starting dungeon generation with config: %s", config);
    long startTime = System.currentTimeMillis();

    if (config.isAsyncGeneration()) {
      return CompletableFuture.supplyAsync(() -> performGeneration(startTime));
    } else {
      return CompletableFuture.completedFuture(performGeneration(startTime));
    }
  }

  /**
   * Performs the actual generation logic.
   */
  @Nonnull
  private GenerationResult performGeneration(long startTime) {
    try {
      // Phase 1: Place base tile at origin
      log.info("Phase 1: Placing base tile at origin");
      placeBaseTile();

      // Phase 2: Generate tiles in each cardinal direction
      log.info("Phase 2: Generating tiles in cardinal directions");
      for (CardinalDirection direction : CardinalDirection.all()) {
        generateDirectionalChain(direction);
      }

      // Phase 3: Assign gates between tiles
      log.info("Phase 3: Assigning gates between tiles");
      assignGates();

      // Phase 4: Block outer edges
      log.info("Phase 4: Blocking outer edges");
      blockOuterEdges();

      long duration = System.currentTimeMillis() - startTime;
      log.info("Generation complete! Generated %d tiles in %d ms", tileMap.size(), duration);

      return new GenerationResult(true, tileMap, duration, null);

    } catch (Exception e) {
      log.error("Generation failed: %s", e.getMessage());
      return new GenerationResult(false, Collections.emptyMap(),
          System.currentTimeMillis() - startTime, e);
    }
  }

  /**
   * Places the base courtyard tile at the origin (0, 0).
   */
  private void placeBaseTile() {
    String basePrefab = selector.getBasePrefab();
    DungeonTile baseTile = new DungeonTile(
        0, 0,
        basePrefab,
        0, // No rotation for base
        DungeonTile.TileType.BASE);
    tileMap.put(new TilePosition(0, 0), baseTile);
    log.info("Placed base tile at (0, 0): %s", basePrefab);
  }

  /**
   * Generates a chain of tiles in the specified direction.
   * 
   * @param direction Cardinal direction to generate
   */
  private void generateDirectionalChain(@Nonnull CardinalDirection direction) {
    int radius = config.getGenerationRadius();
    int offsetX = direction.getOffsetX();
    int offsetZ = direction.getOffsetZ();

    for (int distance = 1; distance <= radius; distance++) {
      int gridX = offsetX * distance;
      int gridZ = offsetZ * distance;

      // Select room or hallway based on probability
      String prefabPath = selector.selectRoomOrHallway(config.getRoomProbability());
      int rotation = selector.selectRandomRotation();

      // Determine tile type based on prefab path
      DungeonTile.TileType type = prefabPath.contains("Hallway")
          ? DungeonTile.TileType.HALLWAY
          : DungeonTile.TileType.ROOM;

      DungeonTile tile = new DungeonTile(gridX, gridZ, prefabPath, rotation, type);
      tileMap.put(new TilePosition(gridX, gridZ), tile);

      if (distance % config.getBatchSize() == 0) {
        log.info("Generated %d tiles in %s direction", distance, direction);
      }
    }
  }

  /**
   * Assigns gates between adjacent tiles.
   */
  private void assignGates() {
    for (DungeonTile tile : tileMap.values()) {
      for (CardinalDirection direction : CardinalDirection.all()) {
        TilePosition neighborPos = new TilePosition(
            tile.getGridX() + direction.getOffsetX(),
            tile.getGridZ() + direction.getOffsetZ());

        DungeonTile neighbor = tileMap.get(neighborPos);
        if (neighbor != null && !tile.hasGate(direction)) {
          // There's a neighbor in this direction, assign a random gate
          String gatePrefab = selector.selectRandomGate();
          tile.setGate(direction, gatePrefab);
        }
      }
    }

    log.info("Assigned gates to %d tiles", tileMap.size());
  }

  /**
   * Blocks outer edges of the dungeon with blocked gate prefabs.
   */
  private void blockOuterEdges() {
    String blockedGate = selector.getBlockedGate();
    int blockedCount = 0;

    for (DungeonTile tile : tileMap.values()) {
      for (CardinalDirection direction : CardinalDirection.all()) {
        TilePosition neighborPos = new TilePosition(
            tile.getGridX() + direction.getOffsetX(),
            tile.getGridZ() + direction.getOffsetZ());

        // If there's no neighbor in this direction, it's an outer edge
        if (!tileMap.containsKey(neighborPos)) {
          tile.setGate(direction, blockedGate);
          blockedCount++;
        }
      }
    }

    log.info("Blocked %d outer edge gates", blockedCount);
  }

  /**
   * Gets the generated tile map.
   * 
   * @return Immutable map of tile positions to tiles
   */
  @Nonnull
  public Map<TilePosition, DungeonTile> getTileMap() {
    return Collections.unmodifiableMap(tileMap);
  }

  /**
   * Gets the tile at a specific grid position.
   * 
   * @param gridX Grid X coordinate
   * @param gridZ Grid Z coordinate
   * @return Tile at that position, or null if none exists
   */
  @Nullable
  public DungeonTile getTile(int gridX, int gridZ) {
    return tileMap.get(new TilePosition(gridX, gridZ));
  }

  /**
   * Gets the generation configuration.
   * 
   * @return Generation config
   */
  @Nonnull
  public GenerationConfig getConfig() {
    return config;
  }

  /**
   * Represents a tile position in the grid.
   */
  private static class TilePosition {
    private final int x;
    private final int z;

    TilePosition(int x, int z) {
      this.x = x;
      this.z = z;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      TilePosition that = (TilePosition) o;
      return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
      return Objects.hash(x, z);
    }

    @Override
    public String toString() {
      return String.format("(%d, %d)", x, z);
    }
  }

  /**
   * Result of dungeon generation.
   */
  public static class GenerationResult {
    private final boolean success;
    private final Map<TilePosition, DungeonTile> tiles;
    private final long durationMs;
    private final Throwable error;

    GenerationResult(boolean success, Map<TilePosition, DungeonTile> tiles,
        long durationMs, @Nullable Throwable error) {
      this.success = success;
      this.tiles = Collections.unmodifiableMap(tiles);
      this.durationMs = durationMs;
      this.error = error;
    }

    public boolean isSuccess() {
      return success;
    }

    @Nonnull
    public Map<TilePosition, DungeonTile> getTiles() {
      return tiles;
    }

    public long getDurationMs() {
      return durationMs;
    }

    @Nullable
    public Throwable getError() {
      return error;
    }

    public int getTileCount() {
      return tiles.size();
    }
  }
}
