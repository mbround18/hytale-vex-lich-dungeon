package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class ReturnPortalSpawnedEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final RoomCoordinate room;
  @Nonnull
  private final Vector3i position;

  public ReturnPortalSpawnedEvent(@Nonnull World world, @Nonnull RoomCoordinate room, @Nonnull Vector3i position) {
    this.world = Objects.requireNonNull(world, "world");
    this.room = Objects.requireNonNull(room, "room");
    this.position = Objects.requireNonNull(position, "position");
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public RoomCoordinate getRoom() {
    return room;
  }

  @Nonnull
  public Vector3i getPosition() {
    return position;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("room", room);
      data.put("position", position);
      return data;
    }));
  }
}
