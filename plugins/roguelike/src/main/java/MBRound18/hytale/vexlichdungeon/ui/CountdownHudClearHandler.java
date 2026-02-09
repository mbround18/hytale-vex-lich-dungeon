package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.vexlichdungeon.commands.VexChallengeCommand;
import MBRound18.hytale.vexlichdungeon.events.CountdownHudClearRequestedEvent;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Objects;
import javax.annotation.Nonnull;

public final class CountdownHudClearHandler {
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    Objects.requireNonNull(eventBus, "eventBus").register(
        (Class) CountdownHudClearRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof CountdownHudClearRequestedEvent event) {
            VexPortalCountdownHud.closeFor(event.getPlayerRef());
          }
        });
  }
}
