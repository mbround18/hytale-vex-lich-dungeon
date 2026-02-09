package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ChestSpawnedEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final Vector3d position;
  @Nonnull
  private final String modelId;
  @Nullable
  private final String prefabPath;

  public ChestSpawnedEvent(@Nonnull World world, @Nonnull Vector3d position, @Nonnull String modelId,
      @Nullable String prefabPath) {
    this.world = Objects.requireNonNull(world, "world");
    this.position = Objects.requireNonNull(position, "position");
    this.modelId = Objects.requireNonNull(modelId, "modelId");
    this.prefabPath = prefabPath;
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public Vector3d getPosition() {
    return position;
  }

  @Nonnull
  public String getModelId() {
    return modelId;
  }

  @Nullable
  public String getPrefabPath() {
    return prefabPath;
  }

  @Override
  public Object toPayload() {
    return onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      data.put("position", position);
      data.put("modelId", modelId);
      data.put("prefabPath", prefabPath);
      return data;
    });
  }
}
