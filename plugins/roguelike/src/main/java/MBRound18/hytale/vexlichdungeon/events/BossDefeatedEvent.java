package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class BossDefeatedEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final UUID entityId;
  @Nonnull
  private final RoomCoordinate room;

  public BossDefeatedEvent(@Nonnull World world, @Nonnull UUID entityId, @Nonnull RoomCoordinate room) {
    this.world = Objects.requireNonNull(world, "world");
    this.entityId = Objects.requireNonNull(entityId, "entityId");
    this.room = Objects.requireNonNull(room, "room");
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public UUID getEntityId() {
    return entityId;
  }

  @Nonnull
  public RoomCoordinate getRoom() {
    return room;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("entityId", entityId);
      data.put("room", room);
      return data;
    }));
  }
}
