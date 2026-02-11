package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class NpcSpawnRequestedEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nullable
  private final RoomCoordinate room;
  @Nonnull
  private final String roleName;
  @Nonnull
  private final String modelId;
  @Nonnull
  private final Vector3d position;
  @Nonnull
  private final Vector3f rotation;
  @Nullable
  private final String prefabPath;
  @Nonnull
  private final CompletableFuture<NpcSpawnResult> result;

  public NpcSpawnRequestedEvent(@Nonnull World world, @Nullable RoomCoordinate room, @Nonnull String roleName,
      @Nonnull String modelId, @Nonnull Vector3d position, @Nonnull Vector3f rotation,
      @Nullable String prefabPath) {
    this.world = Objects.requireNonNull(world, "world");
    this.room = room;
    this.roleName = Objects.requireNonNull(roleName, "roleName");
    this.modelId = Objects.requireNonNull(modelId, "modelId");
    this.position = Objects.requireNonNull(position, "position");
    this.rotation = Objects.requireNonNull(rotation, "rotation");
    this.prefabPath = prefabPath;
    this.result = new CompletableFuture<>();
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nullable
  public RoomCoordinate getRoom() {
    return room;
  }

  @Nonnull
  public String getRoleName() {
    return roleName;
  }

  @Nonnull
  public String getModelId() {
    return modelId;
  }

  @Nonnull
  public Vector3d getPosition() {
    return position;
  }

  @Nonnull
  public Vector3f getRotation() {
    return rotation;
  }

  @Nullable
  public String getPrefabPath() {
    return prefabPath;
  }

  @Nonnull
  public CompletableFuture<NpcSpawnResult> getResult() {
    return result;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      if (room != null) {
        data.put("room", room);
      }
      data.put("roleName", roleName);
      data.put("modelId", modelId);
      data.put("position", Map.of("x", position.x, "y", position.y, "z", position.z));
      data.put("rotation", Map.of("x", rotation.x, "y", rotation.y, "z", rotation.z));
      if (prefabPath != null) {
        data.put("prefabPath", prefabPath);
      }
      return data;
    }));
  }
}
