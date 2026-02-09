package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class CountdownHudClearRequestedEvent extends DebugEvent {
  @Nonnull
  private final PlayerRef playerRef;

  public CountdownHudClearRequestedEvent(@Nonnull PlayerRef playerRef) {
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Override
  public Object toPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("player", playerMeta(playerRef.getUuid(), playerRef.getUsername()));
    return data;
  }
}
