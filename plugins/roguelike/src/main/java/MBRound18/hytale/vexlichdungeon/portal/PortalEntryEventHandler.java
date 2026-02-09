package MBRound18.hytale.vexlichdungeon.portal;

import MBRound18.ImmortalEngine.api.events.WorldEnteredEvent;
import com.hypixel.hytale.event.EventBus;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class PortalEntryEventHandler {
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    Objects.requireNonNull(eventBus, "eventBus").register(
        (Class) WorldEnteredEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof WorldEnteredEvent event) {
            var world = event.getWorld();
            world.execute(() -> PortalManagerSystem.handlePortalEntry(
                event.getPlayerRef(),
                world));
          }
        });
  }
}
