package MBRound18.ImmortalEngine.api.events;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class PortalCreatedEvent extends DebugEvent {
  @Nonnull
  private final UUID portalId;
  @Nonnull
  private final World world;
  @Nonnull
  private final Vector3i placement;
  private final long expiresAt;

  public PortalCreatedEvent(@Nonnull UUID portalId, @Nonnull World world, @Nonnull Vector3i placement,
      long expiresAt) {
    this.portalId = Objects.requireNonNull(portalId, "portalId");
    this.world = Objects.requireNonNull(world, "world");
    this.placement = Objects.requireNonNull(placement, "placement");
    this.expiresAt = expiresAt;
  }

  @Nonnull
  public UUID getPortalId() {
    return portalId;
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public Vector3i getPlacement() {
    return placement;
  }

  public long getExpiresAt() {
    return expiresAt;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("portalId", portalId);
      data.put("world", worldMeta(world));
      data.put("placement", placement);
      data.put("expiresAt", expiresAt);
      return data;
    }));
  }
}
