package MBRound18.ImmortalEngine.api.events;

import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PortalClosedEvent extends DebugEvent {
  @Nonnull
  private final UUID portalId;
  private final World world;

  public PortalClosedEvent(@Nonnull UUID portalId, @Nullable World world) {
    this.portalId = Objects.requireNonNull(portalId, "portalId");
    this.world = world;
  }

  @Nonnull
  public UUID getPortalId() {
    return portalId;
  }

  @Nullable
  public World getWorld() {
    return world;
  }

  @Override
  public Object toPayload() {
    return onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("portalId", portalId);
      data.put("world", worldMeta(world));
      return data;
    });
  }
}
