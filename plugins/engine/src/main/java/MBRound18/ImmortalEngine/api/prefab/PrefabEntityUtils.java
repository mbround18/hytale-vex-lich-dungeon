package MBRound18.ImmortalEngine.api.prefab;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.FromPrefab;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.NewSpawnComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Objects;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility helpers for entities spawned via prefabs.
 */
public final class PrefabEntityUtils {
  private PrefabEntityUtils() {
  }

  /**
   * Attempts to "unfreeze" prefab-spawned NPCs by clearing common spawn-lock
   * components.
   * <p>
   * This is a best-effort helper; if the entity is not an NPC or the components
   * are absent, the call is a no-op.
   *
   * @param entityRef entity reference created by prefab placement
   * @param log       optional logger for warnings
   */
  public static void unfreezePrefabNpc(@Nonnull Ref<EntityStore> entityRef, @Nullable HytaleLogger log) {
    tryUnfreezePrefabEntity(entityRef, log);
  }

  public static boolean tryUnfreezePrefabEntity(@Nullable Ref<EntityStore> entityRef, @Nullable HytaleLogger log) {
    if (entityRef == null || !entityRef.isValid()) {
      return false;
    }
    try {
      Store<EntityStore> store = Objects.requireNonNull(entityRef.getStore(), "store");
      store.removeComponentIfExists(entityRef,
          Objects.requireNonNull(FromPrefab.getComponentType(), "fromPrefab"));
      store.removeComponentIfExists(entityRef,
          Objects.requireNonNull(NewSpawnComponent.getComponentType(), "newSpawn"));
      store.removeComponentIfExists(entityRef,
          Objects.requireNonNull(Intangible.getComponentType(), "intangible"));
      return true;
    } catch (Exception e) {
      if (log != null) {
        log.at(Objects.requireNonNull(Level.WARNING, "level"))
            .log(String.format("Failed to unfreeze prefab entity %s: %s", entityRef,
                e.getMessage()));
      }
      return false;
    }
  }
}
