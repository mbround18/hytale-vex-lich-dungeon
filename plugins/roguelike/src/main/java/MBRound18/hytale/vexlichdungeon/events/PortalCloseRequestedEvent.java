package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class PortalCloseRequestedEvent extends DebugEvent {
  @Nonnull
  private final UUID portalId;

  public PortalCloseRequestedEvent(@Nonnull UUID portalId) {
    this.portalId = Objects.requireNonNull(portalId, "portalId");
  }

  @Nonnull
  public UUID getPortalId() {
    return portalId;
  }

  @Override
  public Object toPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("portalId", portalId);
    return withCorrelation(data);
  }
}
