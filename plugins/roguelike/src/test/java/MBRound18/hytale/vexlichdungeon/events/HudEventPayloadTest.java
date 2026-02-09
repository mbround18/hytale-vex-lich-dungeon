package MBRound18.hytale.vexlichdungeon.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.Test;

public class HudEventPayloadTest {
  @Test
  public void welcomeHudPayload_containsPlayerAndBody() {
    UUID playerId = Objects.requireNonNull(UUID.randomUUID(), "playerId");
    Map<String, Object> data = VexWelcomeHudRequestedEvent.buildPayload(playerId, "Tester", "Welcome");

    @SuppressWarnings("unchecked")
    Map<String, Object> player = (Map<String, Object>) data.get("player");
    assertNotNull(player);
    assertEquals(playerId, player.get("uuid"));
    assertEquals("Tester", player.get("name"));
    assertEquals("Welcome", data.get("bodyText"));
  }

  @Test
  public void scoreHudPayload_containsScores() {
    UUID playerId = Objects.requireNonNull(UUID.randomUUID(), "playerId");
    Map<String, Object> data = VexScoreHudRequestedEvent.buildPayload(playerId, "Tester", 120, 45, 5, "Party");

    @SuppressWarnings("unchecked")
    Map<String, Object> player = (Map<String, Object>) data.get("player");
    assertNotNull(player);
    assertEquals(playerId, player.get("uuid"));
    assertEquals(120, data.get("instanceScore"));
    assertEquals(45, data.get("playerScore"));
    assertEquals(5, data.get("delta"));
    assertEquals("Party", data.get("partyList"));
  }

  @Test
  public void summaryHudPayload_containsLines() {
    UUID playerId = Objects.requireNonNull(UUID.randomUUID(), "playerId");
    Map<String, Object> data = VexSummaryHudRequestedEvent.buildPayload(playerId, "Tester", "Stats", "Summary");

    assertEquals("Stats", data.get("statsLine"));
    assertEquals("Summary", data.get("summaryLine"));
  }

  @Test
  public void leaderboardHudPayload_containsText() {
    UUID playerId = Objects.requireNonNull(UUID.randomUUID(), "playerId");
    Map<String, Object> data = VexLeaderboardHudRequestedEvent.buildPayload(playerId, "Tester", "1. You");

    assertEquals("1. You", data.get("leaderboardText"));
  }

  @Test
  public void demoHudPayload_containsStats() {
    UUID playerId = Objects.requireNonNull(UUID.randomUUID(), "playerId");
    Map<String, Object> data = VexDemoHudRequestedEvent.buildPayload(playerId, "Tester", "Score: 10", "Time: 1:00",
        "DBG");

    assertEquals("Score: 10", data.get("scoreText"));
    assertEquals("Time: 1:00", data.get("timerText"));
    assertEquals("DBG", data.get("debugStat"));
  }

  @Test
  public void portalCountdownHudPayload_containsTimeAndLocation() {
    UUID playerId = Objects.requireNonNull(UUID.randomUUID(), "playerId");
    UUID portalId = Objects.requireNonNull(UUID.randomUUID(), "portalId");
    Map<String, Object> data = PortalCountdownHudUpdateRequestedEvent.buildPayload(playerId, "Tester", portalId,
        "00:10", "X: 1 Y: 2 Z: 3");

    assertEquals("00:10", data.get("timeLeft"));
    assertEquals("X: 1 Y: 2 Z: 3", data.get("locationText"));
    assertEquals(portalId, data.get("portalId"));
  }
}
