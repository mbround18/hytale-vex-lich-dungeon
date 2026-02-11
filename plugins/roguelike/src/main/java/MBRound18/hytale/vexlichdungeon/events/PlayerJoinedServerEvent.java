package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class PlayerJoinedServerEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final PlayerRef playerRef;

  public PlayerJoinedServerEvent(@Nonnull World world, @Nonnull PlayerRef playerRef) {
    this.world = Objects.requireNonNull(world, "world");
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      String worldName = world.getName();
      boolean isInstance = worldName != null && worldName.startsWith("instance-");
      data.put("worldName", worldName);
      data.put("isInstance", isInstance);
      data.put("instanceName", isInstance ? worldName : null);
      data.put("player", playerMeta(playerRef));
      return data;
    }));
  }
}
