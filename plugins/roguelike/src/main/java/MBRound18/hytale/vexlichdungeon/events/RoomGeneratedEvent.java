package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class RoomGeneratedEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final RoomCoordinate room;
  @Nonnull
  private final String prefabPath;

  public RoomGeneratedEvent(@Nonnull World world, @Nonnull RoomCoordinate room, @Nonnull String prefabPath) {
    this.world = Objects.requireNonNull(world, "world");
    this.room = Objects.requireNonNull(room, "room");
    this.prefabPath = Objects.requireNonNull(prefabPath, "prefabPath");
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
  public String getPrefabPath() {
    return prefabPath;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("room", room);
      data.put("prefabPath", prefabPath);
      return data;
    }));
  }
}
