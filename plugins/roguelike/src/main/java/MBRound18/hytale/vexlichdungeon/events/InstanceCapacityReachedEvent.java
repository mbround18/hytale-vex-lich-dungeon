package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class InstanceCapacityReachedEvent extends DebugEvent {
  @Nonnull
  private final String instanceTemplate;
  @Nonnull
  private final String worldName;
  private final int maxPlayers;
  private final int currentPlayers;

  public InstanceCapacityReachedEvent(@Nonnull String instanceTemplate, @Nonnull String worldName,
      int maxPlayers, int currentPlayers) {
    this.instanceTemplate = Objects.requireNonNull(instanceTemplate, "instanceTemplate");
    this.worldName = Objects.requireNonNull(worldName, "worldName");
    this.maxPlayers = maxPlayers;
    this.currentPlayers = currentPlayers;
  }

  @Nonnull
  public String getInstanceTemplate() {
    return instanceTemplate;
  }

  @Nonnull
  public String getWorldName() {
    return worldName;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public int getCurrentPlayers() {
    return currentPlayers;
  }

  @Override
  public Object toPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("instanceTemplate", instanceTemplate);
    data.put("worldName", worldName);
    data.put("maxPlayers", maxPlayers);
    data.put("currentPlayers", currentPlayers);
    return data;
  }
}
