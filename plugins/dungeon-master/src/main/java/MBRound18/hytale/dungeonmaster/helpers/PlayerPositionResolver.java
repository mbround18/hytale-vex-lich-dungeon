package MBRound18.hytale.dungeonmaster.helpers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerPositionResolver {
  private PlayerPositionResolver() {
  }

  public static @Nullable Vector3d resolvePosition(@Nonnull PlayerRef playerRef) {
    try {
      try {
        com.hypixel.hytale.math.vector.Transform transform = playerRef.getTransform();
        if (transform != null) {
          Vector3d pos = transform.getPosition();
          if (pos != null) {
            return pos;
          }
        }
      } catch (Exception ignored) {
        // fall through
      }
      Ref<EntityStore> ref = playerRef.getReference();
      if (ref == null || !ref.isValid()) {
        return null;
      }
      Store<EntityStore> store = ref.getStore();
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      return transform != null ? transform.getPosition() : null;
    } catch (Exception e) {
      return null;
    }
  }

  public static CompletableFuture<Vector3d> resolvePositionAsync(@Nonnull PlayerRef playerRef,
      @Nonnull World world) {
    CompletableFuture<Vector3d> future = new CompletableFuture<>();
    PlayerWorldExecutor.runOnPlayerWorld(playerRef, world, () -> {
      try {
        future.complete(resolvePosition(playerRef));
      } catch (Exception e) {
        future.complete(null);
      }
    });
    return future;
  }
}
