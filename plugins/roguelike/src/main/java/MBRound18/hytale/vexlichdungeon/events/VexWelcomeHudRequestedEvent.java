package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class VexWelcomeHudRequestedEvent extends DebugEvent {
  @Nonnull
  private final PlayerRef playerRef;
  @Nonnull
  private final String bodyText;

  public VexWelcomeHudRequestedEvent(@Nonnull PlayerRef playerRef, @Nonnull String bodyText) {
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
    this.bodyText = Objects.requireNonNull(bodyText, "bodyText");
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Nonnull
  public String getBodyText() {
    return bodyText;
  }

  public static Map<String, Object> buildPayload(UUID playerId, String playerName, String bodyText) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("player", playerMeta(playerId, playerName));
    data.put("bodyText", bodyText);
    return data;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(buildPayload(playerRef.getUuid(), playerRef.getUsername(), bodyText));
  }
}
