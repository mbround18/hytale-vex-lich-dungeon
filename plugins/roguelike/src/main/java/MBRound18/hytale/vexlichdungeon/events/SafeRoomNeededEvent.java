package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class SafeRoomNeededEvent extends DebugEvent {
  @Nonnull
  private final World world;
  private final int roomsSinceEvent;
  private final int eventInterval;

  public SafeRoomNeededEvent(@Nonnull World world, int roomsSinceEvent, int eventInterval) {
    this.world = Objects.requireNonNull(world, "world");
    this.roomsSinceEvent = roomsSinceEvent;
    this.eventInterval = eventInterval;
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  public int getRoomsSinceEvent() {
    return roomsSinceEvent;
  }

  public int getEventInterval() {
    return eventInterval;
  }

  @Override
  public Object toPayload() {
    return onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("roomsSinceEvent", roomsSinceEvent);
      data.put("eventInterval", eventInterval);
      return data;
    });
  }
}
