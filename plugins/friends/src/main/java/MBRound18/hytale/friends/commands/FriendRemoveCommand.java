package MBRound18.hytale.friends.commands;

import MBRound18.hytale.friends.data.FriendRecord;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.services.FriendsServiceImpl;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FriendRemoveCommand extends AbstractCommand {
  private static final String PERMISSION_REMOVE = "friends.remove";
  private final FriendsDataStore dataStore;
  private final FriendsServiceImpl friendsService;

  public FriendRemoveCommand(@Nonnull FriendsDataStore dataStore, @Nonnull FriendsServiceImpl friendsService) {
    super("remove", "Remove a friend");
    this.dataStore = dataStore;
    this.friendsService = friendsService;
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    if (!context.sender().hasPermission(PERMISSION_REMOVE)) {
      context.sendMessage(Message.raw("Missing permission: " + PERMISSION_REMOVE));
      return CompletableFuture.completedFuture(null);
    }
    String[] tokens = tokenize(context.getInputString());
    int index = skipCommandTokens(tokens, "friend", "remove");
    if (index >= tokens.length) {
      context.sendMessage(Message.raw("Usage: /friend remove <name>"));
      return CompletableFuture.completedFuture(null);
    }
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("This command can only be used by players."));
      return CompletableFuture.completedFuture(null);
    }
    String targetName = tokens[index];
    UUID ownerId = context.sender().getUuid();
    UUID targetId = findOnlinePlayerUuidByName(targetName);
    if (targetId == null) {
      targetId = findFriendUuidByName(ownerId, targetName);
    }
    if (targetId == null) {
      context.sendMessage(Message.raw("Player not found: " + targetName));
      return CompletableFuture.completedFuture(null);
    }
    boolean removed = friendsService.removeFriend(ownerId, targetId);
    if (!removed) {
      context.sendMessage(Message.raw("Unable to remove friend."));
      return CompletableFuture.completedFuture(null);
    }
    context.sendMessage(Message.raw("Removed friend: " + targetName));
    return CompletableFuture.completedFuture(null);
  }

  @Nullable
  private UUID findOnlinePlayerUuidByName(@Nonnull String name) {
    for (World world : Universe.get().getWorlds().values()) {
      for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
        if (player.getDisplayName().equalsIgnoreCase(name)) {
          return player.getUuid();
        }
      }
    }
    return null;
  }

  @Nullable
  private UUID findFriendUuidByName(@Nonnull UUID ownerId, @Nonnull String name) {
    Map<UUID, FriendRecord> friends = dataStore.getFriends().get(ownerId);
    if (friends == null) {
      return null;
    }
    for (FriendRecord record : friends.values()) {
      if (record.getName().equalsIgnoreCase(name)) {
        return record.getUuid();
      }
    }
    return null;
  }

  private static String[] tokenize(String input) {
    String trimmed = input == null ? "" : input.trim();
    if (trimmed.isEmpty()) {
      return new String[0];
    }
    return trimmed.split("\\s+");
  }

  private static int skipCommandTokens(String[] tokens, String root, String command) {
    int index = 0;
    if (index < tokens.length && root.equalsIgnoreCase(tokens[index])) {
      index++;
    }
    if (index < tokens.length && command.equalsIgnoreCase(tokens[index])) {
      index++;
    }
    return index;
  }
}
