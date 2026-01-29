package MBRound18.hytale.friends.ui;

import MBRound18.ImmortalEngine.api.ui.EngineHud;
import MBRound18.ImmortalEngine.api.ui.UiThread;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FriendsHudController {
  private static final ScheduledExecutorService HUD_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "FriendsHudUpdate");
    thread.setDaemon(true);
    return thread;
  });
  private static final long HUD_INITIAL_UPDATE_DELAY_MS = 1000L;
  private FriendsHudController() {
  }

  public static boolean openPartyHud(@Nullable PlayerRef playerRef, @Nonnull String partyList) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    if (!EngineHud.isCustomUiMode()) {
      EngineHud.show(playerRef, "Custom/Friends/Hud/FriendsPartyHud.ui",
          java.util.Map.of("FriendsPartyList", partyList == null ? "" : partyList));
      return true;
    }
    CustomUIHud hud = new FriendsHudPage(playerRef, "Custom/Friends/Hud/FriendsPartyHud.ui",
        java.util.Map.of("FriendsPartyList", partyList == null ? "" : partyList));
    return applyHud(playerRef, hud);
  }

  public static boolean clearHud(@Nullable PlayerRef playerRef) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    if (!EngineHud.isCustomUiMode()) {
      EngineHud.clear(playerRef);
      return true;
    }
    return resetHud(playerRef);
  }

  private static boolean applyHud(@Nonnull PlayerRef playerRef, @Nonnull CustomUIHud hud) {
    Ref<EntityStore> entityRef = playerRef.getReference();
    if (entityRef == null || !entityRef.isValid()) {
      return false;
    }
    Store<EntityStore> store = entityRef.getStore();
    if (store.isInThread()) {
      return applyHudOnThread(playerRef, entityRef, store, hud);
    }
    return UiThread.runOnPlayerWorld(playerRef, () -> applyHudOnThread(playerRef, entityRef, store, hud));
  }

  private static boolean resetHud(@Nonnull PlayerRef playerRef) {
    Ref<EntityStore> entityRef = playerRef.getReference();
    if (entityRef == null || !entityRef.isValid()) {
      return false;
    }
    Store<EntityStore> store = entityRef.getStore();
    if (store.isInThread()) {
      return resetHudOnThread(playerRef, entityRef, store);
    }
    return UiThread.runOnPlayerWorld(playerRef, () -> resetHudOnThread(playerRef, entityRef, store));
  }

  private static boolean applyHudOnThread(PlayerRef playerRef, Ref<EntityStore> entityRef,
      Store<EntityStore> store, CustomUIHud hud) {
    Player player = store.getComponent(entityRef, Player.getComponentType());
    if (player == null) {
      return false;
    }
    player.getHudManager().setCustomHud(playerRef, hud);
    if (hud instanceof FriendsHudPage friendsHud) {
      scheduleInitialUpdate(playerRef, friendsHud);
    }
    return true;
  }

  private static boolean resetHudOnThread(PlayerRef playerRef, Ref<EntityStore> entityRef,
      Store<EntityStore> store) {
    Player player = store.getComponent(entityRef, Player.getComponentType());
    if (player == null) {
      return false;
    }
    player.getHudManager().setCustomHud(playerRef, null);
    return true;
  }

  private static void scheduleInitialUpdate(@Nonnull PlayerRef playerRef, @Nonnull FriendsHudPage hud) {
    HUD_SCHEDULER.schedule(() -> UiThread.runOnPlayerWorld(playerRef, () -> {
      UICommandBuilder builder = new UICommandBuilder();
      hud.appendVarCommands(builder, hud.getVars());
      hud.update(false, builder);
    }), HUD_INITIAL_UPDATE_DELAY_MS, TimeUnit.MILLISECONDS);
  }
}
