package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EntitySpawnedEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final UUID entityId;
  @Nonnull
  private final RoomCoordinate room;
  @Nullable
  private final String entityType;
  private final int points;
  @Nullable
  private final Vector3d position;

  public EntitySpawnedEvent(@Nonnull World world, @Nonnull UUID entityId, @Nonnull RoomCoordinate room,
      @Nullable String entityType, int points) {
    this(world, entityId, room, entityType, points, null);
  }

  public EntitySpawnedEvent(@Nonnull World world, @Nonnull UUID entityId, @Nonnull RoomCoordinate room,
      @Nullable String entityType, int points, @Nullable Vector3d position) {
    this.world = Objects.requireNonNull(world, "world");
    this.entityId = Objects.requireNonNull(entityId, "entityId");
    this.room = Objects.requireNonNull(room, "room");
    this.entityType = entityType;
    this.points = points;
    this.position = position;
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

  @Nullable
  public String getEntityType() {
    return entityType;
  }

  public int getPoints() {
    return points;
  }

  @Nullable
  public Vector3d getPosition() {
    return position;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("entityId", entityId);
      data.put("room", room);
      data.put("entityType", entityType);
      data.put("points", points);
      if (position != null) {
        data.put("position", Map.of("x", position.x, "y", position.y, "z", position.z));
      }
      return data;
    }));
  }
}
