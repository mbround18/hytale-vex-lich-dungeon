package MBRound18.ImmortalEngine.api.social;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Stored return location for party members leaving an instance.
 */
public final class ReturnLocation {
  private final String worldName;
  private final Vector3d position;
  private final Vector3f rotation;

  public ReturnLocation(@Nonnull String worldName, @Nonnull Vector3d position,
      @Nonnull Vector3f rotation) {
    this.worldName = Objects.requireNonNull(worldName, "worldName");
    this.position = Objects.requireNonNull(position, "position");
    this.rotation = Objects.requireNonNull(rotation, "rotation");
  }

  @Nonnull
  public String getWorldName() {
    return Objects.requireNonNull(worldName, "worldName");
  }

  @Nonnull
  public Vector3d getPosition() {
    return Objects.requireNonNull(position, "position");
  }

  @Nonnull
  public Vector3f getRotation() {
    return Objects.requireNonNull(rotation, "rotation");
  }
}
