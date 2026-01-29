package MBRound18.hytale.friends.commands;

import MBRound18.ImmortalEngine.api.social.PartyActionResult;
import MBRound18.ImmortalEngine.api.social.PartyInvite;
import MBRound18.ImmortalEngine.api.social.PartyService;
import MBRound18.ImmortalEngine.api.social.PartySnapshot;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.sound.FriendsSoundService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PartyCommand extends AbstractCommand {
  private static final String PERMISSION_USE = "party.use";
  private static final String PERMISSION_CREATE = "party.create";
  private static final String PERMISSION_INVITE = "party.invite";
  private static final String PERMISSION_JOIN = "party.join";
  private static final String PERMISSION_DECLINE = "party.decline";
  private static final String PERMISSION_LEAVE = "party.leave";
  private static final String PERMISSION_DISBAND = "party.disband";
  private final FriendsDataStore dataStore;
  private final PartyService partyService;

  public PartyCommand(@Nonnull FriendsDataStore dataStore, @Nonnull PartyService partyService) {
    super("party", "Party management");
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
    String[] tokens = tokenize(context.getInputString());
    int index = skipCommandTokens(tokens, "party");
    if (index >= tokens.length) {
      sendPartyStatus(context, playerId);
      return CompletableFuture.completedFuture(null);
    }
    String sub = tokens[index].toLowerCase(java.util.Locale.ROOT);
    switch (sub) {
      case "new":
        if (!context.sender().hasPermission(PERMISSION_CREATE)) {
          context.sendMessage(Message.raw("Missing permission: " + PERMISSION_CREATE));
          return CompletableFuture.completedFuture(null);
        }
        respond(context, partyService.createParty(playerId, context.sender().getDisplayName()));
        return CompletableFuture.completedFuture(null);
      case "invite":
        if (!context.sender().hasPermission(PERMISSION_INVITE)) {
          context.sendMessage(Message.raw("Missing permission: " + PERMISSION_INVITE));
          return CompletableFuture.completedFuture(null);
        }
        if (index + 1 >= tokens.length) {
          context.sendMessage(Message.raw("Usage: /party invite <name>"));
          return CompletableFuture.completedFuture(null);
        }
        return invitePlayer(context, playerId, tokens[index + 1]);
      case "join":
        if (!context.sender().hasPermission(PERMISSION_JOIN)) {
          context.sendMessage(Message.raw("Missing permission: " + PERMISSION_JOIN));
          return CompletableFuture.completedFuture(null);
        }
        Optional<PartyInvite> invite = partyService.getInvite(playerId);
        respond(context, partyService.acceptInvite(playerId));
        if (invite.isPresent()) {
          playInviteAcceptedSounds(playerId, invite.get().getInviterId());
        }
        return CompletableFuture.completedFuture(null);
      case "decline":
        if (!context.sender().hasPermission(PERMISSION_DECLINE)) {
          context.sendMessage(Message.raw("Missing permission: " + PERMISSION_DECLINE));
          return CompletableFuture.completedFuture(null);
        }
        Optional<PartyInvite> declineInvite = partyService.getInvite(playerId);
        respond(context, partyService.declineInvite(playerId));
        if (declineInvite.isPresent()) {
          playInviteDeclinedSounds(playerId, declineInvite.get().getInviterId());
        }
        return CompletableFuture.completedFuture(null);
      case "leave":
        if (!context.sender().hasPermission(PERMISSION_LEAVE)) {
          context.sendMessage(Message.raw("Missing permission: " + PERMISSION_LEAVE));
          return CompletableFuture.completedFuture(null);
        }
        respond(context, partyService.leave(playerId));
        return CompletableFuture.completedFuture(null);
      case "disband":
        if (!context.sender().hasPermission(PERMISSION_DISBAND)) {
          context.sendMessage(Message.raw("Missing permission: " + PERMISSION_DISBAND));
          return CompletableFuture.completedFuture(null);
        }
        respond(context, partyService.disband(playerId));
        return CompletableFuture.completedFuture(null);
      default:
        context.sendMessage(Message.raw("Usage: /party [new|invite|join|decline|leave|disband]"));
        return CompletableFuture.completedFuture(null);
    }
  }

  private CompletableFuture<Void> invitePlayer(@Nonnull CommandContext context, @Nonnull UUID inviterId,
      @Nonnull String targetName) {
    PlayerLookup target = findOnlinePlayerByName(targetName);
    if (target == null) {
      context.sendMessage(Message.raw("Player not found: " + targetName));
      return CompletableFuture.completedFuture(null);
    }
    PartyActionResult result = partyService.invite(inviterId, context.sender().getDisplayName(),
        target.uuid, target.displayName);
    respond(context, result);
    if (result.isSuccess()) {
      PlayerRef targetRef = Universe.get().getPlayer(target.uuid);
      if (targetRef != null) {
        targetRef.sendMessage(Message.raw("Party invite from " + context.sender().getDisplayName()
            + ". Use /party join to accept."));
        FriendsSoundService.play(targetRef, FriendsSoundService.SOUND_INVITE, dataStore.getLog());
      }
      PlayerRef inviterRef = Universe.get().getPlayer(inviterId);
      if (inviterRef != null) {
        FriendsSoundService.play(inviterRef, FriendsSoundService.SOUND_INVITE, dataStore.getLog());
      }
    }
    return CompletableFuture.completedFuture(null);
  }

  private void sendPartyStatus(@Nonnull CommandContext context, @Nonnull UUID playerId) {
    Optional<PartySnapshot> party = partyService.getParty(playerId);
    if (party.isEmpty()) {
      context.sendMessage(Message.raw("You are not in a party."));
      return;
    }
    PartySnapshot snapshot = party.get();
    context.sendMessage(Message.raw("Party members: " + snapshot.getMembers().size()));
  }

  private void respond(@Nonnull CommandContext context, @Nonnull PartyActionResult result) {
    String message = result.getMessage() == null
        ? (result.isSuccess() ? "OK" : "Failed")
        : result.getMessage();
    context.sendMessage(Message.raw(message));
  }

  private void playInviteAcceptedSounds(@Nonnull UUID targetId, @Nonnull UUID inviterId) {
    PlayerRef targetRef = Universe.get().getPlayer(targetId);
    PlayerRef inviterRef = Universe.get().getPlayer(inviterId);
    if (targetRef != null) {
      FriendsSoundService.play(targetRef, FriendsSoundService.SOUND_ACCEPT, dataStore.getLog());
    }
    if (inviterRef != null) {
      FriendsSoundService.play(inviterRef, FriendsSoundService.SOUND_ACCEPT, dataStore.getLog());
    }
  }

  private void playInviteDeclinedSounds(@Nonnull UUID targetId, @Nonnull UUID inviterId) {
    PlayerRef targetRef = Universe.get().getPlayer(targetId);
    PlayerRef inviterRef = Universe.get().getPlayer(inviterId);
    if (targetRef != null) {
      FriendsSoundService.play(targetRef, FriendsSoundService.SOUND_DECLINE, dataStore.getLog());
    }
    if (inviterRef != null) {
      FriendsSoundService.play(inviterRef, FriendsSoundService.SOUND_DECLINE, dataStore.getLog());
    }
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

  private static int skipCommandTokens(String[] tokens, String command) {
    int index = 0;
    if (index < tokens.length && command.equalsIgnoreCase(tokens[index])) {
      index++;
    }
    return index;
  }
}
