package MBRound18.hytale.vexlichdungeon.prefab;

import com.hypixel.hytale.math.vector.Vector3i;
import javax.annotation.Nonnull;
import java.util.Objects;

public final class PrefabPlacement {
  private final String worldName;
  private final String prefabPath;
  private final Vector3i origin;
  private final int rotationDegrees;
  private final boolean gate;
  private final long placedAtMs;

  public PrefabPlacement(@Nonnull String worldName, @Nonnull String prefabPath, @Nonnull Vector3i origin,
      int rotationDegrees, boolean gate, long placedAtMs) {
    this.worldName = Objects.requireNonNull(worldName, "worldName");
    this.prefabPath = Objects.requireNonNull(prefabPath, "prefabPath");
    this.origin = Objects.requireNonNull(origin, "origin");
    this.rotationDegrees = rotationDegrees;
    this.gate = gate;
    this.placedAtMs = placedAtMs;
  }

  @Nonnull
  @SuppressWarnings("null")
  public String getWorldName() {
    return worldName;
  }

  @Nonnull
  @SuppressWarnings("null")
  public String getPrefabPath() {
    return prefabPath;
  }

  @Nonnull
  @SuppressWarnings("null")
  public Vector3i getOrigin() {
    return origin;
  }

  public int getRotationDegrees() {
    return rotationDegrees;
  }

  public boolean isGate() {
    return gate;
  }

  public long getPlacedAtMs() {
    return placedAtMs;
  }
}
