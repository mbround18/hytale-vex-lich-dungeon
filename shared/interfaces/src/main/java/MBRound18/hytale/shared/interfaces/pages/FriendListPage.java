package MBRound18.hytale.shared.interfaces.pages;

import javax.annotation.Nonnull;
import java.util.Objects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import MBRound18.hytale.shared.interfaces.ui.generated.PagesFriendslistpageUi;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractInteractivePage;
import MBRound18.hytale.shared.interfaces.codecs.FriendFinder;
import MBRound18.hytale.shared.interfaces.interfaces.handlers.FriendPageHandler;

public class FriendListPage extends AbstractInteractivePage<FriendFinder> {
  private static final String UI_PATH = PagesFriendslistpageUi.UI_PATH;
  private final FriendPageHandler handler;

  public FriendListPage(@Nonnull PlayerRef playerRef, @Nonnull FriendPageHandler handler) {
    this.handler = Objects.requireNonNull(handler, "handler");

    super(
        /**
         * PlayerRef which player sees the UI
         */
        Objects.requireNonNull(playerRef, "playerRef"),
        /**
         * The lifetime of the page
         */
        CustomPageLifetime.CanDismissOrCloseThroughInteraction, /**
                                                                 * Codec to serialize/deserialize data between UI and
                                                                 * server
                                                                 */
        Objects.requireNonNull(FriendFinder.CODEC, "FriendFinder.CODEC"));
  }

  @Override
  public void build(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull UICommandBuilder builder,
      @Nonnull UIEventBuilder events,
      @Nonnull Store<EntityStore> store) {
    // POM-generated UI path
    builder.append(UI_PATH);

    events.addEventBinding(CustomUIEventBindingType.Activating, "#AddFriendButton",
        false);

    // events.addEventBinding(CustomUIEventBindingType.ValueChanged,
    // "#AddFriendButton",
    // EventData
    // .of("@AddFriendButton", "#AddFriendButton.Value")
    // .append("@FriendSearch", "#FriendSearch.Value"),
    // false);

    // events.addEventBinding(
    // CustomUIEventBindingType.ValueChanged,
    // "#RemoveFriendButton",
    // EventData.of("@RemoveFriendButton", "#RemoveFriendButton.Value")
    // .append("@FriendSearch", "#FriendSearch.Value"),
    // false);

    // events.addEventBinding(
    // CustomUIEventBindingType.ValueChanged,
    // "#InvitePartyButton",
    // EventData.of("@InvitePartyButton", "#InvitePartyButton.Value")
    // .append("@FriendSearch", "#FriendSearch.Value"),
    // false);

    // events.addEventBinding(
    // CustomUIEventBindingType.ValueChanged,
    // "#TogglePartyStateButton",
    // EventData.of("@TogglePartyStateButton", "#TogglePartyStateButton.Value"),
    // false);
  }

  @Override
  public void handleDataEvent(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Store<EntityStore> store,
      @Nonnull FriendFinder data) {
    @Nonnull
    Player source = (Player) Objects.requireNonNull(store.getComponent(ref, Player.getComponentType()), "source");
    Universe universe = playerHelpers.getUniverse();

    if (data.action == null) {
      return;
    }

    boolean shouldClose = true;

    switch (data.action) {
      case TOGGLE_PARTY_STATE:
        handler.onTogglePartyState(source);
        shouldClose = false;
        break;
      case ADD_FRIEND:
      case REMOVE_FRIEND:
      case INVITE_TO_PARTY:
        if (data.friendSearch == null || data.friendSearch.isEmpty()) {
          return;
        }

        String friendName = Objects.requireNonNull(data.friendSearch, "friendSearch");
        PlayerRef targetPlayer = universe.getPlayerByUsername(friendName,
            NameMatching.STARTS_WITH_IGNORE_CASE);
        if (targetPlayer == null) {
          return;
        }

        Ref<EntityStore> targetRef = targetPlayer.getReference();
        if (targetRef == null) {
          return;
        }

        Player target = playerHelpers.getPlayerByRef(store, targetPlayer);

        if (data.action == FriendFinder.Action.ADD_FRIEND) {
          handler.onAddFriend(source, target);
        } else if (data.action == FriendFinder.Action.REMOVE_FRIEND) {
          handler.onRemoveFriend(source, target);
        } else {
          handler.onInviteToParty(source, target);
        }
        break;
      default:
        return;
    }

    if (shouldClose) {
      source.getPageManager().setPage(ref, store, Page.None);
    }

  }

}
