package MBRound18.hytale.vexlichdungeon.dungeon;

import javax.annotation.Nonnull;

/**
 * Configuration for dungeon generation behavior.
 * Controls generation radius, prefab selection weights, and performance
 * settings.
 */
public class GenerationConfig {

  /** Default size of each tile in blocks */
  public static final int DEFAULT_TILE_SIZE = 19;

  /** Default gap between stitched tiles (0 = edge-to-edge stitching) */
  public static final int DEFAULT_GATE_GAP = 0;

  /** Default number of tiles to generate in each direction from spawn */
  public static final int DEFAULT_GENERATION_RADIUS = 5;

  /** Default probability of placing a room vs hallway (0.0 to 1.0) */
  public static final double DEFAULT_ROOM_PROBABILITY = 0.7;

  /** Default stitch pattern prefab path (relative to Server/Prefabs/) */
  public static final String DEFAULT_STITCH_PATTERN = "Stitch/Vex_Stitch_Pattern_A";

  /** Default world bounds to prevent spawning outside valid Y range */
  public static final int DEFAULT_WORLD_MIN_Y = 0;
  public static final int DEFAULT_WORLD_MAX_Y = 320;

  private int tileSize = DEFAULT_TILE_SIZE;
  private int gateGap = DEFAULT_GATE_GAP;
  private int generationRadius = DEFAULT_GENERATION_RADIUS;
  private double roomProbability = DEFAULT_ROOM_PROBABILITY;
  private boolean asyncGeneration = true;
  private int batchSize = 10; // Number of tiles to generate per batch
  private long seed = System.currentTimeMillis();
  private boolean useStitchPattern = true;
  private String stitchPatternPrefab = DEFAULT_STITCH_PATTERN;
  private int worldMinY = DEFAULT_WORLD_MIN_Y;
  private int worldMaxY = DEFAULT_WORLD_MAX_Y;

  /**
   * Gets the size of each tile in blocks.
   * 
   * @return Tile size (default: 19)
   */
  public int getTileSize() {
    return tileSize;
  }

  /**
   * Sets the size of each tile in blocks.
   * 
   * @param tileSize Tile size (must be positive)
   * @return This config for chaining
   */
  @Nonnull
  public GenerationConfig setTileSize(int tileSize) {
    if (tileSize <= 0) {
      throw new IllegalArgumentException("Tile size must be positive: " + tileSize);
    }
    this.tileSize = tileSize;
    return this;
  }

  /**
   * Gets the gap between stitched tiles (0 = edge-to-edge stitching).
   */
  public int getGateGap() {
    return gateGap;
  }

  /**
   * Sets the gap between stitched tiles (0 = edge-to-edge stitching).
   *
   * @param gateGap Gap size (must be non-negative)
   * @return This config for chaining
   */
  @Nonnull
  public GenerationConfig setGateGap(int gateGap) {
    if (gateGap < 0) {
      throw new IllegalArgumentException("Gate gap must be non-negative: " + gateGap);
    }
    this.gateGap = gateGap;
    return this;
  }

  /**
   * Gets the total grid step (tile size + seam gap).
   */
  public int getGridStep() {
    return tileSize + gateGap;
  }

  /**
   * Gets the number of tiles to generate in each direction from spawn.
   * 
   * @return Generation radius (default: 5)
   */
  public int getGenerationRadius() {
    return generationRadius;
  }

  /**
   * Sets the number of tiles to generate in each direction from spawn.
   * 
   * @param radius Generation radius (must be positive)
   * @return This config for chaining
   */
  @Nonnull
  public GenerationConfig setGenerationRadius(int radius) {
    if (radius <= 0) {
      throw new IllegalArgumentException("Generation radius must be positive: " + radius);
    }
    this.generationRadius = radius;
    return this;
  }

  /**
   * Gets the probability of generating a room vs hallway.
   * 
   * @return Room probability (0.0 = always hallway, 1.0 = always room)
   */
  public double getRoomProbability() {
    return roomProbability;
  }

  /**
   * Sets the probability of generating a room vs hallway.
   * 
   * @param probability Room probability (0.0 to 1.0)
   * @return This config for chaining
   */
  @Nonnull
  public GenerationConfig setRoomProbability(double probability) {
    if (probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("Room probability must be between 0.0 and 1.0: " + probability);
    }
    this.roomProbability = probability;
    return this;
  }

  /**
   * Checks if generation should run asynchronously.
   * 
   * @return True if async generation is enabled
   */
  public boolean isAsyncGeneration() {
    return asyncGeneration;
  }

  /**
   * Sets whether generation should run asynchronously.
   * 
   * @param async True to enable async generation
   * @return This config for chaining
   */
  @Nonnull
  public GenerationConfig setAsyncGeneration(boolean async) {
    this.asyncGeneration = async;
    return this;
  }

  /**
   * Gets the number of tiles to generate per batch.
   * 
   * @return Batch size
   */
  public int getBatchSize() {
    return batchSize;
  }

  /**
   * Sets the number of tiles to generate per batch.
   * 
   * @param batchSize Batch size (must be positive)
   * @return This config for chaining
   */
  @Nonnull
  public GenerationConfig setBatchSize(int batchSize) {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("Batch size must be positive: " + batchSize);
    }
    this.batchSize = batchSize;
    return this;
  }

  /**
   * Gets the seed for procedural generation.
   * 
   * @return Generation seed
   */
  public long getSeed() {
    return seed;
  }

  /**
   * Sets the seed for procedural generation.
   * 
   * @param seed Generation seed
   * @return This config for chaining
   */
  @Nonnull
  public GenerationConfig setSeed(long seed) {
    this.seed = seed;
    return this;
  }

  /**
   * Enables or disables stitch pattern usage.
   */
  public boolean isUseStitchPattern() {
    return useStitchPattern;
  }

  /**
   * Sets whether to use the stitch pattern prefab to derive tile sizing.
   */
  @Nonnull
  public GenerationConfig setUseStitchPattern(boolean useStitchPattern) {
    this.useStitchPattern = useStitchPattern;
    return this;
  }

  /**
   * Gets the stitch pattern prefab path (relative to Server/Prefabs/).
   */
  @Nonnull
  public String getStitchPatternPrefab() {
    return java.util.Objects.requireNonNull(stitchPatternPrefab, "stitchPatternPrefab");
  }

  /**
   * Sets the stitch pattern prefab path (relative to Server/Prefabs/).
   */
  @Nonnull
  public GenerationConfig setStitchPatternPrefab(@Nonnull String stitchPatternPrefab) {
    if (stitchPatternPrefab.isBlank()) {
      throw new IllegalArgumentException("Stitch pattern prefab path cannot be blank");
    }
    this.stitchPatternPrefab = java.util.Objects.requireNonNull(stitchPatternPrefab, "stitchPatternPrefab");
    return this;
  }

  /**
   * Gets the minimum world Y allowed for prefab placement.
   */
  public int getWorldMinY() {
    return worldMinY;
  }

  /**
   * Sets the minimum world Y allowed for prefab placement.
   */
  @Nonnull
  public GenerationConfig setWorldMinY(int worldMinY) {
    this.worldMinY = worldMinY;
    return this;
  }

  /**
   * Gets the maximum world Y allowed for prefab placement.
   */
  public int getWorldMaxY() {
    return worldMaxY;
  }

  /**
   * Sets the maximum world Y allowed for prefab placement.
   */
  @Nonnull
  public GenerationConfig setWorldMaxY(int worldMaxY) {
    this.worldMaxY = worldMaxY;
    return this;
  }

  /**
   * Calculates the total number of tiles that will be generated.
   * Formula: 1 (base) + 4 * radius (cardinal directions)
   * 
   * @return Total tile count
   */
  public int calculateTotalTiles() {
    return 1 + (4 * generationRadius);
  }

  /**
   * Creates a new config with default values.
   * 
   * @return New generation config
   */
  @Nonnull
  public static GenerationConfig createDefault() {
    return new GenerationConfig();
  }

  /**
   * Creates a copy of this config.
   * 
   * @return New config with same values
   */
  @Nonnull
  public GenerationConfig copy() {
    return new GenerationConfig()
        .setTileSize(tileSize)
        .setGateGap(gateGap)
        .setGenerationRadius(generationRadius)
        .setRoomProbability(roomProbability)
        .setAsyncGeneration(asyncGeneration)
        .setBatchSize(batchSize)
        .setSeed(seed)
        .setUseStitchPattern(useStitchPattern)
        .setStitchPatternPrefab(java.util.Objects.requireNonNull(stitchPatternPrefab, "stitchPatternPrefab"))
        .setWorldMinY(worldMinY)
        .setWorldMaxY(worldMaxY);
  }

  @Override
  public String toString() {
    return String.format(
        "GenerationConfig[radius=%d, tileSize=%d, gateGap=%d, roomProb=%.2f, tiles=%d, async=%b, batch=%d, seed=%d, stitch=%b, pattern=%s, worldY=[%d,%d]]",
        generationRadius, tileSize, gateGap, roomProbability, calculateTotalTiles(),
        asyncGeneration, batchSize, seed, useStitchPattern, stitchPatternPrefab, worldMinY, worldMaxY);
  }
}
