package MBRound18.hytale.dungeonmaster;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class DungeonMasterPlugin extends JavaPlugin {
  private static DungeonMasterPlugin instance;
  private final LoggingHelper log;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private DebugServerConfig config;
  private volatile DebugEventWebServer debugServer;

  // Lifecycle flags
  private final AtomicBoolean debugInitRequested = new AtomicBoolean(false);
  private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

  public DungeonMasterPlugin(@Nonnull JavaPluginInit init) {
    super(init);
    instance = this;
    this.log = Objects.requireNonNull(new LoggingHelper("DungeonMaster"), "log");
  }

  public static DungeonMasterPlugin getInstance() {
    return instance;
  }

  @Override
  protected void setup() {
    loadConfig();
  }

  @Override
  protected void start() {
    if (isDebugServerDisabled()) {
      log.info("DungeonMaster debug server disabled via override.");
      return;
    }

    if (config == null || !config.enabled) {
      log.info("DungeonMaster debug server is disabled in config.");
      return;
    }

    // Prevent double initialization
    if (!debugInitRequested.compareAndSet(false, true)) {
      return;
    }

    Thread initThread = new Thread(this::initializeAsync, "DungeonMaster-Init");
    initThread.setDaemon(true);
    initThread.start();
  }

  private void initializeAsync() {
    try {
      if (shouldAbort())
        return;

      // Optional dependency check
      Object vex = resolveVexPlugin();
      if (vex == null) {
        log.warn("VexLichDungeon plugin not found! Debug server requires it to function.");
        return;
      }

      EventBus eventBus = Objects.requireNonNull(HytaleServer.get().getEventBus(), "eventBus");
      if (shouldAbort())
        return;

      // Initialize Server Instance
      DebugEventWebServer serverInstance = new DebugEventWebServer(log, config);

      // Attempt to bind with retries (Handles TIME_WAIT during dev reloads)
      boolean bound = false;
      int attempts = 0;
      while (!bound && attempts < 5 && !shouldAbort()) {
        try {
          serverInstance.start(eventBus);
          bound = true;
        } catch (BindException e) {
          attempts++;
          log.warn("Port %d in use, retrying in 1s... (Attempt %d/5)", config.port, attempts);
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignored) {
          }
        }
      }

      if (!bound) {
        log.error("Failed to bind debug server port %d after multiple attempts.", config.port);
        return;
      }

      // Success
      this.debugServer = serverInstance;
      String base = serverInstance.getListenAddress();

      log.info("=== DungeonMaster Debug Server Ready ===");
      log.info("Dashboard: %s/api/stats", base);
      log.info("Events:    %s/api/events", base);
      log.info("Health:    %s/api/health", base);
      log.info("========================================");

    } catch (Exception e) {
      log.error("CRITICAL: Failed to start debug server: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  protected void shutdown() {
    shutdownRequested.set(true);

    if (debugServer != null) {
      log.info("Stopping debug server...");
      try {
        debugServer.stop();
      } catch (Exception e) {
        log.error("Error stopping debug server: %s", e.getMessage());
      } finally {
        debugServer = null;
      }
    }

    // Clean up static reference to prevent memory leaks in dev envs
    if (instance == this) {
      instance = null;
    }
  }

  private boolean shouldAbort() {
    return shutdownRequested.get();
  }

  private Object resolveVexPlugin() {
    try {
      Class<?> vexClass = Class.forName("MBRound18.hytale.vexlichdungeon.VexLichDungeonPlugin");
      return vexClass.getMethod("getInstance").invoke(null);
    } catch (ClassNotFoundException e) {
      return null;
    } catch (Exception e) {
      log.error("Error resolving VexLichDungeon plugin: %s", e.getMessage());
      return null;
    }
  }

  private void loadConfig() {
    Path dataDirectory = resolveDataDirectory();

    // Ensure directory exists
    if (!Files.exists(dataDirectory)) {
      try {
        Files.createDirectories(dataDirectory);
      } catch (IOException e) {
        log.error("Failed to create config directory: %s", e.getMessage());
        return;
      }
    }

    Path configPath = dataDirectory.resolve("debug-server.json");

    if (Files.exists(configPath)) {
      try {
        String content = Files.readString(configPath);
        config = gson.fromJson(content, DebugServerConfig.class);
      } catch (IOException | JsonSyntaxException e) {
        log.error("Corrupt config found. Loading defaults. Error: %s", e.getMessage());
        config = new DebugServerConfig();
      }
    } else {
      config = new DebugServerConfig();
      saveConfig(configPath);
    }
  }

  private void saveConfig(Path configPath) {
    try {
      Files.writeString(configPath, gson.toJson(config));
    } catch (IOException e) {
      log.error("Failed to save default config: %s", e.getMessage());
    }
  }

  private Path resolveDataDirectory() {
    // Robustly find a place to store data.
    // Assuming this jar is in /mods/, we want /mods/DungeonMaster/
    try {
      Path pluginJarPath = getFile().toAbsolutePath();
      Path parent = pluginJarPath.getParent();
      return parent != null ? parent.resolve("DungeonMaster") : Path.of("DungeonMaster");
    } catch (Exception e) {
      return Path.of("DungeonMaster");
    }
  }

  private boolean isDebugServerDisabled() {
    if (Boolean.getBoolean("dungeonmaster.debug.disable"))
      return true;
    String env = System.getenv("DUNGEONMASTER_DEBUG_DISABLED");
    return env != null && (env.equals("1") || env.equalsIgnoreCase("true"));
  }
}