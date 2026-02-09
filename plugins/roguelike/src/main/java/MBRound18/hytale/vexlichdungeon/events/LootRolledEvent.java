package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class LootRolledEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final RoomCoordinate room;
  @Nonnull
  private final String itemId;
  private final int count;

  public LootRolledEvent(@Nonnull World world, @Nonnull RoomCoordinate room, @Nonnull String itemId, int count) {
    this.world = Objects.requireNonNull(world, "world");
    this.room = Objects.requireNonNull(room, "room");
    this.itemId = Objects.requireNonNull(itemId, "itemId");
    this.count = count;
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
  public String getItemId() {
    return itemId;
  }

  public int getCount() {
    return count;
  }

  @Override
  public Object toPayload() {
    return onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("room", room);
      data.put("itemId", itemId);
      data.put("count", count);
      return data;
    });
  }
}
