package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class EntitySpawnTrackingHandler {
  private final RoguelikeDungeonController controller;

  public EntitySpawnTrackingHandler(@Nonnull RoguelikeDungeonController controller) {
    this.controller = Objects.requireNonNull(controller, "controller");
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    Objects.requireNonNull(eventBus, "eventBus").register(
        (Class) EntitySpawnedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof EntitySpawnedEvent event) {
            World world = event.getWorld();
            world.execute(() -> controller.trackEntitySpawned(event));
          }
        });
  }
}
