package com.example.hytale.vexlichdungeon.prefab;

import com.example.hytale.vexlichdungeon.dungeon.CardinalDirection;
import com.example.hytale.vexlichdungeon.dungeon.DungeonTile;
import com.example.hytale.vexlichdungeon.logging.PluginLog;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Service for spawning prefabs into the world.
 * This class interfaces with Hytale's prefab system to load and place prefabs.
 * 
 * Uses BlockSelection.place() to write prefabs to the world with proper
 * rotation
 * and entity spawning support.
 */
public class PrefabSpawner {

  private final PluginLog log;
  private final PrefabStore prefabStore;

  /**
   * Creates a new prefab spawner.
   * 
   * @param log Logger for spawning events
   */
  public PrefabSpawner(@Nonnull PluginLog log) {
    this.log = log;
    this.prefabStore = PrefabStore.get();
  }

  /**
   * Loads a prefab from the mod's asset pack.
   * 
   * @param modRelativePath Path relative to Mods/VexLichDungeon/
   * @return CompletableFuture with the loaded BlockSelection
   */
  @Nonnull
  public CompletableFuture<BlockSelection> loadPrefab(@Nonnull String modRelativePath) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("Loading prefab: %s", modRelativePath);

        // TODO: Investigate proper path resolution for mod assets
        // The PrefabStore might need to access assets from our mod's asset pack
        // Current approach attempts to load from asset pack
        BlockSelection prefab = prefabStore.getAssetPrefabFromAnyPack(modRelativePath);

        if (prefab == null) {
          throw new PrefabLoadException("Prefab not found: " + modRelativePath);
        }

        log.info("Successfully loaded prefab: %s", modRelativePath);
        return prefab;

      } catch (Exception e) {
        log.error("Failed to load prefab %s: %s", modRelativePath, e.getMessage());
        throw new RuntimeException("Failed to load prefab: " + modRelativePath, e);
      }
    });
  }

  /**
   * Spawns a dungeon tile into the world.
   * This includes the main tile prefab and all gates.
   * MUST be called from the world thread.
   * 
   * @param tile   The tile to spawn
   * @param world  The world to spawn into
   * @param worldX World X coordinate for tile origin
   * @param worldY World Y coordinate for tile origin
   * @param worldZ World Z coordinate for tile origin
   */
  public void spawnTile(
      @Nonnull DungeonTile tile,
      @Nonnull World world,
      int worldX,
      int worldY,
      int worldZ) {
    try {
      log.info("Spawning tile at world coords (%d, %d, %d): %s",
          worldX, worldY, worldZ, tile.getPrefabPath());

      // Load the tile's prefab
      BlockSelection tilePrefab = loadPrefab(tile.getPrefabPath()).join();

      // Apply rotation based on tile rotation (Y-axis)
      BlockSelection rotatedPrefab = tilePrefab.cloneSelection()
          .rotate(Axis.Y, tile.getRotation());

      // Write prefab to world at the specified coordinates
      rotatedPrefab.place(
          ConsoleSender.INSTANCE,
          world,
          new Vector3i(worldX, worldY, worldZ),
          null,
          entityRef -> log.info("Spawned entity in tile: %s", entityRef));

      // Spawn gates at edges of the tile
      for (CardinalDirection direction : CardinalDirection.all()) {
        String gatePath = tile.getGate(direction);
        if (gatePath != null) {
          spawnGate(gatePath, direction, world, worldX, worldY, worldZ);
        }
      }

      log.info("Successfully spawned tile at (%d, %d, %d)", worldX, worldY, worldZ);

    } catch (Exception e) {
      log.error("Failed to spawn tile at (%d, %d, %d): %s",
          worldX, worldY, worldZ, e.getMessage());
      throw new RuntimeException("Failed to spawn tile", e);
    }
  }

  /**
   * Spawns a gate prefab at the specified location and direction.
   * MUST be called from the world thread.
   * 
   * @param gatePath   Full mod path to gate prefab
   * @param direction  Direction the gate faces
   * @param world      The world to spawn into
   * @param tileWorldX World X of the parent tile
   * @param tileWorldY World Y of the parent tile
   * @param tileWorldZ World Z of the parent tile
   */
  private void spawnGate(
      @Nonnull String gatePath,
      @Nonnull CardinalDirection direction,
      @Nonnull World world,
      int tileWorldX,
      int tileWorldY,
      int tileWorldZ) {
    try {
        log.info("Spawning gate %s facing %s", gatePath, direction);

        // Calculate gate position based on direction
        // Gates are at the edges of 19x19 tiles
        int gateX = tileWorldX + calculateGateOffsetX(direction, 19);
        int gateZ = tileWorldZ + calculateGateOffsetZ(direction, 19);

        // Load and rotate gate prefab
        BlockSelection gatePrefab = loadPrefab(gatePath).join();
        BlockSelection rotatedGate = gatePrefab.cloneSelection()
            .rotate(Axis.Y, direction.getRotationDegrees());

        // Write gate to world
        rotatedGate.place(
            ConsoleSender.INSTANCE,
            world,
            new Vector3i(gateX, tileWorldY, gateZ),
            null,
            entityRef -> log.info("Spawned entity in gate: %s", entityRef));

        log.info("Successfully spawned gate at (%d, %d, %d) facing %s",
            gateX, tileWorldY, gateZ, direction);

    } catch (Exception e) {
      log.error("Failed to spawn gate %s: %s", gatePath, e.getMessage());
      throw new RuntimeException("Failed to spawn gate", e);
    }  }

  /**   * Calculates the X offset for a gate based on direction.
   * 
   * @param direction Gate direction
   * @param tileSize  Size of the tile (19)
   * @return X offset from tile origin
   */
  private int calculateGateOffsetX(@Nonnull CardinalDirection direction, int tileSize) {
    return switch (direction) {
      case EAST -> tileSize; // Right edge
      case WEST -> -1; // Left edge
      case NORTH, SOUTH -> tileSize / 2; // Center
    };
  }

  /**
   * Calculates the Z offset for a gate based on direction.
   * 
   * @param direction Gate direction
   * @param tileSize  Size of the tile (19)
   * @return Z offset from tile origin
   */
  private int calculateGateOffsetZ(@Nonnull CardinalDirection direction, int tileSize) {
    return switch (direction) {
      case NORTH -> -1; // Front edge
      case SOUTH -> tileSize; // Back edge
      case EAST, WEST -> tileSize / 2; // Center
    };
  }

  /**
   * Exception thrown when prefab loading fails.
   */
  public static class PrefabLoadException extends RuntimeException {
    public PrefabLoadException(String message) {
      super(message);
    }

    public PrefabLoadException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
