package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RunFinalizeRequestedEvent extends DebugEvent {
  @Nonnull
  private final String worldName;
  @Nullable
  private final String reason;

  public RunFinalizeRequestedEvent(@Nonnull String worldName, @Nullable String reason) {
    this.worldName = Objects.requireNonNull(worldName, "worldName");
    this.reason = reason;
  }

  @Nonnull
  public String getWorldName() {
    return worldName;
  }

  @Nullable
  public String getReason() {
    return reason;
  }

  @Override
  public Object toPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("worldName", worldName);
    data.put("reason", reason);
    return withCorrelation(data);
  }
}
