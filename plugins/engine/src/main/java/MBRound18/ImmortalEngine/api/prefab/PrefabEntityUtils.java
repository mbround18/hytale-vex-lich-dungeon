package MBRound18.ImmortalEngine.api.prefab;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.FromPrefab;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.NewSpawnComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility helpers for entities spawned via prefabs.
 */
public final class PrefabEntityUtils {
  private PrefabEntityUtils() {
  }

  /**
   * Attempts to "unfreeze" prefab-spawned NPCs by clearing common spawn-lock components.
   * <p>
   * This is a best-effort helper; if the entity is not an NPC or the components
   * are absent, the call is a no-op.
   *
   * @param entityRef entity reference created by prefab placement
   * @param log optional logger for warnings
   */
  public static void unfreezePrefabNpc(@Nonnull Ref<EntityStore> entityRef, @Nullable EngineLog log) {
    try {
      Store<EntityStore> store = entityRef.getStore();
      if (store == null) {
        return;
      }
      NPCEntity npc = store.getComponent(entityRef, NPCEntity.getComponentType());
      if (npc == null) {
        return;
      }
      store.removeComponentIfExists(entityRef, FromPrefab.getComponentType());
      store.removeComponentIfExists(entityRef, NewSpawnComponent.getComponentType());
      store.removeComponentIfExists(entityRef, Intangible.getComponentType());
    } catch (Exception e) {
      if (log != null) {
        log.warn("Failed to unfreeze prefab entity %s: %s", entityRef, e.getMessage());
      }
    }
  }
}
