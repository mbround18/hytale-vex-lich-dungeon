package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class InstanceTeardownStartedEvent extends DebugEvent {
  @Nonnull
  private final String worldName;

  public InstanceTeardownStartedEvent(@Nonnull String worldName) {
    this.worldName = Objects.requireNonNull(worldName, "worldName");
  }

  @Nonnull
  public String getWorldName() {
    return worldName;
  }

  @Override
  public Object toPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("worldName", worldName);
    return data;
  }
}
