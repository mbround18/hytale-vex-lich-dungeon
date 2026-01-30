package MBRound18.hytale.shared.interfaces.pages;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import MBRound18.hytale.shared.interfaces.codecs.FriendFinder;
import MBRound18.hytale.shared.interfaces.handlers.FriendPageHandler;

public class FriendListPage extends InteractiveCustomUIPage<FriendFinder> {
  private final FriendPageHandler handler;

  public FriendListPage(PlayerRef playerRef, FriendPageHandler handler) {
    this.handler = handler;

    super(
        /**
         * PlayerRef which player sees the UI
         */
        playerRef,
        /**
         * The lifetime of the page
         */
        CustomPageLifetime.CanDismissOrCloseThroughInteraction, /**
                                                                 * Codec to serialize/deserialize data between UI and
                                                                 * server
                                                                 */
        FriendFinder.CODEC);
  }

  @Override
  public void build(
      Ref<EntityStore> ref,
      UICommandBuilder builder,
      UIEventBuilder events,
      Store<EntityStore> store) {
    // Relative path from Common/UI/Custom/*
    builder.append("Pages/FriendsListPage.ui");

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#AddFriendButton",
        new EventData().append("@FriendSearch", "#FriendSearch.Value")); // On trigger of button, take value of
                                                                         // #FriendSearch and map to @FriendSearch
  }

  @Override
  public void handleDataEvent(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Store<EntityStore> store,
      @Nonnull FriendFinder data) {

    Player source = (Player) store.getComponent(ref, Player.getComponentType());
    Universe universe = Universe.get();

    if (universe == null) {
      return;
    }

    if (data.friendSearch != null || !data.friendSearch.isEmpty()) {
      PlayerRef targetPlayer = universe.getPlayerByUsername(data.friendSearch,
          NameMatching.STARTS_WITH_IGNORE_CASE);
      if (targetPlayer == null) {
        return;
      }

      Player target = (Player) store.getComponent(targetPlayer
          .getReference(),
          Player.getComponentType());

      handler.onAddFriend(source, target);
    }

    source.getPageManager().setPage(ref, store, Page.None);

  }

}
