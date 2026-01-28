package MBRound18.hytale.vexlichdungeon;

import MBRound18.hytale.vexlichdungeon.commands.VexCommand;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.dungeon.DungeonGenerator;
import MBRound18.hytale.vexlichdungeon.dungeon.GenerationConfig;
import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import MBRound18.hytale.vexlichdungeon.engine.PortalEngineAdapter;
import MBRound18.hytale.vexlichdungeon.events.DungeonGenerationEventHandler;
import MBRound18.hytale.vexlichdungeon.events.UniversalEventLogger;
import MBRound18.PortalEngine.api.logging.InternalLogger;
import MBRound18.PortalEngine.api.logging.EngineLog;
import MBRound18.PortalEngine.api.logging.LoggingController;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import MBRound18.PortalEngine.api.ui.HudRegistry;
import MBRound18.PortalEngine.api.ui.UiRegistry;
import MBRound18.PortalEngine.api.ui.UiTemplate;
import MBRound18.PortalEngine.api.ui.UiTemplateLoader;
import MBRound18.PortalEngine.api.prefab.StitchIndexBuilder;
import MBRound18.hytale.vexlichdungeon.ui.HudController;
import MBRound18.hytale.vexlichdungeon.ui.UIController;
import MBRound18.hytale.vexlichdungeon.ui.UiAssetResolver;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.thread.TickingThread;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

/**
 * VexLichDungeon plugin that logs during setup/start and registers /vex
 * commands and event handlers for dungeon generation.
 */
public class VexLichDungeonPlugin extends JavaPlugin {
  private final EngineLog log;
  private DataStore dataStore;
  private DungeonGenerationEventHandler dungeonEventHandler;
  @SuppressWarnings("unused")
  private UniversalEventLogger eventLogger;
  private TickingThread watchdog;
  private InternalLogger internalLogger;
  private InternalLogger eventsLogger;
  private PrefabSpawner prefabSpawner;

  public VexLichDungeonPlugin(@Nonnull JavaPluginInit init) {
    super(init);
    this.log = LoggingController.forPlugin(this, "VexLichDungeon");
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
    dataStore = new DataStore(log, dataDirectory);
    dataStore.initialize();
    log.lifecycle().atInfo().log("Initialized data store at: %s", dataDirectory);

    Path templatesPath = dataDirectory.resolve("ui-templates.json");
    Path assetsZipPath = resolveAssetsZipPath(pluginJarPath, modsDirectory);
    UiAssetResolver.setAssetsZipPath(assetsZipPath);
    MBRound18.PortalEngine.api.i18n.EngineLang.setAssetsZipPath(assetsZipPath);
    StitchIndexBuilder.loadOrBuild(assetsZipPath, dataDirectory.resolve("index.db"), log);
    boolean loadedTemplates = false;
    try {
      syncTemplatesFile(templatesPath, "ui-templates.json");
      if (Files.exists(templatesPath)) {
        UiTemplateLoader.loadFromPath(templatesPath);
        loadedTemplates = true;
      } else {
        loadedTemplates = UiTemplateLoader.loadFromResource(getClass().getClassLoader(),
            "ui-templates.json");
      }
    } catch (Exception e) {
      log.error("Failed to load ui-templates.json: %s", e.getMessage());
    }
    if (!loadedTemplates) {
      UIController.registerDefaults();
      HudController.registerDefaults();
      log.lifecycle().atInfo().log("Registered Vex UI and HUD templates (defaults)");
    } else {
      log.lifecycle().atInfo().log("Registered Vex UI and HUD templates (data-driven)");
    }

    preflightUiAssets(pluginJarPath, modsDirectory);

    // Register commands
    CommandManager.get().register(new VexCommand(dataStore, log));
    log.lifecycle().atInfo().log("Registered /vex command");

    internalLogger = new InternalLogger(dataDirectory);
    internalLogger.start("Internal logger started");
    internalLogger.info("Plugin setup complete");

    eventsLogger = new InternalLogger(dataDirectory, "events");
    eventsLogger.start("Event logger started");

    // Initialize prefab discovery - loads from ZIP asset bundle
    // The ZIP should be in the same directory as the JAR with the same base name
    // E.g., VexLichDungeon-0.1.0.jar -> VexLichDungeon.zip
    PrefabDiscovery prefabDiscovery = new PrefabDiscovery(log, pluginJarPath);
    PortalEngineAdapter engineAdapter = new PortalEngineAdapter();

    // Initialize dungeon generation components using config
    GenerationConfig config = new GenerationConfig();
    DungeonGenerator dungeonGenerator = new DungeonGenerator(config, log, prefabDiscovery);
    prefabSpawner = new PrefabSpawner(log, prefabDiscovery.getZipFile(), config);
    RoguelikeDungeonController roguelikeController = new RoguelikeDungeonController(
        log, dungeonGenerator, prefabDiscovery, prefabSpawner, dataStore, engineAdapter, eventsLogger);

    // Create and register event handler with data store for concurrency control
    dungeonEventHandler = new DungeonGenerationEventHandler(
        log, dungeonGenerator, prefabSpawner, dataStore, roguelikeController, engineAdapter, eventsLogger);
    eventLogger = new UniversalEventLogger(log);
    watchdog = createWatchdog();
    log.lifecycle().atInfo().log("Initialized dungeon generation components");
  }

  @Override
  protected void start() {
    log.lifecycle().atInfo().log("Plugin starting up");
    log.info("\u001B[35mVex the Lich awakens... The dungeon trials await brave adventurers!\u001B[0m");

    logAssetPacks();

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
      } else {
        log.error("EventBus is null - cannot register handlers");
      }
    } catch (Exception e) {
      log.error("Failed to register event handlers: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  private void logAssetPacks() {
    try {
      AssetModule assetModule = AssetModule.get();
      if (assetModule == null) {
        log.warn("AssetModule not available yet; cannot list asset packs.");
        return;
      }
      List<AssetPack> packs = assetModule.getAssetPacks();
      log.info("Loaded asset packs: %d", packs.size());
      for (AssetPack pack : packs) {
        log.info("- Pack: %s (root=%s)", pack.getName(), pack.getRoot());
      }
    } catch (Exception e) {
      log.warn("Failed to log asset packs: %s", e.getMessage());
    }
  }

  private void syncTemplatesFile(@Nonnull Path templatesPath, @Nonnull String resourcePath) {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (stream == null) {
        return;
      }
      byte[] resourceBytes = stream.readAllBytes();
      if (Files.exists(templatesPath)) {
        byte[] existingBytes = Files.readAllBytes(templatesPath);
        if (Arrays.equals(existingBytes, resourceBytes)) {
          return;
        }
      }
      Files.write(templatesPath, resourceBytes);
      log.lifecycle().atInfo().log("Updated %s from bundled defaults", templatesPath.getFileName());
    } catch (Exception e) {
      log.warn("Failed to sync %s: %s", templatesPath.getFileName(), e.getMessage());
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
    if (internalLogger != null) {
      internalLogger.info("Plugin shutting down");
      internalLogger.close();
    }
    if (eventsLogger != null) {
      eventsLogger.info("Plugin shutting down");
      eventsLogger.close();
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
        MBRound18.PortalEngine.api.portal.PortalPlacementRegistry.tick();
      }

      @Override
      protected void onShutdown() {
        // nothing special; allow thread to exit cleanly
      }
    };
  }

  private void preflightUiAssets(@Nonnull Path pluginJarPath, @Nonnull Path modsDirectory) {
    Path zipPath = resolveAssetsZipPath(pluginJarPath, modsDirectory);

    List<UiTemplate> templates = new ArrayList<>();
    templates.addAll(UiRegistry.getTemplates().values());
    templates.addAll(HudRegistry.getTemplates().values());

    if (templates.isEmpty()) {
      log.warn("UI preflight skipped: no templates registered.");
      return;
    }

    if (!Files.exists(zipPath)) {
      log.error("UI preflight failed: assets zip not found at %s", zipPath);
      return;
    }

    List<String> missing = new ArrayList<>();
    try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
      for (UiTemplate template : templates) {
        String entryPath = "Common/UI/" + template.getPath();
        ZipEntry entry = zipFile.getEntry(entryPath);
        if (entry == null) {
          missing.add(entryPath);
        }
      }
    } catch (Exception e) {
      log.error("UI preflight failed reading %s: %s", zipPath, e.getMessage());
      return;
    }

    if (missing.isEmpty()) {
      log.lifecycle().atInfo().log("UI preflight OK: all UI documents found in %s", zipPath.getFileName());
      return;
    }

    log.error("UI preflight failed: missing %d UI document(s) in %s", missing.size(), zipPath.getFileName());
    for (String path : missing) {
      log.error("- Missing UI: %s", path);
    }
  }

  private Path resolveAssetsZipPath(@Nonnull Path pluginJarPath, @Nonnull Path modsDirectory) {
    String jarName = pluginJarPath.getFileName().toString();
    String baseFull = jarName.endsWith(".jar")
        ? jarName.substring(0, jarName.length() - 4)
        : jarName;
    String versionedZip = baseFull + ".zip";
    Path zipPath = modsDirectory.resolve(versionedZip);
    if (Files.exists(zipPath)) {
      return zipPath;
    }

    String legacyBase = baseFull.split("-")[0];
    String legacyZip = legacyBase + ".zip";
    Path legacyPath = modsDirectory.resolve(legacyZip);
    if (Files.exists(legacyPath)) {
      return legacyPath;
    }

    return zipPath;
  }
}
