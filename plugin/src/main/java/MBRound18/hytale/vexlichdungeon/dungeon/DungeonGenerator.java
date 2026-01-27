package MBRound18.hytale.vexlichdungeon.dungeon;

import MBRound18.hytale.vexlichdungeon.logging.PluginLog;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
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
  private final PrefabDiscovery discovery;
  private final Map<TilePosition, DungeonTile> tileMap;

  // Spawn center coordinates (world coords) where base tile will be centered
  private int spawnCenterX;
  private int spawnCenterY;
  private int spawnCenterZ;
  private boolean skipBaseTile = false;

  /**
   * Creates a new dungeon generator.
   * 
   * @param config    Generation configuration
   * @param log       Logger for generation events
   * @param discovery Prefab discovery system
   */
  public DungeonGenerator(@Nonnull GenerationConfig config, @Nonnull PluginLog log,
      @Nonnull PrefabDiscovery discovery) {
    this.config = config;
    this.log = log;
    this.discovery = discovery;
    this.selector = new PrefabSelector(config.getSeed(), discovery);
    this.tileMap = new HashMap<>();
    this.spawnCenterX = 0;
    this.spawnCenterY = 64;
    this.spawnCenterZ = 0;
  }

  /**
   * Sets the spawn center coordinates for dungeon generation.
   * The base tile will be centered at these coordinates, and all other
   * tiles will be positioned relative to this center point.
   * 
   * @param worldX World X coordinate for spawn center
   * @param worldY World Y coordinate for spawn center
   * @param worldZ World Z coordinate for spawn center
   */
  public void setSpawnCenter(int worldX, int worldY, int worldZ) {
    this.spawnCenterX = worldX;
    this.spawnCenterY = worldY;
    this.spawnCenterZ = worldZ;
    log.info("Set spawn center to (%d, %d, %d)", worldX, worldY, worldZ);
  }

  /**
   * Gets the spawn center X coordinate.
   */
  public int getSpawnCenterX() {
    return spawnCenterX;
  }

  /**
   * Gets the spawn center Y coordinate.
   */
  public int getSpawnCenterY() {
    return spawnCenterY;
  }

  /**
   * Gets the spawn center Z coordinate.
   */
  public int getSpawnCenterZ() {
    return spawnCenterZ;
  }

  /**
   * Sets whether to skip spawning the base tile (if it's already present).
   * 
   * @param skip Whether to skip the base tile
   */
  public void setSkipBaseTile(boolean skip) {
    this.skipBaseTile = skip;
    if (skip) {
      log.info("Base tile spawning disabled - base prefab already exists");
    }
  }

  /**
   * Converts grid coordinates to world coordinates.
   * Grid (0,0) is at the spawn center.
   * Each tile is 20 units (19 blocks + 1 gate).
   * 
   * @param gridX Grid X coordinate
   * @param gridZ Grid Z coordinate
   * @return int[2] array with {worldX, worldZ}
   */
  public int[] gridToWorld(int gridX, int gridZ) {
    int tileSize = 20; // 19 blocks for room + 1 for gate
    int worldX = spawnCenterX + (gridX * tileSize);
    int worldZ = spawnCenterZ + (gridZ * tileSize);
    return new int[] { worldX, worldZ };
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
    // Skip if base already exists (player standing on bedrock)
    if (skipBaseTile) {
      log.info("Skipping base tile placement - base prefab already present");
      return;
    }

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
   * Actually generates a complete grid filling the dungeon area.
   * 
   * @param direction This parameter is now unused but kept for compatibility
   */
  private void generateDirectionalChain(@Nonnull CardinalDirection direction) {
    // Only generate once, on the first direction call
    // The tile map already has the base tile, so we check if we need to fill
    if (tileMap.size() > 1) {
      return; // Already generated grid
    }

    int radius = config.getGenerationRadius();
    int tilesGenerated = 0;

    // Generate tiles in a square grid from -radius to +radius
    for (int gridX = -radius; gridX <= radius; gridX++) {
      for (int gridZ = -radius; gridZ <= radius; gridZ++) {
        // Skip the base tile (already placed at 0,0)
        if (gridX == 0 && gridZ == 0) {
          continue;
        }

        // Select room or hallway based on probability
        String prefabPath = selector.selectRoomOrHallway(config.getRoomProbability());

        // Skip if no prefabs available
        if (prefabPath == null) {
          log.warn("No rooms or hallways available for tile at (%d, %d)", gridX, gridZ);
          continue;
        }

        int rotation = selector.selectRandomRotation();

        // Determine tile type based on prefab path
        DungeonTile.TileType type = prefabPath.contains("Hallway")
            ? DungeonTile.TileType.HALLWAY
            : DungeonTile.TileType.ROOM;

        DungeonTile tile = new DungeonTile(gridX, gridZ, prefabPath, rotation, type);
        tileMap.put(new TilePosition(gridX, gridZ), tile);
        tilesGenerated++;
      }
    }

    log.info("Generated %d tiles in complete grid pattern (radius=%d)", tilesGenerated, radius);
  }

  /**
   * Assigns gates between adjacent tiles.
   */
  private void assignGates() {
    int gatesAssigned = 0;
    
    for (DungeonTile tile : tileMap.values()) {
      for (CardinalDirection direction : CardinalDirection.all()) {
        TilePosition neighborPos = new TilePosition(
            tile.getGridX() + direction.getOffsetX(),
            tile.getGridZ() + direction.getOffsetZ());

        DungeonTile neighbor = tileMap.get(neighborPos);
        if (neighbor != null && !tile.hasGate(direction)) {
          // There's a neighbor in this direction, assign a random gate
          String gatePrefab = selector.selectRandomGate();
          if (gatePrefab != null) {
            tile.setGate(direction, gatePrefab);
            gatesAssigned++;
          } else {
            log.warn("No gate prefabs available for tile at (%d, %d) direction %s", 
                     tile.getGridX(), tile.getGridZ(), direction);
          }
        }
      }
    }

    log.info("Assigned %d gates to tiles", gatesAssigned);
  }

  /**
   * Blocks outer edges of the dungeon with blocked gate prefabs.
   */
  private void blockOuterEdges() {
    String blockedGate = selector.getBlockedGate();
    if (blockedGate == null) {
      log.warn("No blocked gate prefab available, skipping outer edge blocking");
      return;
    }
    
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
