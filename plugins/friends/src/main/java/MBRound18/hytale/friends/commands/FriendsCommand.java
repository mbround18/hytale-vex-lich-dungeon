package MBRound18.hytale.friends.commands;

import MBRound18.ImmortalEngine.api.social.PartyService;
import MBRound18.hytale.friends.data.FriendRecord;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.ui.FriendsUiController;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FriendsCommand extends AbstractCommand {
  private static final String PERMISSION_USE = "friends.use";
  private final FriendsDataStore dataStore;
  private final PartyService partyService;

  public FriendsCommand(@Nonnull FriendsDataStore dataStore, @Nonnull PartyService partyService) {
    super("friends", "Open your friends list");
    this.dataStore = dataStore;
    this.partyService = partyService;
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    if (!context.sender().hasPermission(PERMISSION_USE)) {
      context.sendMessage(Message.raw("Missing permission: " + PERMISSION_USE));
      return CompletableFuture.completedFuture(null);
    }
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("This command can only be used by players."));
      return CompletableFuture.completedFuture(null);
    }

    UUID playerId = context.sender().getUuid();
    PlayerRef playerRef = Universe.get().getPlayer(playerId);
    if (playerRef == null) {
      context.sendMessage(Message.raw("Unable to open friends list."));
      return CompletableFuture.completedFuture(null);
    }

    String partyStatus = buildPartyStatus(playerId);
    String friendList = buildFriendList(playerId);
    World world = Universe.get().getWorld(playerRef.getWorldUuid());
    if (world == null) {
      context.sendMessage(Message.raw("Failed to open friends UI."));
      return CompletableFuture.completedFuture(null);
    }
    world.execute(() -> {
      boolean opened = FriendsUiController.openFriendsList(playerRef, partyStatus, friendList);
      if (!opened) {
        context.sendMessage(Message.raw("Failed to open friends UI."));
      }
    });
    return CompletableFuture.completedFuture(null);
  }

  private String buildPartyStatus(@Nonnull UUID playerId) {
    return partyService.getParty(playerId)
        .map(party -> "Party: " + party.getMembers().size() + " member(s)")
        .orElse("Party: none");
  }

  private String buildFriendList(@Nonnull UUID playerId) {
    Map<UUID, FriendRecord> friends = dataStore.getFriends().get(playerId);
    if (friends == null || friends.isEmpty()) {
      return "No friends yet.";
    }
    List<FriendRecord> list = new ArrayList<>(friends.values());
    list.sort(Comparator.comparing(FriendRecord::getName, String.CASE_INSENSITIVE_ORDER));
    StringBuilder builder = new StringBuilder();
    for (FriendRecord record : list) {
      if (builder.length() > 0) {
        builder.append("\n");
      }
      boolean online = isOnline(record.getUuid());
      builder.append(record.getName())
          .append(online ? " (online)" : " (offline)");
    }
    return builder.toString();
  }

  private boolean isOnline(@Nonnull UUID uuid) {
    PlayerRef playerRef = Universe.get().getPlayer(uuid);
    return playerRef != null && playerRef.isValid();
  }
}
