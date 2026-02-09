package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import MBRound18.ImmortalEngine.api.events.EventDispatcher;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InstanceTeardownHandler {
  private static final long GRACE_MS = 15_000L;
  private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1,
      Thread.ofVirtual().name("vex-instance-teardown-", 0).factory());
  private static final long SWEEP_INTERVAL_MS = 5_000L;

  private final LoggingHelper log;
  private final LoggingHelper eventsLogger;
  private final DataStore dataStore;
  private final RoguelikeDungeonController roguelikeController;
  private final Set<String> shuttingDownWorlds = ConcurrentHashMap.newKeySet();
  private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
  private final AtomicBoolean sweepStarted = new AtomicBoolean(false);

  public InstanceTeardownHandler(
      @Nonnull LoggingHelper log,
      @Nonnull DataStore dataStore,
      @Nullable RoguelikeDungeonController roguelikeController,
      @Nullable LoggingHelper eventsLogger) {
    this.log = log;
    this.dataStore = dataStore;
    this.roguelikeController = roguelikeController;
    this.eventsLogger = eventsLogger;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventRegistry eventRegistry) {
    eventRegistry.registerGlobal(
        (Class) AddPlayerToWorldEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof AddPlayerToWorldEvent event) {
            onAddPlayerToWorld(event);
          }
        });
    eventRegistry.registerGlobal(
        (Class) DrainPlayerFromWorldEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof DrainPlayerFromWorldEvent event) {
            onDrainPlayerFromWorld(event);
          }
        });
    startSweep();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    eventBus.register(
        (Class) AddPlayerToWorldEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof AddPlayerToWorldEvent event) {
            onAddPlayerToWorld(event);
          }
        });
    eventBus.register(
        (Class) DrainPlayerFromWorldEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof DrainPlayerFromWorldEvent event) {
            onDrainPlayerFromWorld(event);
          }
        });
    startSweep();
  }

  private void onDrainPlayerFromWorld(@Nonnull DrainPlayerFromWorldEvent event) {
    try {
      World world = event.getWorld();
      if (world == null) {
        return;
      }
      log.info("[INSTANCE] DrainPlayerFromWorldEvent for %s (players=%d)", world.getName(),
          world.getPlayerCount());
      scheduleCheck(world);
    } catch (Exception e) {
      log.error("Instance teardown handler failed: %s", e.getMessage());
      if (eventsLogger != null) {
        eventsLogger.error("Instance teardown failed: " + e.getMessage());
      }
    }
  }

  private void onAddPlayerToWorld(@Nonnull AddPlayerToWorldEvent event) {
    try {
      World world = event.getWorld();
      String worldName = world.getName();
      if (worldName == null || !worldName.contains("Vex_The_Lich_Dungeon")) {
        return;
      }
      lastSeen.put(worldName, System.currentTimeMillis());
      shuttingDownWorlds.remove(worldName);
    } catch (Exception e) {
      log.warn("Instance add-player handler failed: %s", e.getMessage());
    }
  }

  private void scheduleCheck(@Nonnull World world) {
    String worldName = world.getName();
    if (worldName == null || !worldName.contains("Vex_The_Lich_Dungeon")) {
      return;
    }
    log.info("[INSTANCE] Scheduling teardown check for %s", worldName);
    lastSeen.put(worldName, System.currentTimeMillis());
    SCHEDULER.schedule(() -> shutdownIfEmpty(worldName), GRACE_MS, TimeUnit.MILLISECONDS);
  }

  private void startSweep() {
    if (!sweepStarted.compareAndSet(false, true)) {
      return;
    }
    SCHEDULER.scheduleAtFixedRate(this::sweepWorlds, SWEEP_INTERVAL_MS, SWEEP_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }

  private void sweepWorlds() {
    try {
      for (World world : Universe.get().getWorlds().values()) {
        if (world == null) {
          continue;
        }
        String worldName = world.getName();
        if (worldName == null || !worldName.contains("Vex_The_Lich_Dungeon")) {
          continue;
        }
        world.execute(() -> {
          if (world.getPlayerCount() > 0) {
            lastSeen.put(worldName, System.currentTimeMillis());
            return;
          }
          Long seen = lastSeen.get(worldName);
          if (seen == null || System.currentTimeMillis() - seen >= GRACE_MS) {
            shutdownIfEmptyOnWorldThread(world, worldName);
          }
        });
      }
    } catch (Exception e) {
      log.warn("Instance sweep failed: %s", e.getMessage());
    }
  }

  public void cleanupOrphanedInstancesOnStartup() {
    try {
      for (World world : Universe.get().getWorlds().values()) {
        if (world == null) {
          continue;
        }
        String worldName = world.getName();
        if (worldName == null || !worldName.contains("Vex_The_Lich_Dungeon")) {
          continue;
        }
        if (dataStore.getInstance(worldName).isPresent()) {
          continue;
        }
        log.info("[INSTANCE] Orphaned instance world detected (not in data store): %s", worldName);
        SCHEDULER.schedule(() -> shutdownIfEmpty(worldName), 0L, TimeUnit.MILLISECONDS);
      }
    } catch (Exception e) {
      log.warn("Startup orphan cleanup failed: %s", e.getMessage());
    }
  }

  private void shutdownIfEmpty(@Nonnull String worldName) {
    try {
      World world = Universe.get().getWorld(worldName);
      if (world == null) {
        log.warn("[INSTANCE] Teardown skipped; world missing: %s", worldName);
        return;
      }
      world.execute(() -> shutdownIfEmptyOnWorldThread(world, worldName));
    } catch (Exception e) {
      log.error("Failed to teardown instance %s: %s", worldName, e.getMessage());
      if (eventsLogger != null) {
        eventsLogger.error("Instance teardown failed for " + worldName + ": " + e.getMessage());
      }
    }
  }

  private void shutdownIfEmptyOnWorldThread(@Nonnull World world, @Nonnull String worldName) {
    try {
      if (world.getPlayerCount() > 0) {
        log.info("[INSTANCE] Teardown skipped; players still present in %s", worldName);
        return;
      }
      if (!shuttingDownWorlds.add(worldName)) {
        return;
      }

      dispatchEvent(new InstanceTeardownStartedEvent(worldName));

      dataStore.clearCurrentPlayers(worldName);
      dispatchFinalizeRun(worldName, "teardown");
      if (roguelikeController != null) {
        roguelikeController.removeWorldState(worldName);
      }
      dataStore.removeInstances(Objects.requireNonNull(java.util.List.of(worldName), "worldNames"));

      world.getWorldConfig().setDeleteOnRemove(true);
      Universe.get().removeWorld(worldName);
      WorldEventQueue.get().releaseWorld(worldName);
      log.info("[INSTANCE] Removed empty dungeon instance (teardown handler): %s", worldName);
      dispatchEvent(new InstanceTeardownCompletedEvent(worldName));
    } catch (Exception e) {
      log.warn("[INSTANCE] World-thread teardown failed for %s: %s", worldName, e.getMessage());
    }
  }

  private void dispatchEvent(@Nullable IEvent<Void> event) {
    if (event == null) {
      return;
    }
    WorldEventQueue.get().dispatchGlobal(event);
  }

  private void dispatchFinalizeRun(@Nonnull String worldName, @Nullable String reason) {
    EventDispatcher.dispatch(HytaleServer.get().getEventBus(),
        new RunFinalizeRequestedEvent(worldName, reason));
  }

  public void shutdown() {
    try {
      SCHEDULER.shutdownNow();
      shuttingDownWorlds.clear();
      lastSeen.clear();
    } catch (Exception e) {
      log.warn("Instance teardown scheduler shutdown failed: %s", e.getMessage());
    }
  }
}
