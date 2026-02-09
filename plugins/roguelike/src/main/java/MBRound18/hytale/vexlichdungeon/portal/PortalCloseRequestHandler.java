package MBRound18.hytale.vexlichdungeon.portal;

import MBRound18.hytale.vexlichdungeon.events.PortalCloseRequestedEvent;
import com.hypixel.hytale.event.EventBus;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class PortalCloseRequestHandler {
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    Objects.requireNonNull(eventBus, "eventBus").register(
        (Class) PortalCloseRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof PortalCloseRequestedEvent event) {
            PortalManagerSystem.enqueue(() -> PortalManagerSystem.requestPortalClose(event.getPortalId()));
          }
        });
  }
}
