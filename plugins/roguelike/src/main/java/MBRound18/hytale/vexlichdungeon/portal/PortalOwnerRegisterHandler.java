package MBRound18.hytale.vexlichdungeon.portal;

import MBRound18.hytale.vexlichdungeon.events.PortalOwnerRegisteredEvent;
import com.hypixel.hytale.event.EventBus;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class PortalOwnerRegisterHandler {
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    Objects.requireNonNull(eventBus, "eventBus").register(
        (Class) PortalOwnerRegisteredEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof PortalOwnerRegisteredEvent event) {
            PortalManagerSystem
                .enqueue(() -> PortalManagerSystem.registerPortalOwner(event.getPortalId(), event.getOwnerId()));
          }
        });
  }
}
