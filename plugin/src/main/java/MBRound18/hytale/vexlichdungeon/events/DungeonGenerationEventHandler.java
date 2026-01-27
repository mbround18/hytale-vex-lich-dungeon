package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.data.PlayerSpawnTracker;
import MBRound18.hytale.vexlichdungeon.dungeon.DungeonGenerator;
import MBRound18.hytale.vexlichdungeon.logging.PluginLog;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for player join events and automatically generates dungeons
 * for VexLichDungeon worlds that haven't been generated yet.
 */
public class DungeonGenerationEventHandler {

  private final PluginLog log;
  private final DungeonGenerator dungeonGenerator;
  private final PrefabSpawner prefabSpawner;
  private final DataStore dataStore;
  private final PlayerSpawnTracker spawnTracker;
  private final Set<String> currentlyGenerating = ConcurrentHashMap.newKeySet();

  /**
   * Creates a new dungeon generation event handler.
   * 
   * @param log              Logger for events
   * @param dungeonGenerator Generator for dungeon layouts
   * @param prefabSpawner    Spawner for prefabs into world
   * @param dataStore        Persistent storage for tracking generated worlds
   */
  public DungeonGenerationEventHandler(
      @Nonnull PluginLog log,
      @Nonnull DungeonGenerator dungeonGenerator,
      @Nonnull PrefabSpawner prefabSpawner,
      @Nonnull DataStore dataStore) {
    this.log = log;
    this.dungeonGenerator = dungeonGenerator;
    this.prefabSpawner = prefabSpawner;
    this.dataStore = dataStore;
    this.spawnTracker = new PlayerSpawnTracker();
  }

  /**
   * Registers this handler with the event bus.
   * 
   * @param eventBus The event bus to register with
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    eventBus.register(
        (Class) StartWorldEvent.class,
        (java.util.function.Consumer) (Object e) -> onStartWorld((StartWorldEvent) e));
    log.info("Successfully registered dungeon generation event handler");
  }

  /**
   * Registers this handler with the event registry (global listeners like
   * InstancesPlugin).
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventRegistry eventRegistry) {
    eventRegistry.registerGlobal(
        (Class) AddPlayerToWorldEvent.class,
        (java.util.function.Consumer) (Object e) -> onAddPlayerToWorld((AddPlayerToWorldEvent) e));

    eventRegistry.registerGlobal(
        (Class) PlayerReadyEvent.class,
        (java.util.function.Consumer) (Object e) -> onPlayerReady((PlayerReadyEvent) e));

    log.info(
        "Successfully registered dungeon generation handler on EventRegistry (AddPlayerToWorldEvent, PlayerReadyEvent)");
  }

  /**
   * Poll worlds for VexLichDungeon instances and generate missing ones.
   * Safe to call periodically from a watchdog thread.
   */
  public void pollAndGenerate() {
    try {
      Map<String, World> worlds = Universe.get().getWorlds();
      for (World world : worlds.values()) {
        handleWorld(world);
      }
    } catch (Exception e) {
      log.error("Exception in pollAndGenerate: %s", e.getMessage());
    }
  }

  /**
   * Called when a world starts.
   * If it's a VexLichDungeon world that hasn't been generated yet,
   * triggers automatic dungeon generation.
   * 
   * @param event The start world event
   */
  @SuppressWarnings("unused")
  private void onStartWorld(StartWorldEvent event) {
    try {
      handleWorld(event.getWorld());
    } catch (Exception e) {
      log.error("Exception in onStartWorld: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  private void onAddPlayerToWorld(AddPlayerToWorldEvent event) {
    try {
      World world = event.getWorld();
      String worldName = world.getName();

      // Only track Vex dungeon worlds
      if (!worldName.contains("Vex_The_Lich_Dungeon")) {
        return;
      }

      log.info("[EVENT] AddPlayerToWorldEvent fired for world: %s", worldName);

      // Capture first player's actual position if not already tracked
      // Check if we already have a spawn location for this world
      if (!spawnTracker.hasSpawn(worldName)) {
        // Get all players currently in the world
        java.util.List<com.hypixel.hytale.server.core.entity.entities.Player> players = world.getPlayers();

        if (!players.isEmpty()) {
          // Get the first player (the one who just joined)
          com.hypixel.hytale.server.core.entity.entities.Player player = players.get(0);
          com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform = player
              .getTransformComponent();

          if (transform != null) {
            Vector3d playerPos = transform.getPosition();

            boolean isFirstSpawn = spawnTracker.recordFirstSpawn(worldName, playerPos);

            if (isFirstSpawn) {
              log.info("[SPAWN] Captured first player actual position at (%.1f, %.1f, %.1f) for world: %s",
                  playerPos.x, playerPos.y, playerPos.z, worldName);

              // Set spawn center on generator
              // Use player's actual position as the dungeon floor level
              // Round X and Z to nearest block, use player's Y directly
              int spawnX = (int) Math.floor(playerPos.x + 0.5);
              int baseY = (int) Math.floor(playerPos.y);
              int spawnY = baseY; // Will adjust to ground level if a solid block is below
              int spawnZ = (int) Math.floor(playerPos.z + 0.5);

              // Check if player is standing on bedrock - if so, base prefab already exists
              boolean baseAlreadyPresent = false;
              try {
                long chunkKey = (((long) (int) playerPos.x >> 4) << 32)
                    | (((long) (int) playerPos.z >> 4) & 0xFFFFFFFFL);
                com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor chunk = world
                    .getChunkIfLoaded(chunkKey);
                if (chunk != null) {
                  int blockBelowY = baseY - 1;
                  int blockBelow = chunk.getBlock((int) playerPos.x, blockBelowY, (int) playerPos.z);
                  // blockBelow is an ID, check if it matches bedrock block ID
                  // For now, we'll skip base if any solid block is found below
                  baseAlreadyPresent = blockBelow != 0; // 0 is typically empty/air
                  if (blockBelow != 0) {
                    spawnY = blockBelowY; // Align prefab floor to the solid block below player
                  }
                }
              } catch (Exception e) {
                log.warn("Could not check block below player: %s", e.getMessage());
              }

              // Adjust for known instance spawn offset (dungeon appears ~14 blocks high)
              final int spawnYOffset = -14;
              spawnY += spawnYOffset;

              log.info("[SPAWN] PlayerY=%d, baseY=%d, spawnY=%d (offset=%d)",
                  (int) Math.floor(playerPos.y), baseY, spawnY, spawnYOffset);

              log.info("[SPAWN] Dungeon will be centered at grid coordinates: (%d, %d, %d), base present: %s",
                  spawnX, spawnY, spawnZ, baseAlreadyPresent);

              if (baseAlreadyPresent) {
                log.info("[SPAWN] Base courtyard already present (bedrock detected), skipping base spawning");
              }

              dungeonGenerator.setSpawnCenter(spawnX, spawnY, spawnZ);
              dungeonGenerator.setSkipBaseTile(baseAlreadyPresent);
            }
          }
        } else {
          log.info("[SPAWN] No players found in world yet, will capture on next event");
        }
      }

      // Trigger generation if spawn is tracked and not yet generated
      if (spawnTracker.hasSpawn(worldName)) {
        handleWorld(world);
      }
    } catch (Exception e) {
      log.error("Exception in onAddPlayerToWorld: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  private void onPlayerReady(PlayerReadyEvent event) {
    try {
      log.info("[EVENT] PlayerReadyEvent fired - polling all worlds");
      // PlayerReadyEvent lacks world; fall back to polling all worlds
      pollAndGenerate();
    } catch (Exception e) {
      log.error("Exception in onPlayerReady: %s", e.getMessage());
    }
  }

  private void handleWorld(World world) {
    String worldName = world.getName();

    if (!worldName.contains("Vex_The_Lich_Dungeon")) {
      return;
    }

    if (dataStore.isGenerated(worldName)) {
      return;
    }

    if (!currentlyGenerating.add(worldName)) {
      return;
    }

    generateDungeonOnWorldThread(world)
        .exceptionally(ex -> {
          log.error("Failed to generate dungeon for world %s: %s", worldName, ex.getMessage());
          currentlyGenerating.remove(worldName); // Allow retry on failure
          return null;
        });
  }

  /**
   * Generates a dungeon for the specified world on its world thread.
   * 
   * @param world The world to generate dungeon for
   * @return CompletableFuture that completes when generation is done
   */
  @Nonnull
  private CompletableFuture<Void> generateDungeonOnWorldThread(@Nonnull World world) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    world.execute(() -> {
      try {
        log.info("Starting dungeon generation for world: %s", world.getName());

        // Generate the dungeon layout
        DungeonGenerator.GenerationResult result = dungeonGenerator.generateLayout().join();

        log.info("Generated dungeon layout: %d tiles", result.getTileCount());

        // Spawn tiles into the world using grid-to-world conversion
        Map<?, ?> tiles = result.getTiles();

        // Iterate through tiles and spawn them at their calculated world positions
        for (Object key : tiles.keySet()) {
          Object tile = tiles.get(key);

          if (tile instanceof MBRound18.hytale.vexlichdungeon.dungeon.DungeonTile) {
            MBRound18.hytale.vexlichdungeon.dungeon.DungeonTile dungeonTile = (MBRound18.hytale.vexlichdungeon.dungeon.DungeonTile) tile;

            // Convert grid coordinates to world coordinates
            int[] worldPos = dungeonGenerator.gridToWorld(dungeonTile.getGridX(), dungeonTile.getGridZ());
            int worldX = worldPos[0];
            int worldZ = worldPos[1];
            int worldY = dungeonGenerator.getSpawnCenterY();

            try {
              prefabSpawner.spawnTile(dungeonTile, world, worldX, worldY, worldZ);
              log.info("Spawned tile at grid(%d, %d) -> world(%d, %d, %d): %s",
                  dungeonTile.getGridX(), dungeonTile.getGridZ(),
                  worldX, worldY, worldZ,
                  dungeonTile.getPrefabPath());
            } catch (Exception ex) {
              log.error("Failed to spawn tile at grid(%d, %d) world(%d, %d, %d): %s",
                  dungeonTile.getGridX(), dungeonTile.getGridZ(),
                  worldX, worldY, worldZ, ex.getMessage());
            }
          }
        }

        log.info("[GENERATE-COMPLETE] Successfully generated and spawned dungeon for world: %s", world.getName());

        // Mark as generated in persistent storage
        dataStore.markGenerated(world.getName(), System.currentTimeMillis(), tiles.size());
        currentlyGenerating.remove(world.getName());
        log.info("[GENERATE-MARKED] Marked world as generated: %s", world.getName());
        future.complete(null);

      } catch (Exception e) {
        log.error("Exception during dungeon generation: %s", e.getMessage());
        currentlyGenerating.remove(world.getName());
        future.completeExceptionally(new RuntimeException("Dungeon generation failed", e));
      }
    });
    return future;
  }
}
