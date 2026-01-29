package MBRound18.hytale.friends.commands;

import MBRound18.hytale.friends.data.FriendInviteRecord;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.services.FriendsServiceImpl;
import MBRound18.hytale.friends.sound.FriendsSoundService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FriendAddCommand extends AbstractCommand {
  private static final String PERMISSION_ADD = "friends.add";
  private final FriendsDataStore dataStore;
  private final FriendsServiceImpl friendsService;

  public FriendAddCommand(@Nonnull FriendsDataStore dataStore, @Nonnull FriendsServiceImpl friendsService) {
    super("add", "Add a friend");
    this.dataStore = dataStore;
    this.friendsService = friendsService;
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    if (!context.sender().hasPermission(PERMISSION_ADD)) {
      context.sendMessage(Message.raw("Missing permission: " + PERMISSION_ADD));
      return CompletableFuture.completedFuture(null);
    }
    String[] tokens = tokenize(context.getInputString());
    int index = skipCommandTokens(tokens, "friend", "add");
    if (index >= tokens.length) {
      context.sendMessage(Message.raw("Usage: /friend add <name>"));
      return CompletableFuture.completedFuture(null);
    }
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("This command can only be used by players."));
      return CompletableFuture.completedFuture(null);
    }
    String targetName = tokens[index];
    PlayerLookup lookup = findOnlinePlayerByName(targetName);
    if (lookup == null) {
      context.sendMessage(Message.raw("Player not found: " + targetName));
      return CompletableFuture.completedFuture(null);
    }
    UUID ownerId = context.sender().getUuid();
    if (friendsService.areFriends(ownerId, lookup.uuid)) {
      context.sendMessage(Message.raw("You are already friends with " + lookup.displayName));
      return CompletableFuture.completedFuture(null);
    }
    FriendInviteRecord invite = new FriendInviteRecord(ownerId, lookup.uuid, System.currentTimeMillis());
    dataStore.getFriendInvites().put(lookup.uuid, invite);
    String ownerName = context.sender().getDisplayName();
    context.sendMessage(Message.raw("Friend invite sent to " + lookup.displayName));
    com.hypixel.hytale.server.core.universe.PlayerRef targetRef = Universe.get().getPlayer(lookup.uuid);
    if (targetRef != null) {
      targetRef.sendMessage(Message.raw(ownerName + " sent you a friend invite. Use /friend accept."));
      FriendsSoundService.play(targetRef, FriendsSoundService.SOUND_INVITE, dataStore.getLog());
    }
    com.hypixel.hytale.server.core.universe.PlayerRef ownerRef = Universe.get().getPlayer(ownerId);
    if (ownerRef != null) {
      FriendsSoundService.play(ownerRef, FriendsSoundService.SOUND_INVITE, dataStore.getLog());
    }
    return CompletableFuture.completedFuture(null);
  }

  @Nullable
  private PlayerLookup findOnlinePlayerByName(@Nonnull String name) {
    for (World world : Universe.get().getWorlds().values()) {
      for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
        if (player.getDisplayName().equalsIgnoreCase(name)) {
          return new PlayerLookup(player.getUuid(), player.getDisplayName());
        }
      }
    }
    return null;
  }

  private static final class PlayerLookup {
    private final UUID uuid;
    private final String displayName;

    private PlayerLookup(UUID uuid, String displayName) {
      this.uuid = uuid;
      this.displayName = displayName;
    }
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
