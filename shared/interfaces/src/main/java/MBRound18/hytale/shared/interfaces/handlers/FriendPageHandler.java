package MBRound18.hytale.shared.interfaces.handlers;

import com.hypixel.hytale.server.core.entity.entities.Player;

/**
 * Handles user actions coming from a friends-related UI page.
 *
 * <p>
 * Implementations should perform the appropriate server-side operations
 * when a player interacts with the UI (add, remove, cancel, reject), and
 * should tolerate missing or invalid context via {@link #onUnknown()}.
 * </p>
 */
public interface FriendPageHandler {

  /**
   * Called when the source player requests to add the target player as a friend.
   *
   * @param source the player initiating the add action
   * @param target the player being added
   */
  void onAddFriend(

      Player source,
      Player target);

  // /**
  // * Called when the source player requests to remove the target player.
  // *
  // * @param source the player initiating the removal
  // * @param target the player being removed
  // */
  // void onRemoveFriend(PlayerRef source, PlayerRef target);

  // /**
  // * Called when the source player cancels the current friends UI flow.
  // *
  // * @param source the player canceling the flow
  // * @param target the player the flow was targeting (if available)
  // */
  // void onCancel(PlayerRef source, PlayerRef target);

  // /**
  // * Called when the source player rejects a friends request from the target
  // * player.
  // *
  // * @param source the player rejecting the request
  // * @param target the player whose request was rejected
  // */
  // void onReject(PlayerRef source, PlayerRef target);

  // /**
  // * Called when the UI action cannot be mapped to a known handler.
  // */
  // void onUnknown();

}