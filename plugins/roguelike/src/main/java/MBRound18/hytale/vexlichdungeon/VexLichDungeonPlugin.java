package MBRound18.hytale.vexlichdungeon;

import MBRound18.hytale.vexlichdungeon.commands.VexCommand;
import MBRound18.hytale.vexlichdungeon.commands.VexChallengeCommand;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.events.AssetPacksLoadedEventHandler;
import MBRound18.hytale.vexlichdungeon.dungeon.DungeonGenerator;
import MBRound18.hytale.vexlichdungeon.dungeon.GenerationConfig;
import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import MBRound18.hytale.vexlichdungeon.engine.PortalEngineAdapter;
import MBRound18.hytale.vexlichdungeon.events.AssetPackBootstrapHandler;
import MBRound18.hytale.vexlichdungeon.events.DungeonGenerationEventHandler;
import MBRound18.hytale.vexlichdungeon.events.EngineAdapterEventHandler;
import MBRound18.hytale.vexlichdungeon.events.EntitySpawnTrackingHandler;
import MBRound18.hytale.vexlichdungeon.events.InstanceTeardownHandler;
import MBRound18.hytale.vexlichdungeon.events.UniversalEventLogger;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabHookRegistry;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabPlacementHook;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import MBRound18.hytale.vexlichdungeon.loot.LootCatalog;
import MBRound18.hytale.vexlichdungeon.loot.LootService;
import MBRound18.hytale.vexlichdungeon.loot.LootTableConfig;
import MBRound18.hytale.vexlichdungeon.loot.LootTableLoader;
import MBRound18.ImmortalEngine.api.prefab.StitchIndex;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabStitchIndexBuilder;

import MBRound18.hytale.vexlichdungeon.portal.PortalManagerSystem;
import MBRound18.hytale.vexlichdungeon.portal.PortalEntryEventHandler;
import MBRound18.hytale.vexlichdungeon.portal.PortalCapacityEventHandler;
import MBRound18.hytale.vexlichdungeon.portal.PortalOwnerRegisterHandler;
import MBRound18.hytale.vexlichdungeon.portal.PortalCloseRequestHandler;
import MBRound18.hytale.vexlichdungeon.events.RoomTileSpawnRequestHandler;
import MBRound18.hytale.vexlichdungeon.events.RoomEnemiesSpawnRequestHandler;
import MBRound18.hytale.vexlichdungeon.ui.VexHudEventHandler;
import MBRound18.hytale.vexlichdungeon.ui.CountdownHudClearHandler;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.thread.TickingThread;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * VexLichDungeon plugin that logs during setup/start and registers /vex
 * commands and event handlers for dungeon generation.
 */
public class VexLichDungeonPlugin extends JavaPlugin {
  private static volatile VexLichDungeonPlugin instance;
  private final LoggingHelper log;
  private DataStore dataStore;
  private DungeonGenerationEventHandler dungeonEventHandler;
  @SuppressWarnings("unused")
  private UniversalEventLogger eventLogger;
  private TickingThread watchdog;
  private LoggingHelper eventsLogger;
  private PrefabSpawner prefabSpawner;
  private PrefabDiscovery prefabDiscovery;
  private AssetPackBootstrapHandler assetPackBootstrapHandler;
  private AssetPacksLoadedEventHandler assetPacksLoadedEventHandler;
  private InstanceTeardownHandler instanceTeardownHandler;
  private EngineAdapterEventHandler engineAdapterEventHandler;
  private EntitySpawnTrackingHandler entitySpawnTrackingHandler;
  private VexHudEventHandler hudEventHandler;
  private CountdownHudClearHandler countdownHudClearHandler;
  private PortalEntryEventHandler portalEntryEventHandler;
  private PortalCapacityEventHandler portalCapacityEventHandler;
  private PortalOwnerRegisterHandler portalOwnerRegisterHandler;
  private PortalCloseRequestHandler portalCloseRequestHandler;
  private RoomTileSpawnRequestHandler roomTileSpawnRequestHandler;
  private RoomEnemiesSpawnRequestHandler roomEnemiesSpawnRequestHandler;

  public VexLichDungeonPlugin(@Nonnull JavaPluginInit init) {
    super(init);
    instance = this;
    this.log = Objects.requireNonNull(new LoggingHelper("VexLichDungeon"), "log");
  }

  @Nullable
  public static VexLichDungeonPlugin getInstance() {
    return instance;
  }

  @Nullable
  public PrefabSpawner getPrefabSpawner() {
    return prefabSpawner;
  }

  @Nullable
  public PrefabDiscovery getPrefabDiscovery() {
    return prefabDiscovery;
  }

  @Nullable
  public DataStore getDataStore() {
    return dataStore;
  }

  @Nullable
  public DungeonGenerationEventHandler getDungeonEventHandler() {
    return dungeonEventHandler;
  }

  @Override
  protected void setup() {
    // Initialize data store (creates directories and loads config)
    // Plugin jar is at: Server/mods/VexLichDungeon-0.1.0.jar
    // Data directory: Server/mods/VexLichDungeon/
    Path pluginJarPath = getFile().toAbsolutePath();
    Path modsDirectory = pluginJarPath.getParent();

    if (modsDirectory == null) {
      log.error("Could not determine mods directory from plugin jar path: %s", pluginJarPath);
      throw new RuntimeException("Plugin jar has no parent directory - cannot initialize");
    }

    Path dataDirectory = modsDirectory.resolve("VexLichDungeon");
    dataStore = new DataStore(Objects.requireNonNull(log, "log"),
        Objects.requireNonNull(dataDirectory, "dataDirectory"));
    dataStore.initialize();
    log.fine("[HUD-BOOT] EngineHud adapter setup complete");
    MBRound18.hytale.vexlichdungeon.ui.VexUiCatalog.registerDefaults();
    CommandManager.get().register(new VexCommand());
    log.info("Registered Vex UI and HUD templates (code-driven)");
    eventsLogger = new LoggingHelper("VexLichDungeon-Events");

    // Initialize prefab discovery - loads from ZIP asset bundle
    // The ZIP should be in the same directory as the JAR with the same base name
    // E.g., VexLichDungeon-0.1.0.jar -> VexLichDungeon.zip
    Path unpackedRoot = Path.of("data", "unpacked");
    if (!Files.exists(unpackedRoot)) {
      unpackedRoot = null;
    }
    prefabDiscovery = Objects.requireNonNull(
        new PrefabDiscovery(Objects.requireNonNull(log, "log"), unpackedRoot),
        "prefabDiscovery");
    PortalEngineAdapter engineAdapter = new PortalEngineAdapter();
    engineAdapterEventHandler = new EngineAdapterEventHandler(engineAdapter);
    hudEventHandler = new VexHudEventHandler();
    countdownHudClearHandler = new CountdownHudClearHandler();
    portalEntryEventHandler = new PortalEntryEventHandler();
    portalCapacityEventHandler = new PortalCapacityEventHandler();
    portalOwnerRegisterHandler = new PortalOwnerRegisterHandler();
    portalCloseRequestHandler = new PortalCloseRequestHandler();

    // Initialize dungeon generation components using config
    GenerationConfig config = new GenerationConfig();
    DungeonGenerator dungeonGenerator = new DungeonGenerator(config, Objects.requireNonNull(log, "log"),
        Objects.requireNonNull(prefabDiscovery, "prefabDiscovery"));
    prefabSpawner = new PrefabSpawner(
        Objects.requireNonNull(log, "log"),
        config,
        unpackedRoot);
    StitchIndex stitchIndex = PrefabStitchIndexBuilder.build(
        Objects.requireNonNull(prefabDiscovery, "prefabDiscovery"),
        Objects.requireNonNull(prefabSpawner, "prefabSpawner"),
        Objects.requireNonNull(log, "log"));
    PrefabHookRegistry.register(new PrefabPlacementHook());
    Path lootTablePath = dataDirectory.resolve("loot_tables.json");
    LootService lootService = buildLootService(
        Objects.requireNonNull(lootTablePath, "lootTablePath"),
        Objects.requireNonNull(log, "log"),
        generatorSeed(config),
        Objects.requireNonNull(dataDirectory, "dataDirectory"));
    RoguelikeDungeonController roguelikeController = new RoguelikeDungeonController(
        Objects.requireNonNull(log, "log"),
        dungeonGenerator,
        Objects.requireNonNull(prefabDiscovery, "prefabDiscovery"),
        Objects.requireNonNull(prefabSpawner, "prefabSpawner"),
        Objects.requireNonNull(dataStore, "dataStore"),
        Objects.requireNonNull(engineAdapter, "engineAdapter"),
        Objects.requireNonNull(eventsLogger, "eventsLogger"),
        stitchIndex,
        lootService);

    assetPackBootstrapHandler = new AssetPackBootstrapHandler(
        Objects.requireNonNull(log, "log"),
        Objects.requireNonNull(prefabDiscovery, "prefabDiscovery"),
        Objects.requireNonNull(prefabSpawner, "prefabSpawner"),
        roguelikeController,
        Objects.requireNonNull(dataDirectory, "dataDirectory"));

    assetPacksLoadedEventHandler = new AssetPacksLoadedEventHandler(
        Objects.requireNonNull(log, "log"),
        Objects.requireNonNull(prefabDiscovery, "prefabDiscovery"),
        Objects.requireNonNull(prefabSpawner, "prefabSpawner"),
        roguelikeController,
        Objects.requireNonNull(dataDirectory, "dataDirectory"));

    // Initialize event-driven spawning request handlers with actual dependencies
    roomTileSpawnRequestHandler = new RoomTileSpawnRequestHandler(
        Objects.requireNonNull(prefabSpawner, "prefabSpawner"));
    roomEnemiesSpawnRequestHandler = new RoomEnemiesSpawnRequestHandler(
        roguelikeController);

    // Create and register event handler with data store for concurrency control
    dungeonEventHandler = new DungeonGenerationEventHandler(
        Objects.requireNonNull(log, "log"),
        dungeonGenerator,
        Objects.requireNonNull(prefabSpawner, "prefabSpawner"),
        Objects.requireNonNull(dataStore, "dataStore"),
        roguelikeController,
        engineAdapter,
        Objects.requireNonNull(eventsLogger, "eventsLogger"));
    instanceTeardownHandler = new InstanceTeardownHandler(
        Objects.requireNonNull(log, "log"),
        Objects.requireNonNull(dataStore, "dataStore"),
        roguelikeController,
        eventsLogger);
    eventLogger = new UniversalEventLogger(Objects.requireNonNull(log, "log"));
    entitySpawnTrackingHandler = new EntitySpawnTrackingHandler(roguelikeController);
    getChunkStoreRegistry().registerSystem(new PortalManagerSystem());
    watchdog = createWatchdog();
    log.info("Initialized dungeon generation components");
  }

  @Override
  protected void start() {
    log.info("Plugin starting up");
    log.info("\u001B[35mVex the Lich awakens... The dungeon trials await brave adventurers!\u001B[0m");

    // Purge any stale dungeon instances from previous server runs
    purgeExistingDungeonInstances();

    if (dataStore != null) {
      VexChallengeCommand.cleanupPersistedPortals(dataStore);
    }

    // Register event handler with the global event bus
    try {
      EventBus eventBus = HytaleServer.get().getEventBus();

      if (eventBus != null) {
        // Register dungeon generation handler
        if (dungeonEventHandler != null) {
          dungeonEventHandler.register(eventBus);
          dungeonEventHandler.register(getEventRegistry());
          log.info("Dungeon generation event handler registered with event bus and event registry");
        } else {
          log.error("DungeonEventHandler is null - not registering");
        }

        if (instanceTeardownHandler != null) {
          instanceTeardownHandler.register(eventBus);
          instanceTeardownHandler.register(getEventRegistry());
          log.info("Instance teardown handler registered with event registry");
          instanceTeardownHandler.cleanupOrphanedInstancesOnStartup();
        }

        if (engineAdapterEventHandler != null) {
          engineAdapterEventHandler.register(eventBus);
        }

        if (entitySpawnTrackingHandler != null) {
          entitySpawnTrackingHandler.register(eventBus);
        }

        if (hudEventHandler != null) {
          hudEventHandler.register(eventBus);
        }

        if (countdownHudClearHandler != null) {
          countdownHudClearHandler.register(eventBus);
        }

        if (portalEntryEventHandler != null) {
          portalEntryEventHandler.register(eventBus);
        }

        if (portalCapacityEventHandler != null) {
          portalCapacityEventHandler.register(eventBus);
        }

        if (portalOwnerRegisterHandler != null) {
          portalOwnerRegisterHandler.register(eventBus);
        }

        if (portalCloseRequestHandler != null) {
          portalCloseRequestHandler.register(eventBus);
        }

        if (roomTileSpawnRequestHandler != null) {
          roomTileSpawnRequestHandler.register(eventBus);
          log.info("Room tile spawn request handler registered with event bus");
        }

        if (roomEnemiesSpawnRequestHandler != null) {
          roomEnemiesSpawnRequestHandler.register(eventBus);
          log.info("Room enemies spawn request handler registered with event bus");
        }

        if (assetPackBootstrapHandler != null) {
          assetPackBootstrapHandler.register(eventBus);
          assetPackBootstrapHandler.scheduleInitialBootstrap();
        }

        if (assetPacksLoadedEventHandler != null) {
          assetPacksLoadedEventHandler.register(eventBus);
        }

        // UniversalEventLogger disabled (too noisy)

        if (watchdog != null) {
          watchdog.start();
          log.info("Watchdog thread started for dungeon generation polling");
        }
      }
    } catch (Exception e) {
      log.error("Failed to register event handlers: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  private LootService buildLootService(@Nonnull Path lootTablePath, @Nonnull LoggingHelper log,
      long seed, @Nonnull Path dataDirectory) {
    LootTableLoader loader = new LootTableLoader();
    LootTableConfig table = loader.load(lootTablePath, log);
    if (table == null) {
      return null;
    }
    LootCatalog catalog = new LootCatalog();
    Path itemsRoot = Path.of("data", "assets", "Server", "Item", "Items");
    catalog.load(itemsRoot, table, log);
    return new LootService(table, catalog, seed, log);
  }

  private long generatorSeed(@Nonnull GenerationConfig config) {
    return config.getSeed();
  }

  @Override
  protected void shutdown() {
    log.info("Plugin shutting down");
    if (watchdog != null) {
      watchdog.stop();
    }

    // Clean up active dungeon instances before saving
    cleanupActiveDungeonInstances();

    if (dataStore != null) {
      dataStore.saveInstances();
      dataStore.saveConfig();
      dataStore.savePortalPlacements();
      log.info("Saved all data before shutdown");
    }
    if (prefabSpawner != null) {
      prefabSpawner.clearCaches();
    }
    PortalManagerSystem.shutdown();
    MBRound18.hytale.vexlichdungeon.events.WorldEventQueue.get().shutdown();
  }

  private void cleanupActiveDungeonInstances() {
    try {
      Universe universe = Universe.get();
      if (universe == null) {
        return;
      }

      java.util.List<String> instancesToRemove = new java.util.ArrayList<>();
      for (World world : universe.getWorlds().values()) {
        if (world == null) {
          continue;
        }
        String worldName = world.getName();
        if (worldName != null && worldName.contains("Vex_The_Lich_Dungeon")) {
          instancesToRemove.add(worldName);
          log.info("[INSTANCE] Marking dungeon instance for cleanup on shutdown: %s", worldName);
        }
      }

      // Remove instances from data store
      if (!instancesToRemove.isEmpty() && dataStore != null) {
        dataStore.removeInstances(instancesToRemove);
        log.info("[INSTANCE] Removed %d dungeon instances from data store on shutdown", instancesToRemove.size());
      }

      // Remove worlds from universe
      for (String worldName : instancesToRemove) {
        try {
          World world = universe.getWorld(worldName);
          if (world != null) {
            world.getWorldConfig().setDeleteOnRemove(true);
            universe.removeWorld(java.util.Objects.requireNonNull(worldName, "worldName"));
            log.info("[INSTANCE] Removed world from universe on shutdown: %s", worldName);
          }
        } catch (Exception e) {
          log.warn("[INSTANCE] Failed to remove world %s on shutdown: %s", worldName, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Dungeon instance cleanup on shutdown failed: %s", e.getMessage());
    }
  }

  private void purgeExistingDungeonInstances() {
    try {
      Universe universe = Universe.get();
      if (universe == null) {
        return;
      }

      java.util.List<String> instancesToRemove = new java.util.ArrayList<>();
      for (World world : universe.getWorlds().values()) {
        if (world == null) {
          continue;
        }
        String worldName = world.getName();
        if (worldName != null && worldName.contains("Vex_The_Lich_Dungeon")) {
          instancesToRemove.add(worldName);
          log.info("[INSTANCE] Purging dungeon instance on startup: %s", worldName);
        }
      }

      // Remove instances from data store
      if (!instancesToRemove.isEmpty() && dataStore != null) {
        dataStore.removeInstances(instancesToRemove);
        log.info("[INSTANCE] Removed %d dungeon instances from data store on startup", instancesToRemove.size());
      }

      // Remove worlds from universe
      for (String worldName : instancesToRemove) {
        try {
          World world = universe.getWorld(worldName);
          if (world != null) {
            world.getWorldConfig().setDeleteOnRemove(true);
            universe.removeWorld(java.util.Objects.requireNonNull(worldName, "worldName"));
            log.info("[INSTANCE] Purged world from universe on startup: %s", worldName);
          }
        } catch (Exception e) {
          log.warn("[INSTANCE] Failed to purge world %s on startup: %s", worldName, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Dungeon instance purge on startup failed: %s", e.getMessage());
    }
  }

  private TickingThread createWatchdog() {
    return new TickingThread("VexDungeonWatchdog", 2, true) {
      @Override
      protected void tick(float deltaSeconds) {
        if (dungeonEventHandler != null) {
          dungeonEventHandler.pollAndGenerate();
        }
        MBRound18.ImmortalEngine.api.portal.PortalPlacementRegistry.tick();
      }

      @Override
      protected void onShutdown() {
        // nothing special; allow thread to exit cleanly
      }
    };
  }

}
