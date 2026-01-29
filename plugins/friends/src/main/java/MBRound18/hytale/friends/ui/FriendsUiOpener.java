package MBRound18.hytale.friends.ui;

import MBRound18.ImmortalEngine.api.ui.UiThread;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nullable;

public final class FriendsUiOpener {
  private FriendsUiOpener() {
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
    if (store.isInThread()) {
      return openOnThread(entityRef, store, page);
    }
    return UiThread.runOnPlayerWorld(playerRef, () -> openOnThread(entityRef, store, page));
  }

  private static boolean openOnThread(Ref<EntityStore> entityRef, Store<EntityStore> store,
      CustomUIPage page) {
    Player player = store.getComponent(entityRef, Player.getComponentType());
    if (player == null) {
      return false;
    }
    player.getPageManager().openCustomPage(entityRef, store, page);
    return true;
  }
}
