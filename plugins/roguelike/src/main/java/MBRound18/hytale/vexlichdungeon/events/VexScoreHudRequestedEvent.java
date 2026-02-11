package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class VexScoreHudRequestedEvent extends DebugEvent {
  @Nonnull
  private final PlayerRef playerRef;
  private final int instanceScore;
  private final int playerScore;
  private final int delta;
  @Nonnull
  private final String partyList;

  public VexScoreHudRequestedEvent(@Nonnull PlayerRef playerRef, int instanceScore, int playerScore,
      int delta, @Nonnull String partyList) {
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
    this.instanceScore = instanceScore;
    this.playerScore = playerScore;
    this.delta = delta;
    this.partyList = Objects.requireNonNull(partyList, "partyList");
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  public int getInstanceScore() {
    return instanceScore;
  }

  public int getPlayerScore() {
    return playerScore;
  }

  public int getDelta() {
    return delta;
  }

  @Nonnull
  public String getPartyList() {
    return partyList;
  }

  public static Map<String, Object> buildPayload(UUID playerId, String playerName,
      int instanceScore, int playerScore, int delta, String partyList) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("player", playerMeta(playerId, playerName));
    data.put("instanceScore", instanceScore);
    data.put("playerScore", playerScore);
    data.put("delta", delta);
    data.put("partyList", partyList);
    return data;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(buildPayload(playerRef.getUuid(), playerRef.getUsername(), instanceScore, playerScore, delta, partyList));
  }
}
