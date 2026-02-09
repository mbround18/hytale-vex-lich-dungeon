package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class VexLeaderboardHudRequestedEvent extends DebugEvent {
  @Nonnull
  private final PlayerRef playerRef;
  @Nonnull
  private final String leaderboardText;

  public VexLeaderboardHudRequestedEvent(@Nonnull PlayerRef playerRef, @Nonnull String leaderboardText) {
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
    this.leaderboardText = Objects.requireNonNull(leaderboardText, "leaderboardText");
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Nonnull
  public String getLeaderboardText() {
    return leaderboardText;
  }

  public static Map<String, Object> buildPayload(UUID playerId, String playerName, String leaderboardText) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("player", playerMeta(playerId, playerName));
    data.put("leaderboardText", leaderboardText);
    return data;
  }

  @Override
  public Object toPayload() {
    return buildPayload(playerRef.getUuid(), playerRef.getUsername(), leaderboardText);
  }
}
