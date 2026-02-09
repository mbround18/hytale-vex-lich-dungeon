package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class RoundClearedEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final RoomCoordinate room;

  public RoundClearedEvent(@Nonnull World world, @Nonnull RoomCoordinate room) {
    this.world = Objects.requireNonNull(world, "world");
    this.room = Objects.requireNonNull(room, "room");
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public RoomCoordinate getRoom() {
    return room;
  }

  @Override
  public Object toPayload() {
    return onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("room", room);
      return data;
    });
  }
}
