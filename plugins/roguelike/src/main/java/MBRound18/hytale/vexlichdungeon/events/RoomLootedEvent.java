package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RoomLootedEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final RoomCoordinate room;
  @Nullable
  private final UUID chestId;

  public RoomLootedEvent(@Nonnull World world, @Nonnull RoomCoordinate room, @Nullable UUID chestId) {
    this.world = Objects.requireNonNull(world, "world");
    this.room = Objects.requireNonNull(room, "room");
    this.chestId = chestId;
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public RoomCoordinate getRoom() {
    return room;
  }

  @Nullable
  public UUID getChestId() {
    return chestId;
  }

  @Override
  public Object toPayload() {
    return onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("room", room);
      data.put("chestId", chestId);
      return data;
    });
  }
}
