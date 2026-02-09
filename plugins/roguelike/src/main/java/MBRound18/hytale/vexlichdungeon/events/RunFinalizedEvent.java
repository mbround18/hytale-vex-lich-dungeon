package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.RunSummary;
import MBRound18.ImmortalEngine.api.events.DebugEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RunFinalizedEvent extends DebugEvent {
  @Nonnull
  private final String worldName;
  @Nonnull
  private final RunSummary summary;
  @Nullable
  private final String reason;

  public RunFinalizedEvent(@Nonnull String worldName, @Nonnull RunSummary summary, @Nullable String reason) {
    this.worldName = Objects.requireNonNull(worldName, "worldName");
    this.summary = Objects.requireNonNull(summary, "summary");
    this.reason = reason;
  }

  @Nonnull
  public String getWorldName() {
    return worldName;
  }

  @Nonnull
  public RunSummary getSummary() {
    return summary;
  }

  @Nullable
  public String getReason() {
    return reason;
  }

  @Override
  public Object toPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("worldName", worldName);
    data.put("summary", summary);
    data.put("reason", reason);
    return data;
  }
}
