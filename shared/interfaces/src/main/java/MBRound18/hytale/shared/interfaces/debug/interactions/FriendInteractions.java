package MBRound18.hytale.shared.interfaces.debug.interactions;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractInteraction;
import MBRound18.hytale.shared.interfaces.interfaces.handlers.FriendPageHandler;
import javax.annotation.Nonnull;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FriendInteractions extends AbstractInteraction implements FriendPageHandler {
  private static final Map<String, Boolean> PARTY_STATE = new ConcurrentHashMap<>();

  public FriendInteractions(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Store<EntityStore> store) {
    super(ref, store);
  }

  /**
   * Called when the source player requests to add the target player as a friend.
   *
   * @param source the player initiating the add action
   * @param target the player being added
   */
  public void onAddFriend(
      @Nonnull Player source,
      @Nonnull Player target) {

    target.sendMessage(Message.raw("Player " + source.getDisplayName() + " Wishes To Be You Friend!"));
  }

  @Override
  public void onRemoveFriend(
      @Nonnull Player source,
      @Nonnull Player target) {
    target.sendMessage(Message.raw("Player " + source.getDisplayName() + " removed you as a friend (debug)."));
  }

  @Override
  public void onInviteToParty(
      @Nonnull Player source,
      @Nonnull Player target) {
    String sourceName = source.getDisplayName();
    boolean inParty = PARTY_STATE.getOrDefault(sourceName, false);
    if (inParty) {
      source.sendMessage(Message.raw("You are already in a party (debug). Toggle party state to invite."));
      return;
    }

    target.sendMessage(Message.raw("Player " + sourceName + " invited you to a party (debug)."));
  }

  @Override
  public void onTogglePartyState(@Nonnull Player source) {
    String sourceName = source.getDisplayName();
    boolean current = PARTY_STATE.getOrDefault(sourceName, false);
    boolean next = !current;
    PARTY_STATE.put(sourceName, next);
    source.sendMessage(Message.raw("Party state set to " + (next ? "IN PARTY" : "NOT IN PARTY") + " (debug)."));
  }
}
