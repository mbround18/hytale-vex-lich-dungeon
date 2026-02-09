package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class PortalCountdownHudUpdateRequestedEvent extends DebugEvent {
  @Nonnull
  private final PlayerRef playerRef;
  @Nonnull
  private final UUID portalId;
  @Nonnull
  private final String timeLeft;
  @Nonnull
  private final String locationText;

  public PortalCountdownHudUpdateRequestedEvent(@Nonnull PlayerRef playerRef, @Nonnull UUID portalId,
      @Nonnull String timeLeft, @Nonnull String locationText) {
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
    this.portalId = Objects.requireNonNull(portalId, "portalId");
    this.timeLeft = Objects.requireNonNull(timeLeft, "timeLeft");
    this.locationText = Objects.requireNonNull(locationText, "locationText");
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Nonnull
  public UUID getPortalId() {
    return portalId;
  }

  @Nonnull
  public String getTimeLeft() {
    return timeLeft;
  }

  @Nonnull
  public String getLocationText() {
    return locationText;
  }

  public static Map<String, Object> buildPayload(UUID playerId, String playerName, UUID portalId,
      String timeLeft, String locationText) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("player", playerMeta(playerId, playerName));
    data.put("portalId", portalId);
    data.put("timeLeft", timeLeft);
    data.put("locationText", locationText);
    return data;
  }

  @Override
  public Object toPayload() {
    return buildPayload(playerRef.getUuid(), playerRef.getUsername(), portalId, timeLeft, locationText);
  }
}
