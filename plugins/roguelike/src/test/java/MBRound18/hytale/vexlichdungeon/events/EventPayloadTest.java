package MBRound18.hytale.vexlichdungeon.events;

import static org.junit.Assert.assertEquals;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import javax.annotation.Nonnull;
import MBRound18.ImmortalEngine.api.RunSummary;

public class EventPayloadTest {
  private Map<String, Object> unwrapPayload(Object payload) {
    if (payload instanceof CompletableFuture<?> future) {
      Object resolved = future.join();
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) resolved;
      return data;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) payload;
    return data;
  }

  @Test
  public void countdownHudClearPayload_containsPlayerMeta() {
    @Nonnull
    UUID playerId = Objects.requireNonNull(UUID.randomUUID(), "playerId");
    PlayerRef playerRef = new PlayerRef("Tester", playerId);
    CountdownHudClearRequestedEvent event = new CountdownHudClearRequestedEvent(playerRef);

    Map<String, Object> data = unwrapPayload(event.toPayload());

    @SuppressWarnings("unchecked")
    Map<String, Object> player = (Map<String, Object>) data.get("player");
    assertEquals(playerId, player.get("uuid"));
    assertEquals("Tester", player.get("name"));
  }

  @Test
  public void portalClosePayload_containsPortalId() {
    @Nonnull
    UUID portalId = Objects.requireNonNull(UUID.randomUUID(), "portalId");
    PortalCloseRequestedEvent event = new PortalCloseRequestedEvent(portalId);

    Map<String, Object> data = unwrapPayload(event.toPayload());

    assertEquals(portalId, data.get("portalId"));
  }

  @Test
  public void portalOwnerPayload_containsPortalAndOwnerId() {
    @Nonnull
    UUID portalId = Objects.requireNonNull(UUID.randomUUID(), "portalId");
    @Nonnull
    UUID ownerId = Objects.requireNonNull(UUID.randomUUID(), "ownerId");
    PortalOwnerRegisteredEvent event = new PortalOwnerRegisteredEvent(portalId, ownerId);

    Map<String, Object> data = unwrapPayload(event.toPayload());

    assertEquals(portalId, data.get("portalId"));
    assertEquals(ownerId, data.get("ownerId"));
  }

  @Test
  public void instanceCapacityPayload_containsFields() {
    InstanceCapacityReachedEvent event = new InstanceCapacityReachedEvent("vex", "instance-1", 6, 4);

    Map<String, Object> data = unwrapPayload(event.toPayload());

    assertEquals("vex", data.get("instanceTemplate"));
    assertEquals("instance-1", data.get("worldName"));
    assertEquals(6, data.get("maxPlayers"));
    assertEquals(4, data.get("currentPlayers"));
  }

  @Test
  public void instanceTeardownStartedPayload_containsWorldName() {
    InstanceTeardownStartedEvent event = new InstanceTeardownStartedEvent("instance-2");

    Map<String, Object> data = unwrapPayload(event.toPayload());
    assertEquals("instance-2", data.get("worldName"));
  }

  @Test
  public void instanceTeardownCompletedPayload_containsWorldName() {
    InstanceTeardownCompletedEvent event = new InstanceTeardownCompletedEvent("instance-3");

    Map<String, Object> data = unwrapPayload(event.toPayload());
    assertEquals("instance-3", data.get("worldName"));
  }

  @Test
  public void runFinalizeRequestedPayload_containsWorldAndReason() {
    RunFinalizeRequestedEvent event = new RunFinalizeRequestedEvent("instance-7", "timeout");

    Map<String, Object> data = unwrapPayload(event.toPayload());
    assertEquals("instance-7", data.get("worldName"));
    assertEquals("timeout", data.get("reason"));
  }

  @Test
  public void runFinalizedPayload_containsSummary() {
    RunSummary summary = new RunSummary("instance-9", 1500, 42, 8, 2, 1, java.util.List.of());
    RunFinalizedEvent event = new RunFinalizedEvent("instance-9", summary, null);

    Map<String, Object> data = unwrapPayload(event.toPayload());
    assertEquals("instance-9", data.get("worldName"));
    assertEquals(summary, data.get("summary"));
    assertEquals(null, data.get("reason"));
  }
}
