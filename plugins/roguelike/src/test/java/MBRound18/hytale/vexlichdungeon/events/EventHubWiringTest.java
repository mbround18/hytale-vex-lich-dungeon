package MBRound18.hytale.vexlichdungeon.events;

import static org.junit.Assert.assertEquals;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class EventHubWiringTest {
  @Test
  public void eventBus_dispatchesCountdownClearEvent() {
    TestEventHub eventBus = new TestEventHub();
    AtomicReference<PlayerRef> seenPlayer = new AtomicReference<>();

    eventBus.register(CountdownHudClearRequestedEvent.class, event -> seenPlayer.set(event.getPlayerRef()));

    UUID playerId = Objects.requireNonNull(UUID.randomUUID(), "playerId");
    PlayerRef playerRef = new PlayerRef("Tester", playerId);
    eventBus.dispatch(new CountdownHudClearRequestedEvent(playerRef));

    assertEquals(playerRef, seenPlayer.get());
  }
}
