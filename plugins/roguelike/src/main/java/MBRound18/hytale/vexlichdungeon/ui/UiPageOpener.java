package MBRound18.hytale.vexlichdungeon.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nullable;

public final class UiPageOpener {
  private UiPageOpener() {
  }

  public static boolean open(@Nullable PlayerRef playerRef, CustomUIPage page) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    Ref<EntityStore> entityRef = playerRef.getReference();
    if (entityRef == null || !entityRef.isValid()) {
      return false;
    }
    Store<EntityStore> store = entityRef.getStore();
    Player player = store.getComponent(entityRef, Player.getComponentType());
    if (player == null) {
      return false;
    }
    player.getPageManager().openCustomPage(entityRef, store, page);
    return true;
  }
}
