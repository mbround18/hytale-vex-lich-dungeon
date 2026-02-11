package MBRound18.hytale.vexlichdungeon.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Objects;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import MBRound18.hytale.vexlichdungeon.events.PortalCountdownHudUpdateRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexDemoHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexLeaderboardHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexScoreHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexSummaryHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexWelcomeHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.portal.PortalManagerSystem;

public class VexHudEventHandlerTest {
  @BeforeClass
  public static void configureLogging() {
    System.setProperty("java.util.logging.manager",
        "com.hypixel.hytale.logger.backend.HytaleLogManager");
    System.setProperty("vex.welcomeHud.enabled", "true");
  }

  private PlayerRef buildPlayerRef(String name) {
    return new PlayerRef(Objects.requireNonNull(name, "name"));
  }

  @Test
  public void welcomeHud_passesPlayerRefToSink() {
    PlayerRef playerRef = Objects.requireNonNull(buildPlayerRef("Tester"), "playerRef");
    CapturingSink sink = new CapturingSink();

    VexHudEventHandler handler = new VexHudEventHandler((ref, action) -> {
      action.run();
      return true;
    }, sink);
    handler.onEvent(new VexWelcomeHudRequestedEvent(playerRef, "Hi"));

    assertSame(playerRef, sink.lastPlayerRef);
    assertEquals("Hi", sink.lastBodyText);
  }

  @Test
  public void welcomeHud_skipsWhenPlayerInvalid() {
    PlayerRef playerRef = Objects.requireNonNull(buildPlayerRef("Tester"), "playerRef");
    playerRef.setValid(false);
    CapturingSink sink = new CapturingSink();
    CountingExecutor executor = new CountingExecutor(true);

    VexHudEventHandler handler = new VexHudEventHandler(executor, sink);
    handler.onEvent(new VexWelcomeHudRequestedEvent(playerRef, "Hi"));

    assertEquals(0, executor.runCount);
    assertEquals(null, sink.lastPlayerRef);
  }

  @Test
  public void welcomeHud_skipsWhenExecutorDeclines() {
    PlayerRef playerRef = Objects.requireNonNull(buildPlayerRef("Tester"), "playerRef");
    CapturingSink sink = new CapturingSink();
    CountingExecutor executor = new CountingExecutor(false);

    VexHudEventHandler handler = new VexHudEventHandler(executor, sink);
    handler.onEvent(new VexWelcomeHudRequestedEvent(playerRef, "Hi"));

    assertEquals(1, executor.runCount);
    assertEquals(null, sink.lastPlayerRef);
  }

  @Test
  public void scoreHud_passesPlayerRefToSink() {
    PlayerRef playerRef = Objects.requireNonNull(buildPlayerRef("Tester"), "playerRef");
    CapturingSink sink = new CapturingSink();

    VexHudEventHandler handler = new VexHudEventHandler((ref, action) -> {
      action.run();
      return true;
    }, sink);
    handler.onEvent(new VexScoreHudRequestedEvent(playerRef, 10, 5, 1, "party"));

    assertSame(playerRef, sink.lastPlayerRef);
    assertEquals(10, sink.lastInstanceScore);
    assertEquals(5, sink.lastPlayerScore);
    assertEquals(1, sink.lastDelta);
    assertEquals("party", sink.lastPartyList);
  }

  @Test
  public void summaryHud_passesPlayerRefToSink() {
    PlayerRef playerRef = Objects.requireNonNull(buildPlayerRef("Tester"), "playerRef");
    CapturingSink sink = new CapturingSink();

    VexHudEventHandler handler = new VexHudEventHandler((ref, action) -> {
      action.run();
      return true;
    }, sink);
    handler.onEvent(new VexSummaryHudRequestedEvent(playerRef, "stats", "summary"));

    assertSame(playerRef, sink.lastPlayerRef);
    assertEquals("stats", sink.lastStatsLine);
    assertEquals("summary", sink.lastSummaryLine);
  }

  @Test
  public void leaderboardHud_passesPlayerRefToSink() {
    PlayerRef playerRef = Objects.requireNonNull(buildPlayerRef("Tester"), "playerRef");
    CapturingSink sink = new CapturingSink();

    VexHudEventHandler handler = new VexHudEventHandler((ref, action) -> {
      action.run();
      return true;
    }, sink);
    handler.onEvent(new VexLeaderboardHudRequestedEvent(playerRef, "1. You"));

    assertSame(playerRef, sink.lastPlayerRef);
    assertEquals("1. You", sink.lastLeaderboardText);
  }

  @Test
  public void demoHud_passesPlayerRefToSink() {
    PlayerRef playerRef = Objects.requireNonNull(buildPlayerRef("Tester"), "playerRef");
    CapturingSink sink = new CapturingSink();

    VexHudEventHandler handler = new VexHudEventHandler((ref, action) -> {
      action.run();
      return true;
    }, sink);
    handler.onEvent(new VexDemoHudRequestedEvent(playerRef, "Score", "Time", "DBG"));

    assertSame(playerRef, sink.lastPlayerRef);
    assertEquals("Score", sink.lastScoreText);
    assertEquals("Time", sink.lastTimerText);
    assertEquals("DBG", sink.lastDebugStat);
  }

  @Test
  public void portalCountdownHud_passesPlayerRefToSink() {
    PlayerRef playerRef = Objects.requireNonNull(buildPlayerRef("Tester"), "playerRef");
    CapturingSink sink = new CapturingSink();
    UUID portalId = Objects.requireNonNull(UUID.randomUUID(), "portalId");
    PortalManagerSystem.registerPortalOwner(portalId,
        Objects.requireNonNull(playerRef.getUuid(), "playerId"));

    VexHudEventHandler handler = new VexHudEventHandler((ref, action) -> {
      action.run();
      return true;
    }, sink);
    handler.onEvent(new PortalCountdownHudUpdateRequestedEvent(playerRef, portalId, "00:10", "X:1"));

    assertSame(playerRef, sink.lastPlayerRef);
    assertEquals("00:10", sink.lastTimeLeft);
    assertEquals("X:1", sink.lastLocationText);
  }

  private static final class CapturingSink implements VexHudEventHandler.HudUpdateSink {
    private PlayerRef lastPlayerRef;
    private String lastBodyText;
    private int lastInstanceScore;
    private int lastPlayerScore;
    private int lastDelta;
    private String lastPartyList;
    private String lastStatsLine;
    private String lastSummaryLine;
    private String lastLeaderboardText;
    private String lastScoreText;
    private String lastTimerText;
    private String lastDebugStat;
    private String lastTimeLeft;
    private String lastLocationText;

    @Override
    public void updateWelcome(PlayerRef playerRef, String bodyText) {
      this.lastPlayerRef = playerRef;
      this.lastBodyText = bodyText;
    }

    @Override
    public void updateScore(PlayerRef playerRef, int instanceScore, int playerScore, int delta, String partyList) {
      this.lastPlayerRef = playerRef;
      this.lastInstanceScore = instanceScore;
      this.lastPlayerScore = playerScore;
      this.lastDelta = delta;
      this.lastPartyList = partyList;
    }

    @Override
    public void updateSummary(PlayerRef playerRef, String statsLine, String summaryLine) {
      this.lastPlayerRef = playerRef;
      this.lastStatsLine = statsLine;
      this.lastSummaryLine = summaryLine;
    }

    @Override
    public void updateLeaderboard(PlayerRef playerRef, String leaderboardText) {
      this.lastPlayerRef = playerRef;
      this.lastLeaderboardText = leaderboardText;
    }

    @Override
    public void updateDemo(PlayerRef playerRef, String scoreText, String timerText, String debugStat) {
      this.lastPlayerRef = playerRef;
      this.lastScoreText = scoreText;
      this.lastTimerText = timerText;
      this.lastDebugStat = debugStat;
    }

    @Override
    public void updatePortalCountdown(PlayerRef playerRef, String timeLeft, String locationText) {
      this.lastPlayerRef = playerRef;
      this.lastTimeLeft = timeLeft;
      this.lastLocationText = locationText;
    }
  }

  private static final class CountingExecutor implements VexHudEventHandler.UiExecutor {
    private final boolean runAction;
    private int runCount;

    private CountingExecutor(boolean runAction) {
      this.runAction = runAction;
    }

    @Override
    public boolean run(PlayerRef playerRef, Runnable handler) {
      runCount++;
      if (runAction) {
        handler.run();
      }
      return runAction;
    }
  }
}
