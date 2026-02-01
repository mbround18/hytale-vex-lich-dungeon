package MBRound18.hytale.friends.commands;

import MBRound18.hytale.friends.data.FriendInviteRecord;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.services.FriendsServiceImpl;
import MBRound18.hytale.friends.sound.FriendsSoundService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Objects;
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
    if (targetName == null || targetName.isBlank()) {
      context.sendMessage(Message.raw("Usage: /friend add <name>"));
      return CompletableFuture.completedFuture(null);
    }
    PlayerLookup lookup = findOnlinePlayerByName(targetName);
    if (lookup == null) {
      context.sendMessage(Message.raw("Player not found: " + targetName));
      return CompletableFuture.completedFuture(null);
    }
    UUID ownerId = Objects.requireNonNull(context.sender().getUuid(), "sender uuid");
    UUID targetId = Objects.requireNonNull(lookup.uuid, "target uuid");
    if (friendsService.areFriends(ownerId, targetId)) {
      context.sendMessage(Message.raw("You are already friends with " + lookup.displayName));
      return CompletableFuture.completedFuture(null);
    }
    FriendInviteRecord invite = new FriendInviteRecord(ownerId, targetId, System.currentTimeMillis());
    dataStore.getFriendInvites().put(targetId, invite);
    String ownerName = context.sender().getDisplayName();
    context.sendMessage(Message.raw("Friend invite sent to " + lookup.displayName));
    com.hypixel.hytale.server.core.universe.PlayerRef targetRef = Universe.get().getPlayer(targetId);
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
      for (PlayerRef playerRef : world.getPlayerRefs()) {
        if (playerRef == null) {
          continue;
        }
        String displayName = resolveDisplayName(playerRef);
        String username = playerRef.getUsername();
        if ((displayName != null && displayName.equalsIgnoreCase(name))
            || (username != null && username.equalsIgnoreCase(name))) {
          String resolved = (displayName == null || displayName.isBlank())
              ? (username == null ? "" : username)
              : displayName;
          UUID uuid = playerRef.getUuid();
          if (uuid != null) {
            return new PlayerLookup(uuid, resolved);
          }
        }
      }
    }
    return null;
  }

  @Nonnull
  private String resolveDisplayName(@Nonnull PlayerRef playerRef) {
    String username = playerRef.getUsername();
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return username == null ? "" : username;
    }
    Store<EntityStore> store = ref.getStore();
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      return username == null ? "" : username;
    }
    String displayName = player.getDisplayName();
    if (displayName == null || displayName.isBlank()) {
      return username == null ? "" : username;
    }
    return displayName;
  }

  private static final class PlayerLookup {
    private final @Nonnull UUID uuid;
    private final @Nonnull String displayName;

    private PlayerLookup(@Nonnull UUID uuid, @Nonnull String displayName) {
      this.uuid = Objects.requireNonNull(uuid, "uuid");
      this.displayName = Objects.requireNonNull(displayName, "displayName");
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
