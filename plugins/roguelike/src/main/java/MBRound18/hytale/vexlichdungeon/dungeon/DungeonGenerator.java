package MBRound18.hytale.vexlichdungeon.dungeon;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabInspector;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;

/**
 * Core dungeon generation logic.
 * Manages tile placement and procedural layout generation.
 */
public class DungeonGenerator {

  private final EngineLog log;
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
  public DungeonGenerator(@Nonnull GenerationConfig config, @Nonnull EngineLog log,
      @Nonnull PrefabDiscovery discovery) {
    this.config = config;
    this.log = log;
    this.discovery = discovery;
    this.selector = new PrefabSelector(config.getSeed(), discovery);
    this.tileMap = new HashMap<>();
    this.spawnCenterX = 0;
    this.spawnCenterY = 64;
    this.spawnCenterZ = 0;
    applyStitchPattern();
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
   * Checks if base tile spawning is skipped.
   */
  public boolean isSkipBaseTile() {
    return skipBaseTile;
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
   * Tiles are stitched edge-to-edge with no gate gap.
   * 
   * @param gridX Grid X coordinate
   * @param gridZ Grid Z coordinate
   * @return int[2] array with {worldX, worldZ}
   */
  public int[] gridToWorld(int gridX, int gridZ) {
    int gridStep = config.getGridStep(); // tile size + seam gap (usually 0)
    int worldX = spawnCenterX + (gridX * gridStep);
    int worldZ = spawnCenterZ + (gridZ * gridStep);
    return new int[] { worldX, worldZ };
  }

  private void applyStitchPattern() {
    if (!config.isUseStitchPattern()) {
      return;
    }

    String patternPrefab = config.getStitchPatternPrefab();
    String entryPath = "Server/Prefabs/" + patternPrefab + ".prefab.json";
    ZipEntry entry = discovery.getZipFile().getEntry(entryPath);
    if (entry == null) {
      log.warn("Stitch pattern prefab not found in ZIP: %s (using tileSize=%d)",
          entryPath, config.getTileSize());
      return;
    }

    PrefabInspector inspector = new PrefabInspector(log, discovery.getZipFile(), config.getTileSize(),
        config.getGateGap());
    PrefabInspector.PrefabDimensions dims = inspector.getPrefabDimensions(patternPrefab);
    int tileSize = Math.max(dims.width, dims.depth);
    if (tileSize <= 0) {
      log.warn("Invalid stitch pattern dimensions for %s (width=%d, depth=%d). Using tileSize=%d",
          patternPrefab, dims.width, dims.depth, config.getTileSize());
      return;
    }

    if (tileSize != config.getTileSize()) {
      log.info("Applied stitch pattern %s: tileSize %d -> %d",
          patternPrefab, config.getTileSize(), tileSize);
      config.setTileSize(tileSize);
    }
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

      // Phase 3: Layout complete (no gates; tiles stitch edge-to-edge)
      log.info("Phase 3: Finalizing stitched layout");

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
