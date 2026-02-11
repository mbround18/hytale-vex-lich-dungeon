package MBRound18.ImmortalEngine.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class WorldEnteredEvent extends DebugEvent {
  @Nonnull
  private final World world;
  @Nonnull
  private final PlayerRef playerRef;

  public WorldEnteredEvent(@Nonnull World world, @Nonnull PlayerRef playerRef) {
    this.world = Objects.requireNonNull(world, "world");
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      Map<String, Object> player = playerMeta(playerRef);
      Map<String, Object> location = resolvePlayerLocation(playerRef);
      if (player != null && location != null && !location.isEmpty()) {
        player.put("location", location);
      }
      data.put("player", player);
      return data;
    }));
  }

  private Map<String, Object> resolvePlayerLocation(PlayerRef ref) {
    if (ref == null) {
      return null;
    }
    try {
      Ref<EntityStore> entityRef = ref.getReference();
      if (entityRef == null || !entityRef.isValid()) {
        return null;
      }
      Store<EntityStore> store = entityRef.getStore();
      if (store == null) {
        return null;
      }
      TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
      if (transform == null) {
        return null;
      }
      Vector3d position = transform.getPosition();
      if (position == null) {
        return null;
      }
      Map<String, Object> coords = new LinkedHashMap<>();
      coords.put("x", formatCoordinate(position.getX()));
      coords.put("y", formatCoordinate(position.getY()));
      coords.put("z", formatCoordinate(position.getZ()));
      return coords;
    } catch (Exception ignored) {
      return null;
    }
  }

  private static Object formatCoordinate(Object coord) {
    if (coord instanceof Number) {
      double value = ((Number) coord).doubleValue();
      return Math.round(value * 100.0) / 100.0;
    }
    return coord;
  }
}
