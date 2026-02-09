package MBRound18.hytale.vexlichdungeon.events;

import static org.junit.Assert.assertEquals;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class EventHubMockTest {
  @Test
  public void testHub_dispatchesRegisteredEvent() {
    TestEventHub hub = new TestEventHub();
    AtomicReference<PlayerRef> seen = new AtomicReference<>();

    hub.register(CountdownHudClearRequestedEvent.class, event -> seen.set(event.getPlayerRef()));

    UUID playerId = Objects.requireNonNull(UUID.randomUUID(), "playerId");
    PlayerRef playerRef = new PlayerRef("Tester", playerId);
    hub.dispatch(new CountdownHudClearRequestedEvent(playerRef));

    assertEquals(playerRef, seen.get());
  }
}
