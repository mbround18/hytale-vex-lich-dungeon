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

public final class EntityEliminatedEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final UUID entityId;
  @Nullable
  private final UUID killerId;
  @Nullable
  private final String killerName;
  private final int points;
  @Nonnull
  private final RoomCoordinate room;
  @Nullable
  private final String entityType;
  @Nullable
  private final Vector3d position;

  public EntityEliminatedEvent(@Nonnull World world, @Nonnull UUID entityId, @Nullable UUID killerId, int points,
      @Nonnull RoomCoordinate room) {
    this(world, entityId, killerId, null, points, room, null, null);
  }

  public EntityEliminatedEvent(@Nonnull World world, @Nonnull UUID entityId, @Nullable UUID killerId,
      @Nullable String killerName, int points, @Nonnull RoomCoordinate room, @Nullable String entityType,
      @Nullable Vector3d position) {
    this.world = Objects.requireNonNull(world, "world");
    this.entityId = Objects.requireNonNull(entityId, "entityId");
    this.killerId = killerId;
    this.killerName = killerName;
    this.points = points;
    this.room = Objects.requireNonNull(room, "room");
    this.entityType = entityType;
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

  @Nullable
  public UUID getKillerId() {
    return killerId;
  }

  @Nullable
  public String getKillerName() {
    return killerName;
  }

  public int getPoints() {
    return points;
  }

  @Nonnull
  public RoomCoordinate getRoom() {
    return room;
  }

  @Nullable
  public String getEntityType() {
    return entityType;
  }

  @Nullable
  public Vector3d getPosition() {
    return position;
  }

  @Override
  public Object toPayload() {
    return onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      Map<String, Object> entity = new LinkedHashMap<>();
      entity.put("id", entityId);
      entity.put("type", entityType);
      Vector3d pos = position;
      if (pos != null) {
        entity.put("position", Map.of("x", pos.x, "y", pos.y, "z", pos.z));
      }
      data.put("entity", entity);
      if (killerId != null) {
        data.put("player", playerMeta(killerId, killerName));
      }
      data.put("points", points);
      data.put("room", room);
      return data;
    });
  }
}
