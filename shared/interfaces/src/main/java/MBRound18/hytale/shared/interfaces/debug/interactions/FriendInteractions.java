package MBRound18.hytale.shared.interfaces.debug.interactions;

import MBRound18.hytale.shared.interfaces.handlers.FriendPageHandler;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;

public class FriendInteractions implements FriendPageHandler {
  /**
   * Called when the source player requests to add the target player as a friend.
   *
   * @param source the player initiating the add action
   * @param target the player being added
   */
  public void onAddFriend(Player source, Player target) {

    target.sendMessage(Message.raw("Player " + source.getDisplayName() + " Wishes To Be You Friend!"));
  }
}
