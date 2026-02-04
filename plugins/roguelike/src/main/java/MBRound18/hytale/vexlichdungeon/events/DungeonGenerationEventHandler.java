package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.data.PlayerSpawnTracker;
import MBRound18.hytale.vexlichdungeon.dungeon.DungeonGenerator;
import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import MBRound18.hytale.vexlichdungeon.engine.PortalEngineAdapter;
import MBRound18.hytale.vexlichdungeon.commands.VexChallengeCommand;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import MBRound18.hytale.vexlichdungeon.ui.VexHudSequenceSupport;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for player join events and automatically generates dungeons
 * for VexLichDungeon worlds that haven't been generated yet.
 */
@SuppressWarnings({ "removal", "unused" })
public class DungeonGenerationEventHandler {

  private final LoggingHelper log;
  private final DungeonGenerator dungeonGenerator;
  private final PrefabSpawner prefabSpawner;
  private final DataStore dataStore;
  private final RoguelikeDungeonController roguelikeController;
  private final PortalEngineAdapter engineAdapter;
  private final LoggingHelper eventsLogger;
  private final PlayerSpawnTracker spawnTracker;
  private final Set<String> currentlyGenerating = ConcurrentHashMap.newKeySet();
  private final Set<String> shuttingDownWorlds = ConcurrentHashMap.newKeySet();
  private final Map<String, Long> lastPlayerSeen = new ConcurrentHashMap<>();
  private final Set<UUID> welcomedPlayers = ConcurrentHashMap.newKeySet();
  private static final long EMPTY_INSTANCE_GRACE_MS = 15_000L;

  /**
   * Creates a new dungeon generation event handler.
   * 
   * @param log              Logger for events
   * @param dungeonGenerator Generator for dungeon layouts
   * @param prefabSpawner    Spawner for prefabs into world
   * @param dataStore        Persistent storage for tracking generated worlds
   */
  public DungeonGenerationEventHandler(
      @Nonnull LoggingHelper log,
      @Nonnull DungeonGenerator dungeonGenerator,
      @Nonnull PrefabSpawner prefabSpawner,
      @Nonnull DataStore dataStore,
      @Nonnull RoguelikeDungeonController roguelikeController,
      @Nonnull PortalEngineAdapter engineAdapter,
      @Nonnull LoggingHelper eventsLogger) {
    this.log = log;
    this.dungeonGenerator = dungeonGenerator;
    this.prefabSpawner = prefabSpawner;
    this.dataStore = dataStore;
    this.roguelikeController = roguelikeController;
    this.engineAdapter = engineAdapter;
    this.eventsLogger = eventsLogger;
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

    eventRegistry.registerGlobal(
        (Class) EntityRemoveEvent.class,
        (java.util.function.Consumer) (Object e) -> onEntityRemoved((EntityRemoveEvent) e));

    eventRegistry.registerGlobal(
        (Class) DrainPlayerFromWorldEvent.class,
        (java.util.function.Consumer) (Object e) -> onDrainPlayerFromWorld((DrainPlayerFromWorldEvent) e));

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
        if (world.getPlayerCount() > 0) {
          lastPlayerSeen.put(world.getName(), System.currentTimeMillis());
        }
        world.execute(() -> roguelikeController.pollWorld(world));
        scheduleEmptyInstanceCheck(world);
      }
    } catch (Exception e) {
      log.error("Exception in pollAndGenerate: %s", e.getMessage());
      if (eventsLogger != null) {
        eventsLogger.error("pollAndGenerate failed: " + e.getMessage());
      }
    }
  }

  /**
   * Force-remove all Vex dungeon instances during plugin shutdown.
   */
  public void shutdownAllInstances() {
    try {
      Map<String, World> worlds = Universe.get().getWorlds();
      ArrayList<String> removed = new ArrayList<>();

      for (World world : worlds.values()) {
        String worldName = world.getName();
        if (!worldName.contains("Vex_The_Lich_Dungeon")) {
          continue;
        }
        if (!shuttingDownWorlds.add(worldName)) {
          continue;
        }

        dataStore.clearCurrentPlayers(worldName);
        MBRound18.ImmortalEngine.api.RunSummary summary = engineAdapter.finalizeRun(worldName);
        if (summary != null) {
          dataStore.applyRunSummary(worldName, summary);
        }
        roguelikeController.removeWorldState(worldName);
        spawnTracker.clearWorld(worldName);
        Universe.get().removeWorld(worldName);
        removed.add(worldName);
        log.info("[INSTANCE] Removed dungeon instance during shutdown: %s", worldName);
      }

      if (!removed.isEmpty()) {
        dataStore.removeInstances(removed);
      }
    } catch (Exception e) {
      log.error("Failed to remove dungeon instances during shutdown: %s", e.getMessage());
      if (eventsLogger != null) {
        eventsLogger.error("shutdownAllInstances failed: " + e.getMessage());
      }
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
      lastPlayerSeen.put(worldName, System.currentTimeMillis());

      // Capture first player's actual position if not already tracked
      // Check if we already have a spawn location for this world
      if (!spawnTracker.hasSpawn(worldName)) {
        // Get all players currently in the world
        Collection<PlayerRef> playerRefs = world.getPlayerRefs();

        if (!playerRefs.isEmpty()) {
          PlayerRef playerRef = playerRefs.iterator().next();
          com.hypixel.hytale.server.core.entity.entities.Player player = resolvePlayer(
              java.util.Objects.requireNonNull(playerRef, "playerRef"));
          if (player == null) {
            log.info("[SPAWN] Unable to resolve player entity for spawn capture.");
            return;
          }
          engineAdapter.onPlayerEnter(world, playerRef);
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

        PlayerRef eventPlayerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
        if (eventPlayerRef != null) {
          UUID uuid = eventPlayerRef.getUuid();
          if (world.getName().contains("Vex_The_Lich_Dungeon")) {
            VexChallengeCommand.forceRemovePortal(uuid);
          }
          boolean showWelcome = welcomedPlayers.add(uuid);
          roguelikeController.initializePlayer(world, eventPlayerRef, showWelcome);
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

  private void onEntityRemoved(EntityRemoveEvent event) {
    try {
      roguelikeController.handleEntityRemoved(
          java.util.Objects.requireNonNull(event.getEntity(), "entity"));
    } catch (Exception e) {
      log.error("Exception in onEntityRemoved: %s", e.getMessage());
      if (eventsLogger != null) {
        eventsLogger.error("onEntityRemoved failed: " + e.getMessage());
      }
    }
  }

  private void onDrainPlayerFromWorld(DrainPlayerFromWorldEvent event) {
    try {
      World world = event.getWorld();
      if (world == null) {
        return;
      }
      if (world.getName().contains("Vex_The_Lich_Dungeon")) {
        PlayerRef ref = event.getHolder().getComponent(PlayerRef.getComponentType());
        if (ref != null) {
          roguelikeController.showExitSummary(ref, world);
        }
      }
      scheduleEmptyInstanceCheck(world);
    } catch (Exception e) {
      log.error("Exception in onDrainPlayerFromWorld: %s", e.getMessage());
      if (eventsLogger != null) {
        eventsLogger.error("onDrainPlayerFromWorld failed: " + e.getMessage());
      }
    }
  }

  private void scheduleEmptyInstanceCheck(@Nonnull World world) {
    if (!dataStore.getConfig().isRemoveInstanceWhenEmpty()) {
      return;
    }
    world.execute(() -> tryShutdownEmptyInstance(world));
  }

  @Nullable
  private Player resolvePlayer(@Nonnull PlayerRef playerRef) {
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return null;
    }
    Store<EntityStore> store = ref.getStore();
    return store.getComponent(ref, Player.getComponentType());
  }

  private void tryShutdownEmptyInstance(@Nonnull World world) {
    String worldName = world.getName();
    if (!worldName.contains("Vex_The_Lich_Dungeon")) {
      return;
    }
    if (!dataStore.getConfig().isRemoveInstanceWhenEmpty()) {
      return;
    }
    if (currentlyGenerating.contains(worldName)) {
      return;
    }
    if (!dataStore.isGenerated(worldName)) {
      return;
    }
    if (!spawnTracker.hasSpawn(worldName)) {
      return;
    }
    if (world.getPlayerCount() > 0) {
      return;
    }
    Long lastSeen = lastPlayerSeen.get(worldName);
    if (lastSeen != null && System.currentTimeMillis() - lastSeen < EMPTY_INSTANCE_GRACE_MS) {
      return;
    }
    if (!shuttingDownWorlds.add(worldName)) {
      return;
    }

    dataStore.clearCurrentPlayers(worldName);
    MBRound18.ImmortalEngine.api.RunSummary summary = engineAdapter.finalizeRun(worldName);
    if (summary != null) {
      dataStore.applyRunSummary(worldName, summary);
      announceRunSummary(summary);
      dataStore.getInstance(worldName).ifPresent(this::announceHighScores);
    }
    roguelikeController.removeWorldState(worldName);
    spawnTracker.clearWorld(worldName);
    Universe.get().removeWorld(worldName);
    log.info("[INSTANCE] Removed empty dungeon instance: %s", worldName);
  }

  private void announceRunSummary(@Nonnull MBRound18.ImmortalEngine.api.RunSummary summary) {
    String header = EngineLang.t(
        "event.vex.summary.header",
        summary.getTotalScore(),
        summary.getTotalKills(),
        summary.getRoomsCleared(),
        summary.getRoundsCleared(),
        summary.getSafeRoomsVisited());

    String statsLine = EngineLang.t(
        "customUI.vexSummary.statsLine",
        summary.getTotalScore(),
        summary.getTotalKills(),
        summary.getRoomsCleared(),
        summary.getRoundsCleared(),
        summary.getSafeRoomsVisited());

    ArrayList<MBRound18.ImmortalEngine.api.RunSummary.PlayerSummary> players = new ArrayList<>(summary.getPlayers());
    players.sort(Comparator.comparingInt(MBRound18.ImmortalEngine.api.RunSummary.PlayerSummary::getScore)
        .reversed());

    for (MBRound18.ImmortalEngine.api.RunSummary.PlayerSummary player : players) {
      try {
        UUID uuid = UUID.fromString(player.getPlayerId());
        PlayerRef ref = Universe.get().getPlayer(
            java.util.Objects.requireNonNull(uuid, "uuid"));
        if (ref == null) {
          continue;
        }
        ref.sendMessage(Message.raw(java.util.Objects.requireNonNull(header, "header")));
        StringBuilder bodyBuilder = new StringBuilder();
        for (MBRound18.ImmortalEngine.api.RunSummary.PlayerSummary progress : players) {
          String name = progress.getDisplayName() != null ? progress.getDisplayName() : progress.getPlayerId();
          String line = EngineLang.t(
              "customUI.vexSummary.leaderboardDetail",
              name,
              progress.getScore(),
              progress.getKills());
          bodyBuilder.append(line).append("\n");
          ref.sendMessage(Message.raw(java.util.Objects.requireNonNull(line, "line")));
        }

        String leaderboardBody = bodyBuilder.toString();
        VexHudSequenceSupport.showSummarySequence(ref,
            java.util.Objects.requireNonNull(statsLine, "statsLine"),
            java.util.Objects.requireNonNull(bodyBuilder.toString(), "body"),
            java.util.Objects.requireNonNull(leaderboardBody, "leaderboard"));
      } catch (IllegalArgumentException ignored) {
        // Ignore malformed UUIDs
      }
    }
  }

  private void announceHighScores(@Nonnull MBRound18.hytale.vexlichdungeon.data.DungeonInstanceData current) {
    if (!dataStore.getConfig().isEnableLeaderboard()) {
      return;
    }

    int bestGroupScore = 0;
    int bestPlayerScore = 0;

    for (MBRound18.hytale.vexlichdungeon.data.DungeonInstanceData instance : dataStore.getAllInstances()) {
      if (instance == current ||
          (instance.getWorldName() != null && instance.getWorldName().equals(current.getWorldName()))) {
        continue;
      }
      bestGroupScore = Math.max(bestGroupScore, instance.getTotalScore());
      for (MBRound18.hytale.vexlichdungeon.data.DungeonInstanceData.PlayerProgress progress : instance
          .getPlayerProgress().values()) {
        bestPlayerScore = Math.max(bestPlayerScore, progress.getScore());
      }
    }

    if (current.getTotalScore() > bestGroupScore) {
      ArrayList<String> names = new ArrayList<>();
      for (MBRound18.hytale.vexlichdungeon.data.DungeonInstanceData.PlayerProgress progress : current
          .getPlayerProgress().values()) {
        if (progress.getPlayerName() != null) {
          names.add(progress.getPlayerName());
        } else if (progress.getPlayerUuid() != null) {
          names.add(progress.getPlayerUuid());
        }
      }
      String teamNames = names.isEmpty()
          ? EngineLang.t("common.unknown")
          : String.join(", ", names);
      Universe.get().sendMessage(Message.raw(java.util.Objects.requireNonNull(EngineLang.t(
          "event.vex.record.group",
          current.getTotalScore(),
          current.getRoundsCleared(),
          teamNames), "message")));
    }

    for (MBRound18.hytale.vexlichdungeon.data.DungeonInstanceData.PlayerProgress progress : current.getPlayerProgress()
        .values()) {
      if (progress.getScore() > bestPlayerScore) {
        String name = progress.getPlayerName() != null ? progress.getPlayerName() : progress.getPlayerUuid();
        Universe.get().sendMessage(Message.raw(java.util.Objects.requireNonNull(EngineLang.t(
            "event.vex.record.player",
            name,
            progress.getScore()), "message")));
        bestPlayerScore = progress.getScore();
      }
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
        log.info("Starting roguelike initialization for world: %s", world.getName());
        roguelikeController.initializeWorld(world);

        log.info("[GENERATE-COMPLETE] Successfully initialized roguelike dungeon for world: %s", world.getName());

        // Mark as generated in persistent storage
        dataStore.markGenerated(world.getName(), System.currentTimeMillis(), 1);
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
