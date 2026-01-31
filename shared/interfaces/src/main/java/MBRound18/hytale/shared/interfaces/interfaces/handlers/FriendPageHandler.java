package MBRound18.hytale.shared.interfaces.interfaces.handlers;

import javax.annotation.Nonnull;

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
      @Nonnull Player source,
      @Nonnull Player target);

  /**
   * Called when the source player requests to remove the target player.
   *
   * @param source the player initiating the removal
   * @param target the player being removed
   */
  void onRemoveFriend(
      @Nonnull Player source,
      @Nonnull Player target);

  /**
   * Called when the source player requests to invite the target player to a party.
   *
   * @param source the player initiating the invite
   * @param target the player being invited
   */
  void onInviteToParty(
      @Nonnull Player source,
      @Nonnull Player target);

  /**
   * Debug helper to flip whether the source player is already in a party.
   *
   * @param source the player toggling their party state
   */
  void onTogglePartyState(
      @Nonnull Player source);

}
