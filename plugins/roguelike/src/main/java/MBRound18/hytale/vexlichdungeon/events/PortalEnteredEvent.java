package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PortalEnteredEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final PlayerRef playerRef;
  @Nullable
  private final UUID portalId;

  public PortalEnteredEvent(@Nonnull World world, @Nonnull PlayerRef playerRef, @Nullable UUID portalId) {
    this.world = Objects.requireNonNull(world, "world");
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
    this.portalId = portalId;
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Nullable
  public UUID getPortalId() {
    return portalId;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      String worldName = world.getName();
      boolean isInstance = worldName != null && worldName.startsWith("instance-");
      data.put("world", worldMeta(world));
      data.put("worldName", worldName);
      data.put("isInstance", isInstance);
      data.put("instanceName", isInstance ? worldName : null);
      data.put("player", playerMeta(playerRef));
      data.put("portalId", portalId);
      if (portalId != null) {
        String portalString = portalId.toString();
        data.put("portalIdString", portalString);
        data.put("portalIdShort", portalString.length() > 8 ? portalString.substring(0, 8) : portalString);
      }
      return data;
    }));
  }
}
