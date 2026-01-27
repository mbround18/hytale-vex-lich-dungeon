package com.example.hytale.vexlichdungeon.events;

import com.example.hytale.vexlichdungeon.data.DataStore;
import com.example.hytale.vexlichdungeon.dungeon.DungeonGenerator;
import com.example.hytale.vexlichdungeon.logging.PluginLog;
import com.example.hytale.vexlichdungeon.prefab.PrefabSpawner;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventRegistry;
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
   * Registers this handler with the event registry (global listeners like InstancesPlugin).
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventRegistry eventRegistry) {
    eventRegistry.registerGlobal(
        (Class) AddPlayerToWorldEvent.class,
        (java.util.function.Consumer) (Object e) -> onAddPlayerToWorld((AddPlayerToWorldEvent) e));

    eventRegistry.registerGlobal(
        (Class) PlayerReadyEvent.class,
        (java.util.function.Consumer) (Object e) -> onPlayerReady((PlayerReadyEvent) e));

    log.info("Successfully registered dungeon generation handler on EventRegistry (AddPlayerToWorldEvent, PlayerReadyEvent)");
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
      log.info("[EVENT] AddPlayerToWorldEvent fired for world: %s", event.getWorld().getName());
      handleWorld(event.getWorld());
    } catch (Exception e) {
      log.error("Exception in onAddPlayerToWorld: %s", e.getMessage());
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

    // Only process Vex_The_Lich_Dungeon worlds
    if (!worldName.contains("Vex_The_Lich_Dungeon")) {
      return;
    }

    log.info("[GENERATE-CHECK] World: %s", worldName);

    // Check if already generated (persistent storage)
    if (dataStore.isGenerated(worldName)) {
      log.info("[GENERATE-SKIP] World already generated: %s", worldName);
      return;
    }

    // Check if currently generating (in-memory lock)
    if (!currentlyGenerating.add(worldName)) {
      // Another thread is already generating this world
      log.info("[GENERATE-SKIP] World currently being generated: %s", worldName);
      return;
    }

    log.info("[GENERATE-START] Starting dungeon generation for world: %s", worldName);

    // Generate dungeon on the world thread
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

        // Spawn tiles into the world
        Map<?, ?> tiles = result.getTiles();
        int tileSize = 19;

        // Iterate through tiles and spawn them at their grid positions
        for (Object key : tiles.keySet()) {
          Object tile = tiles.get(key);

          if (tile instanceof com.example.hytale.vexlichdungeon.dungeon.DungeonTile) {
            com.example.hytale.vexlichdungeon.dungeon.DungeonTile dungeonTile = (com.example.hytale.vexlichdungeon.dungeon.DungeonTile) tile;

            // Use the tile's actual grid coordinates (not iteration index)
            int worldX = dungeonTile.getGridX() * tileSize;
            int worldZ = dungeonTile.getGridZ() * tileSize;

            try {
              prefabSpawner.spawnTile(dungeonTile, world, worldX, 64, worldZ);
            } catch (Exception ex) {
              log.error("Failed to spawn tile at (%d, %d): %s",
                  worldX, worldZ, ex.getMessage());
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
