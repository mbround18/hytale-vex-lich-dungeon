package MBRound18.hytale.vexlichdungeon.prefab;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;
import java.util.Objects;

public final class PrefabPlaceContext {
  private final World world;
  private final String prefabPath;
  private final Vector3i origin;
  private final int rotationDegrees;
  private final boolean gate;
  private final BlockSelection prefab;

  public PrefabPlaceContext(
      @Nonnull World world,
      @Nonnull String prefabPath,
      @Nonnull Vector3i origin,
      int rotationDegrees,
      boolean gate,
      @Nonnull BlockSelection prefab) {
    this.world = Objects.requireNonNull(world, "world");
    this.prefabPath = Objects.requireNonNull(prefabPath, "prefabPath");
    this.origin = Objects.requireNonNull(origin, "origin");
    this.rotationDegrees = rotationDegrees;
    this.gate = gate;
    this.prefab = Objects.requireNonNull(prefab, "prefab");
  }

  @Nonnull
  @SuppressWarnings("null")
  public World getWorld() {
    return world;
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

  @Nonnull
  @SuppressWarnings("null")
  public BlockSelection getPrefab() {
    return prefab;
  }
}
