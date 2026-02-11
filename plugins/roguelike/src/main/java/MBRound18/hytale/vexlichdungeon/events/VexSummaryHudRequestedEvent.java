package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class VexSummaryHudRequestedEvent extends DebugEvent {
  @Nonnull
  private final PlayerRef playerRef;
  @Nonnull
  private final String statsLine;
  @Nonnull
  private final String summaryLine;

  public VexSummaryHudRequestedEvent(@Nonnull PlayerRef playerRef, @Nonnull String statsLine,
      @Nonnull String summaryLine) {
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
    this.statsLine = Objects.requireNonNull(statsLine, "statsLine");
    this.summaryLine = Objects.requireNonNull(summaryLine, "summaryLine");
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Nonnull
  public String getStatsLine() {
    return statsLine;
  }

  @Nonnull
  public String getSummaryLine() {
    return summaryLine;
  }

  public static Map<String, Object> buildPayload(UUID playerId, String playerName,
      String statsLine, String summaryLine) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("player", playerMeta(playerId, playerName));
    data.put("statsLine", statsLine);
    data.put("summaryLine", summaryLine);
    return data;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(buildPayload(playerRef.getUuid(), playerRef.getUsername(), statsLine, summaryLine));
  }
}
