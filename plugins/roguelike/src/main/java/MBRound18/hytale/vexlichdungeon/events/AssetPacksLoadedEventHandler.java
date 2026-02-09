package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.AssetPacksLoadedEvent;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabEdgeIndex;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabEdgeIndexBuilder;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabEdgeIndexStorage;
import MBRound18.ImmortalEngine.api.prefab.StitchIndex;
import com.hypixel.hytale.event.EventBus;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nonnull;

/**
 * Listens for AssetPacksLoadedEvent from the engine plugin and rebuilds
 * dungeon prefab indexes in response.
 */
public final class AssetPacksLoadedEventHandler {
  private final LoggingHelper log;
  private final PrefabDiscovery discovery;
  private final PrefabSpawner spawner;
  private final RoguelikeDungeonController controller;
  private final Path dataDirectory;
  private final ScheduledExecutorService scheduler;

  public AssetPacksLoadedEventHandler(
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
      Thread t = new Thread(r, "vex-assets-loaded-handler");
      t.setDaemon(true);
      return t;
    });
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    eventBus.register(
        (Class) AssetPacksLoadedEvent.class,
        (java.util.function.Consumer) (Object e) -> onAssetPacksLoaded());
  }

  private void onAssetPacksLoaded() {
    log.fine("[ASSETS] AssetPacksLoaded event received");
    CompletableFuture.runAsync(() -> {
      discovery.refresh();
      PrefabEdgeIndex edgeIndex = PrefabEdgeIndexBuilder.build(
          Objects.requireNonNull(discovery, "discovery"),
          Objects.requireNonNull(spawner, "spawner"),
          Objects.requireNonNull(log, "log"));
      controller.setEdgeIndex(edgeIndex);
      StitchIndex stitchIndex = edgeIndex != null ? edgeIndex.toStitchIndex() : null;
      controller.setStitchIndex(stitchIndex);
      log.info("[ASSETS] Rebuilt indexes (edge=%s, stitch=%s)", edgeIndex == null ? "null" : "ready",
          stitchIndex == null ? "null" : "ready");
      if (edgeIndex != null) {
        PrefabEdgeIndexStorage.save(
            Objects.requireNonNull(dataDirectory, "dataDirectory"),
            edgeIndex,
            Objects.requireNonNull(log, "log"));
      }
    });
  }

  public void shutdown() {
    scheduler.shutdownNow();
  }
}
