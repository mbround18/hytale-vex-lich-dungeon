package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabEdgeIndex;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabEdgeIndexBuilder;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabEdgeIndexStorage;
import MBRound18.ImmortalEngine.api.prefab.StitchIndex;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.asset.AssetPackRegisterEvent;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

public final class AssetPackBootstrapHandler {
  private static final long DEBOUNCE_MS = 750L;

  private final LoggingHelper log;
  private final PrefabDiscovery discovery;
  private final PrefabSpawner spawner;
  private final RoguelikeDungeonController controller;
  private final Path dataDirectory;
  private final ScheduledExecutorService scheduler;
  private final AtomicBoolean scheduled = new AtomicBoolean(false);
  private final AtomicBoolean startedLogged = new AtomicBoolean(false);
  private volatile ScheduledFuture<?> pending;

  public AssetPackBootstrapHandler(
      @Nonnull LoggingHelper log,
      @Nonnull PrefabDiscovery discovery,
      @Nonnull PrefabSpawner spawner,
      @Nonnull RoguelikeDungeonController controller,
      @Nonnull Path dataDirectory) {
    this.log = Objects.requireNonNull(log, "log");
    this.discovery = Objects.requireNonNull(discovery, "discovery");
    this.spawner = Objects.requireNonNull(spawner, "spawner");
    this.controller = Objects.requireNonNull(controller, "controller");
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "vex-asset-bootstrap");
      t.setDaemon(true);
      return t;
    });
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    eventBus.register(
        (Class) AssetPackRegisterEvent.class,
        (java.util.function.Consumer) (Object e) -> onAssetPackRegistered());
  }

  public void scheduleInitialBootstrap() {
    try {
      if (!PrefabStore.get().getAllAssetPrefabPaths().isEmpty()) {
        scheduleBootstrap("startup");
      }
    } catch (Exception e) {
      scheduleBootstrap("startup");
    }
  }

  private void onAssetPackRegistered() {
    scheduleBootstrap("asset-pack");
  }

  private void scheduleBootstrap(@Nonnull String reason) {
    ScheduledFuture<?> existing = pending;
    if (existing != null) {
      existing.cancel(false);
    }
    pending = scheduler.schedule(() -> bootstrap(reason), DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    scheduled.set(true);
  }

  private void bootstrap(@Nonnull String reason) {
    if (!scheduled.compareAndSet(true, false)) {
      return;
    }
    log.fine("[ASSETS] Bootstrapping prefabs after %s", reason);
    CompletableFuture.runAsync(() -> {
      discovery.refresh();
      PrefabEdgeIndex edgeIndex = PrefabEdgeIndexBuilder.build(
          Objects.requireNonNull(discovery, "discovery"),
          Objects.requireNonNull(spawner, "spawner"),
          Objects.requireNonNull(log, "log"));
      controller.setEdgeIndex(edgeIndex);
      StitchIndex stitchIndex = edgeIndex != null ? edgeIndex.toStitchIndex() : null;
      controller.setStitchIndex(stitchIndex);
      log.fine("[ASSETS] Bootstrap rebuilt indexes (edge=%s, stitch=%s)", edgeIndex == null ? "null" : "ready",
          stitchIndex == null ? "null" : "ready");
      if (edgeIndex != null) {
        PrefabEdgeIndexStorage.save(
            Objects.requireNonNull(dataDirectory, "dataDirectory"),
            edgeIndex,
            Objects.requireNonNull(log, "log"));
      }
      if (startedLogged.compareAndSet(false, true)) {
        log.info("[ASSETS] Startup complete; prefab indexes ready.");
      }
    });
  }

  public void shutdown() {
    ScheduledFuture<?> existing = pending;
    if (existing != null) {
      existing.cancel(false);
    }
    scheduler.shutdownNow();
  }
}
