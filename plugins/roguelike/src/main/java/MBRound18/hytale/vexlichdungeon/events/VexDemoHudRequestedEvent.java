package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class VexDemoHudRequestedEvent extends DebugEvent {
  @Nonnull
  private final PlayerRef playerRef;
  @Nonnull
  private final String scoreText;
  @Nonnull
  private final String timerText;
  @Nonnull
  private final String debugStat;

  public VexDemoHudRequestedEvent(@Nonnull PlayerRef playerRef, @Nonnull String scoreText,
      @Nonnull String timerText, @Nonnull String debugStat) {
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
    this.scoreText = Objects.requireNonNull(scoreText, "scoreText");
    this.timerText = Objects.requireNonNull(timerText, "timerText");
    this.debugStat = Objects.requireNonNull(debugStat, "debugStat");
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Nonnull
  public String getScoreText() {
    return scoreText;
  }

  @Nonnull
  public String getTimerText() {
    return timerText;
  }

  @Nonnull
  public String getDebugStat() {
    return debugStat;
  }

  public static Map<String, Object> buildPayload(UUID playerId, String playerName,
      String scoreText, String timerText, String debugStat) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("player", playerMeta(playerId, playerName));
    data.put("scoreText", scoreText);
    data.put("timerText", timerText);
    data.put("debugStat", debugStat);
    return data;
  }

  @Override
  public Object toPayload() {
    return buildPayload(playerRef.getUuid(), playerRef.getUsername(), scoreText, timerText, debugStat);
  }
}
