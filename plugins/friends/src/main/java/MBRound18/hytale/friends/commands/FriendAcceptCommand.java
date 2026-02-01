package MBRound18.hytale.friends.commands;

import MBRound18.hytale.friends.data.FriendInviteRecord;
import MBRound18.hytale.friends.data.FriendRecord;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.services.FriendsServiceImpl;
import MBRound18.hytale.friends.sound.FriendsSoundService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FriendAcceptCommand extends AbstractCommand {
  private static final String PERMISSION_ACCEPT = "friends.accept";
  private final FriendsDataStore dataStore;
  private final FriendsServiceImpl friendsService;

  public FriendAcceptCommand(@Nonnull FriendsDataStore dataStore, @Nonnull FriendsServiceImpl friendsService) {
    super("accept", "Accept a friend invite");
    this.dataStore = dataStore;
    this.friendsService = friendsService;
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    if (!context.sender().hasPermission(PERMISSION_ACCEPT)) {
      context.sendMessage(Message.raw("Missing permission: " + PERMISSION_ACCEPT));
      return CompletableFuture.completedFuture(null);
    }
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("This command can only be used by players."));
      return CompletableFuture.completedFuture(null);
    }
    UUID targetId = Objects.requireNonNull(context.sender().getUuid(), "targetId");
    FriendInviteRecord invite = dataStore.getFriendInvites().remove(targetId);
    if (invite == null) {
      context.sendMessage(Message.raw("No friend invite found."));
      return CompletableFuture.completedFuture(null);
    }
    String targetName = context.sender().getDisplayName();
    targetName = targetName == null ? "" : targetName;
    boolean added = friendsService.addFriend(invite.getInviterId(), targetId, targetName);
    if (!added) {
      context.sendMessage(Message.raw("Unable to accept friend invite."));
      return CompletableFuture.completedFuture(null);
    }
    Map<UUID, FriendRecord> inviterMap = dataStore.getFriends().get(invite.getInviterId());
    if (inviterMap != null) {
      FriendRecord record = inviterMap.get(targetId);
      if (record != null) {
        record.setName(Objects.requireNonNull(targetName, "targetName"));
      }
    }
    dataStore.saveAll();
    context.sendMessage(Message.raw("Friend invite accepted."));

    com.hypixel.hytale.server.core.universe.PlayerRef targetRef = Universe.get().getPlayer(targetId);
    com.hypixel.hytale.server.core.universe.PlayerRef inviterRef = Universe.get().getPlayer(invite.getInviterId());
    if (targetRef != null) {
      FriendsSoundService.play(targetRef, FriendsSoundService.SOUND_ACCEPT, dataStore.getLog());
    }
    if (inviterRef != null) {
      inviterRef.sendMessage(Message.raw(targetName + " accepted your friend invite."));
      FriendsSoundService.play(inviterRef, FriendsSoundService.SOUND_ACCEPT, dataStore.getLog());
    }
    return CompletableFuture.completedFuture(null);
  }
}
