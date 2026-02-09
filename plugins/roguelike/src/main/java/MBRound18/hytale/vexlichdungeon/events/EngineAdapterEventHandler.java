package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.RunSummary;
import MBRound18.hytale.vexlichdungeon.engine.PortalEngineAdapter;
import MBRound18.ImmortalEngine.api.events.EventDispatcher;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class EngineAdapterEventHandler {
  private final PortalEngineAdapter engineAdapter;

  public EngineAdapterEventHandler(@Nonnull PortalEngineAdapter engineAdapter) {
    this.engineAdapter = Objects.requireNonNull(engineAdapter, "engineAdapter");
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    Objects.requireNonNull(eventBus, "eventBus").register(
        (Class) InstanceEnteredEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof InstanceEnteredEvent event) {
            World world = event.getWorld();
            world.execute(() -> engineAdapter.onPlayerEnter(world, event.getPlayerRef()));
          }
        });

    eventBus.register(
        (Class) EntityEliminatedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof EntityEliminatedEvent event) {
            java.util.UUID killerId = event.getKillerId();
            if (killerId != null) {
              World world = event.getWorld();
              world.execute(() -> engineAdapter.onKill(world.getName(),
                  killerId.toString(),
                  "Enemy",
                  event.getPoints()));
            }
          }
        });

    eventBus.register(
        (Class) RoomClearedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof RoomClearedEvent event) {
            World world = event.getWorld();
            world.execute(() -> engineAdapter.onRoomCleared(world.getName()));
          }
        });

    eventBus.register(
        (Class) RoundClearedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof RoundClearedEvent event) {
            World world = event.getWorld();
            world.execute(() -> engineAdapter.onRoundCleared(world.getName()));
          }
        });

    eventBus.register(
        (Class) SafeRoomVisitedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof SafeRoomVisitedEvent event) {
            World world = event.getWorld();
            world.execute(() -> engineAdapter.onSafeRoomVisited(world.getName()));
          }
        });

    eventBus.register(
        (Class) RunFinalizeRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof RunFinalizeRequestedEvent event) {
            RunSummary summary = engineAdapter.finalizeRun(event.getWorldName());
            if (summary != null) {
              EventDispatcher.dispatch(HytaleServer.get().getEventBus(),
                  new RunFinalizedEvent(event.getWorldName(), summary, event.getReason()));
            }
          }
        });
  }
}
