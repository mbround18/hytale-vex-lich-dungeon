package MBRound18.hytale.vexlichdungeon.prefab;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import javax.annotation.Nonnull;
import java.util.Objects;

public final class PrefabEntityDefinition {
  private final String modelId;
  private final Vector3d position;
  private final Vector3f rotation;

  public PrefabEntityDefinition(@Nonnull String modelId, @Nonnull Vector3d position, @Nonnull Vector3f rotation) {
    this.modelId = Objects.requireNonNull(modelId, "modelId");
    this.position = Objects.requireNonNull(position, "position");
    this.rotation = Objects.requireNonNull(rotation, "rotation");
  }

  @Nonnull
  @SuppressWarnings("null")
  public String getModelId() {
    return modelId;
  }

  @Nonnull
  @SuppressWarnings("null")
  public Vector3d getPosition() {
    return position;
  }

  @Nonnull
  @SuppressWarnings("null")
  public Vector3f getRotation() {
    return rotation;
  }
}
