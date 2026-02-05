package MBRound18.hytale.vexlichdungeon.prefab;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface PrefabHook {
  default void onPrefabDiscovered(PrefabDiscovered prefab) {
  }

  default void onPrefabLoaded(String prefabPath, BlockSelection prefab) {
  }

  default void beforePlace(PrefabPlaceContext context) {
  }

  default void afterPlace(PrefabPlaceContext context) {
  }

  default void onSpawnEntity(World world, String prefabPath, Ref<EntityStore> entityRef) {
  }
}
