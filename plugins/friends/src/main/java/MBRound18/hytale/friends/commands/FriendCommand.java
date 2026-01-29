package MBRound18.hytale.friends.commands;

import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.services.FriendsServiceImpl;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import javax.annotation.Nonnull;

public class FriendCommand extends AbstractCommandCollection {
  public FriendCommand(@Nonnull FriendsDataStore dataStore, @Nonnull FriendsServiceImpl friendsService) {
    super("friend", "Manage friends");
    addSubCommand(new FriendAddCommand(dataStore, friendsService));
    addSubCommand(new FriendAcceptCommand(dataStore, friendsService));
    addSubCommand(new FriendDeclineCommand(dataStore));
    addSubCommand(new FriendRemoveCommand(dataStore, friendsService));
  }
}
