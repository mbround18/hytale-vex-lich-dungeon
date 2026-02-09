package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class PortalOwnerRegisteredEvent extends DebugEvent {
  @Nonnull
  private final UUID portalId;
  @Nonnull
  private final UUID ownerId;

  public PortalOwnerRegisteredEvent(@Nonnull UUID portalId, @Nonnull UUID ownerId) {
    this.portalId = Objects.requireNonNull(portalId, "portalId");
    this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
  }

  @Nonnull
  public UUID getPortalId() {
    return portalId;
  }

  @Nonnull
  public UUID getOwnerId() {
    return ownerId;
  }

  @Override
  public Object toPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("portalId", portalId);
    data.put("ownerId", ownerId);
    return data;
  }
}
