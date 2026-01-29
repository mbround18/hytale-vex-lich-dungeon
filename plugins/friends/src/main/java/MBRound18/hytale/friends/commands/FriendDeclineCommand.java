package MBRound18.hytale.friends.commands;

import MBRound18.hytale.friends.data.FriendInviteRecord;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.sound.FriendsSoundService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FriendDeclineCommand extends AbstractCommand {
  private static final String PERMISSION_DECLINE = "friends.decline";
  private final FriendsDataStore dataStore;

  public FriendDeclineCommand(@Nonnull FriendsDataStore dataStore) {
    super("decline", "Decline a friend invite");
    this.dataStore = dataStore;
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    if (!context.sender().hasPermission(PERMISSION_DECLINE)) {
      context.sendMessage(Message.raw("Missing permission: " + PERMISSION_DECLINE));
      return CompletableFuture.completedFuture(null);
    }
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("This command can only be used by players."));
      return CompletableFuture.completedFuture(null);
    }
    UUID targetId = context.sender().getUuid();
    FriendInviteRecord invite = dataStore.getFriendInvites().remove(targetId);
    if (invite == null) {
      context.sendMessage(Message.raw("No friend invite found."));
      return CompletableFuture.completedFuture(null);
    }
    String targetName = context.sender().getDisplayName();
    context.sendMessage(Message.raw("Friend invite declined."));

    com.hypixel.hytale.server.core.universe.PlayerRef targetRef = Universe.get().getPlayer(targetId);
    com.hypixel.hytale.server.core.universe.PlayerRef inviterRef = Universe.get().getPlayer(invite.getInviterId());
    if (targetRef != null) {
      FriendsSoundService.play(targetRef, FriendsSoundService.SOUND_DECLINE, dataStore.getLog());
    }
    if (inviterRef != null) {
      inviterRef.sendMessage(Message.raw(targetName + " declined your friend invite."));
      FriendsSoundService.play(inviterRef, FriendsSoundService.SOUND_DECLINE, dataStore.getLog());
    }
    return CompletableFuture.completedFuture(null);
  }
}
