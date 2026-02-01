package MBRound18.hytale.friends.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIPage;
import MBRound18.hytale.shared.interfaces.ui.PlayerSubscriptionController;
import MBRound18.hytale.shared.interfaces.ui.UiThread;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.logging.Logger;
import java.util.Objects;
import javax.annotation.Nullable;

public final class FriendsUiOpener {
  private static final Logger LOGGER = Logger.getLogger(FriendsUiOpener.class.getName());
  private static final PlayerSubscriptionController SUBSCRIPTIONS = new PlayerSubscriptionController(
      Objects.requireNonNull(LOGGER, "LOGGER"), "FriendsUiInit");

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
      return openOnThread(playerRef, entityRef, store, Objects.requireNonNull(page, "page"));
    }
    return UiThread.runOnPlayerWorld(playerRef,
        () -> openOnThread(playerRef, entityRef, store, Objects.requireNonNull(page, "page")));
  }

  public static boolean close(@Nullable PlayerRef playerRef) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    Ref<EntityStore> entityRef = playerRef.getReference();
    if (entityRef == null || !entityRef.isValid()) {
      return false;
    }
    Store<EntityStore> store = entityRef.getStore();
    if (store.isInThread()) {
      return closeOnThread(entityRef, store);
    }
    return UiThread.runOnPlayerWorld(playerRef, () -> closeOnThread(entityRef, store));
  }

  private static boolean openOnThread(PlayerRef playerRef, Ref<EntityStore> entityRef, Store<EntityStore> store,
      CustomUIPage page) {
    Player player = store.getComponent(Objects.requireNonNull(entityRef, "entityRef"), Player.getComponentType());
    if (player == null) {
      return false;
    }
    player.getPageManager().openCustomPage(entityRef, store, Objects.requireNonNull(page, "page"));
    applyInitialStateIfSupported(Objects.requireNonNull(playerRef, "playerRef"), page);
    return true;
  }

  private static boolean closeOnThread(Ref<EntityStore> entityRef, Store<EntityStore> store) {
    Player player = store.getComponent(Objects.requireNonNull(entityRef, "entityRef"), Player.getComponentType());
    if (player == null) {
      return false;
    }
    player.getPageManager().setPage(entityRef, store, Page.None);
    return true;
  }

  private static void applyInitialStateIfSupported(PlayerRef playerRef, CustomUIPage page) {
    if (!(page instanceof AbstractCustomUIPage abstractPage)) {
      return;
    }
    SUBSCRIPTIONS.nextTick(Objects.requireNonNull(playerRef, "playerRef"),
        () -> UiThread.runOnPlayerWorld(playerRef, abstractPage::applyInitialState));
  }
}
