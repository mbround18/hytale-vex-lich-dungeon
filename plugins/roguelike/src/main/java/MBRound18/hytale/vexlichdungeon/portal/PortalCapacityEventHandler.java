package MBRound18.hytale.vexlichdungeon.portal;

import MBRound18.hytale.vexlichdungeon.events.InstanceCapacityReachedEvent;
import MBRound18.ImmortalEngine.api.portal.PortalPlacementRegistry;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class PortalCapacityEventHandler {
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    Objects.requireNonNull(eventBus, "eventBus").register(
        (Class) InstanceCapacityReachedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof InstanceCapacityReachedEvent event) {
            World world = Universe.get().getWorld(event.getWorldName());
            if (world != null) {
              world.execute(() -> PortalPlacementRegistry.closePortals(event.getInstanceTemplate()));
              return;
            }
            PortalManagerSystem.enqueue(() -> PortalPlacementRegistry.closePortals(event.getInstanceTemplate()));
          }
        });
  }
}
