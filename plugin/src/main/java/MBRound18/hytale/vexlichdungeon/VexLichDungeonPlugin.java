package MBRound18.hytale.vexlichdungeon;

import MBRound18.hytale.vexlichdungeon.commands.VexChallengeCommand;
import MBRound18.hytale.vexlichdungeon.commands.VexCommand;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.dungeon.DungeonGenerator;
import MBRound18.hytale.vexlichdungeon.dungeon.GenerationConfig;
import MBRound18.hytale.vexlichdungeon.events.DungeonGenerationEventHandler;
import MBRound18.hytale.vexlichdungeon.events.UniversalEventLogger;
import MBRound18.hytale.vexlichdungeon.logging.PluginLog;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.thread.TickingThread;
import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * VexLichDungeon plugin that logs during setup/start and registers /vex
 * commands and event handlers for dungeon generation.
 */
public class VexLichDungeonPlugin extends JavaPlugin {
  private final PluginLog log;
  private DataStore dataStore;
  private DungeonGenerationEventHandler dungeonEventHandler;
  private UniversalEventLogger eventLogger;
  private TickingThread watchdog;

  public VexLichDungeonPlugin(@Nonnull JavaPluginInit init) {
    super(init);
    this.log = PluginLog.forPlugin(this, "VexLichDungeon");
  }

  @Override
  protected void setup() {
    // Register commands
    CommandManager.get().register(new VexCommand());
    CommandManager.get().register(new VexChallengeCommand(log));
    log.lifecycle().atInfo().log("Registered /vex command");

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
    dataStore = new DataStore(log, dataDirectory);
    dataStore.initialize();
    log.lifecycle().atInfo().log("Initialized data store at: %s", dataDirectory);

    // Initialize prefab discovery - loads from ZIP asset bundle
    // The ZIP should be in the same directory as the JAR with the same base name
    // E.g., VexLichDungeon-0.1.0.jar -> VexLichDungeon.zip
    PrefabDiscovery prefabDiscovery = new PrefabDiscovery(log, pluginJarPath);

    // Initialize dungeon generation components using config
    GenerationConfig config = new GenerationConfig();
    DungeonGenerator dungeonGenerator = new DungeonGenerator(config, log, prefabDiscovery);
    PrefabSpawner prefabSpawner = new PrefabSpawner(log, prefabDiscovery.getZipFile());

    // Create and register event handler with data store for concurrency control
    dungeonEventHandler = new DungeonGenerationEventHandler(log, dungeonGenerator, prefabSpawner, dataStore);
    eventLogger = new UniversalEventLogger(log);
    watchdog = createWatchdog();
    log.lifecycle().atInfo().log("Initialized dungeon generation components");
  }

  @Override
  protected void start() {
    log.lifecycle().atInfo().log("Plugin starting up");
    log.info("\u001B[35mVex the Lich awakens... The dungeon trials await brave adventurers!\u001B[0m");

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

        // Register universal event logger (temporary debugging)
        if (eventLogger != null) {
          eventLogger.register(eventBus);
          eventLogger.registerAllKnown(eventBus);
          log.info("Universal event logger registered - watching for all events");
        }

        if (watchdog != null) {
          watchdog.start();
          log.info("Watchdog thread started for dungeon generation polling");
        }
      } else {
        log.error("EventBus is null - cannot register handlers");
      }
    } catch (Exception e) {
      log.error("Failed to register event handlers: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  protected void shutdown() {
    log.lifecycle().atInfo().log("Plugin shutting down");
    if (watchdog != null) {
      watchdog.stop();
    }
    if (dataStore != null) {
      dataStore.saveInstances();
      dataStore.saveConfig();
      log.info("Saved all data before shutdown");
    }
  }

  private TickingThread createWatchdog() {
    return new TickingThread("VexDungeonWatchdog", 2, true) {
      @Override
      protected void tick(float deltaSeconds) {
        if (dungeonEventHandler != null) {
          dungeonEventHandler.pollAndGenerate();
        }
      }

      @Override
      protected void onShutdown() {
        // nothing special; allow thread to exit cleanly
      }
    };
  }
}
