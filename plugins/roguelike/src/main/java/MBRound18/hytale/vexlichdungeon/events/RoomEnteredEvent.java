package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RoomEnteredEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final PlayerRef playerRef;
  @Nonnull
  private final RoomCoordinate room;
  @Nullable
  private final RoomCoordinate previousRoom;

  public RoomEnteredEvent(@Nonnull World world, @Nonnull PlayerRef playerRef, @Nonnull RoomCoordinate room,
      @Nullable RoomCoordinate previousRoom) {
    this.world = Objects.requireNonNull(world, "world");
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
    this.room = Objects.requireNonNull(room, "room");
    this.previousRoom = previousRoom;
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Nonnull
  public RoomCoordinate getRoom() {
    return room;
  }

  @Nullable
  public RoomCoordinate getPreviousRoom() {
    return previousRoom;
  }

  @Override
  public Object toPayload() {
    return onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("player", playerMeta(playerRef));
      data.put("room", roomMeta(room));
      data.put("previousRoom", roomMeta(previousRoom));
      data.put("roomKey", toRoomKey(room));
      data.put("previousRoomKey", toRoomKey(previousRoom));
      RoomCoordinate prev = previousRoom;
      if (prev != null) {
        data.put("delta", Map.of("x", room.getX() - prev.getX(), "z", room.getZ() - prev.getZ()));
      }
      data.put("isFirstRoom", previousRoom == null);
      return data;
    });
  }

  private Map<String, Object> roomMeta(@Nullable RoomCoordinate coordinate) {
    if (coordinate == null) {
      return null;
    }
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("x", coordinate.getX());
    meta.put("z", coordinate.getZ());
    meta.put("key", toRoomKey(coordinate));
    return meta;
  }

  private String toRoomKey(@Nullable RoomCoordinate coordinate) {
    if (coordinate == null) {
      return null;
    }
    return coordinate.getX() + "," + coordinate.getZ();
  }
}
