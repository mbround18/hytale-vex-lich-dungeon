package MBRound18.hytale.vexlichdungeon;

import MBRound18.hytale.vexlichdungeon.commands.VexCommand;
import MBRound18.hytale.vexlichdungeon.commands.VexChallengeCommand;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.dungeon.DungeonGenerator;
import MBRound18.hytale.vexlichdungeon.dungeon.GenerationConfig;
import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import MBRound18.hytale.vexlichdungeon.engine.PortalEngineAdapter;
import MBRound18.hytale.vexlichdungeon.events.DungeonGenerationEventHandler;
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
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.thread.TickingThread;
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
        new PrefabDiscovery(Objects.requireNonNull(log, "log"), pluginJarPath, unpackedRoot),
        "prefabDiscovery");
    PortalEngineAdapter engineAdapter = new PortalEngineAdapter();

    // Initialize dungeon generation components using config
    GenerationConfig config = new GenerationConfig();
    DungeonGenerator dungeonGenerator = new DungeonGenerator(config, Objects.requireNonNull(log, "log"),
        Objects.requireNonNull(prefabDiscovery, "prefabDiscovery"));
    prefabSpawner = new PrefabSpawner(
        Objects.requireNonNull(log, "log"),
        prefabDiscovery.getZipFile(),
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
        engineAdapter,
        Objects.requireNonNull(eventsLogger, "eventsLogger"),
        stitchIndex,
        lootService);

    // Create and register event handler with data store for concurrency control
    dungeonEventHandler = new DungeonGenerationEventHandler(
        Objects.requireNonNull(log, "log"),
        dungeonGenerator,
        Objects.requireNonNull(prefabSpawner, "prefabSpawner"),
        Objects.requireNonNull(dataStore, "dataStore"),
        roguelikeController,
        engineAdapter,
        Objects.requireNonNull(eventsLogger, "eventsLogger"));
    eventLogger = new UniversalEventLogger(Objects.requireNonNull(log, "log"));
    getChunkStoreRegistry().registerSystem(new PortalManagerSystem());
    watchdog = createWatchdog();
    log.info("Initialized dungeon generation components");
  }

  @Override
  protected void start() {
    log.info("Plugin starting up");
    log.info("\u001B[35mVex the Lich awakens... The dungeon trials await brave adventurers!\u001B[0m");

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
    if (dataStore != null) {
      dataStore.saveInstances();
      dataStore.saveConfig();
      dataStore.savePortalPlacements();
      log.info("Saved all data before shutdown");
    }
    if (prefabSpawner != null) {
      prefabSpawner.clearCaches();
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
