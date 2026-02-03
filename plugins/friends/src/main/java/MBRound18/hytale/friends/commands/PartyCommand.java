package MBRound18.hytale.friends.commands;

import MBRound18.ImmortalEngine.api.social.PartyActionResult;
import MBRound18.ImmortalEngine.api.social.PartyInvite;
import MBRound18.ImmortalEngine.api.social.PartyService;
import MBRound18.ImmortalEngine.api.social.PartySnapshot;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.sound.FriendsSoundService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Objects;
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
    addSubCommand(new PartyNewCommand());
    addSubCommand(new PartyInviteCommand());
    addSubCommand(new PartyJoinCommand());
    addSubCommand(new PartyDeclineCommand());
    addSubCommand(new PartyLeaveCommand());
    addSubCommand(new PartyDisbandCommand());
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    UUID playerId = requirePlayerId(context, PERMISSION_USE);
    if (playerId == null) {
      return CompletableFuture.completedFuture(null);
    }
    String[] tokens = tokenize(context.getInputString());
    int index = skipCommandTokens(tokens, "party");
    if (index < tokens.length) {
      context.sendMessage(Message.raw("Usage: /party [new|invite|join|decline|leave|disband]"));
      return CompletableFuture.completedFuture(null);
    }
    sendPartyStatus(context, playerId);
    return CompletableFuture.completedFuture(null);
  }

  private boolean checkPermission(@Nonnull CommandContext context, @Nonnull String permission) {
    if (context.sender().hasPermission(permission)) {
      return true;
    }
    context.sendMessage(Message.raw("Missing permission: " + permission));
    return false;
  }

  @Nullable
  private UUID requirePlayerId(@Nonnull CommandContext context) {
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("This command can only be used by players."));
      return null;
    }
    UUID playerId = context.sender().getUuid();
    if (playerId == null) {
      context.sendMessage(Message.raw("Unable to resolve player id."));
      return null;
    }
    return playerId;
  }

  @Nullable
  private UUID requirePlayerId(@Nonnull CommandContext context, @Nonnull String permission) {
    if (!checkPermission(context, permission)) {
      return null;
    }
    return requirePlayerId(context);
  }

  private final class PartyNewCommand extends AbstractCommand {
    private PartyNewCommand() {
      super("new", "Create a new party");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      UUID playerId = requirePlayerId(context, PERMISSION_USE);
      if (playerId == null) {
        return CompletableFuture.completedFuture(null);
      }
      if (!checkPermission(context, PERMISSION_CREATE)) {
        return CompletableFuture.completedFuture(null);
      }
      String displayName = context.sender().getDisplayName();
      respond(context, partyService.createParty(playerId, displayName == null ? "" : displayName));
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class PartyInviteCommand extends AbstractCommand {
    private final RequiredArg<String> nameArg;

    @SuppressWarnings("null")
    private PartyInviteCommand() {
      super("invite", "Invite a player to your party");
      this.nameArg = Objects.requireNonNull(
          withRequiredArg("name", "Player name", ArgTypes.STRING), "nameArg");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      UUID playerId = requirePlayerId(context, PERMISSION_USE);
      if (playerId == null) {
        return CompletableFuture.completedFuture(null);
      }
      if (!checkPermission(context, PERMISSION_INVITE)) {
        return CompletableFuture.completedFuture(null);
      }
      String targetName = context.get(Objects.requireNonNull(nameArg, "nameArg"));
      if (targetName == null || targetName.isBlank()) {
        context.sendMessage(Message.raw("Usage: /party invite <name>"));
        return CompletableFuture.completedFuture(null);
      }
      return invitePlayer(context, playerId, targetName);
    }
  }

  private final class PartyJoinCommand extends AbstractCommand {
    private PartyJoinCommand() {
      super("join", "Join a party via invite");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      UUID playerId = requirePlayerId(context, PERMISSION_USE);
      if (playerId == null) {
        return CompletableFuture.completedFuture(null);
      }
      if (!checkPermission(context, PERMISSION_JOIN)) {
        return CompletableFuture.completedFuture(null);
      }
      Optional<PartyInvite> invite = partyService.getInvite(playerId);
      respond(context, partyService.acceptInvite(playerId));
      if (invite.isPresent()) {
        playInviteAcceptedSounds(playerId, invite.get().getInviterId());
      }
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class PartyDeclineCommand extends AbstractCommand {
    private PartyDeclineCommand() {
      super("decline", "Decline a party invite");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      UUID playerId = requirePlayerId(context, PERMISSION_USE);
      if (playerId == null) {
        return CompletableFuture.completedFuture(null);
      }
      if (!checkPermission(context, PERMISSION_DECLINE)) {
        return CompletableFuture.completedFuture(null);
      }
      Optional<PartyInvite> invite = partyService.getInvite(playerId);
      respond(context, partyService.declineInvite(playerId));
      if (invite.isPresent()) {
        playInviteDeclinedSounds(playerId, invite.get().getInviterId());
      }
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class PartyLeaveCommand extends AbstractCommand {
    private PartyLeaveCommand() {
      super("leave", "Leave your current party");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      UUID playerId = requirePlayerId(context, PERMISSION_USE);
      if (playerId == null) {
        return CompletableFuture.completedFuture(null);
      }
      if (!checkPermission(context, PERMISSION_LEAVE)) {
        return CompletableFuture.completedFuture(null);
      }
      respond(context, partyService.leave(playerId));
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class PartyDisbandCommand extends AbstractCommand {
    private PartyDisbandCommand() {
      super("disband", "Disband your party");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      UUID playerId = requirePlayerId(context, PERMISSION_USE);
      if (playerId == null) {
        return CompletableFuture.completedFuture(null);
      }
      if (!checkPermission(context, PERMISSION_DISBAND)) {
        return CompletableFuture.completedFuture(null);
      }
      respond(context, partyService.disband(playerId));
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
    String displayName = context.sender().getDisplayName();
    PartyActionResult result = partyService.invite(inviterId, displayName == null ? "" : displayName,
        Objects.requireNonNull(target.uuid, "target uuid"),
        Objects.requireNonNull(target.displayName, "target display"));
    respond(context, result);
    if (result.isSuccess()) {
      PlayerRef targetRef = Universe.get().getPlayer(Objects.requireNonNull(target.uuid, "target uuid"));
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
    context.sendMessage(Message.raw(Objects.requireNonNull(message, "message")));
  }

  private void playInviteAcceptedSounds(@Nonnull UUID targetId, @Nonnull UUID inviterId) {
    PlayerRef targetRef = Universe.get().getPlayer(Objects.requireNonNull(targetId, "targetId"));
    PlayerRef inviterRef = Universe.get().getPlayer(Objects.requireNonNull(inviterId, "inviterId"));
    if (targetRef != null) {
      FriendsSoundService.play(targetRef, FriendsSoundService.SOUND_ACCEPT, dataStore.getLog());
    }
    if (inviterRef != null) {
      FriendsSoundService.play(inviterRef, FriendsSoundService.SOUND_ACCEPT, dataStore.getLog());
    }
  }

  private void playInviteDeclinedSounds(@Nonnull UUID targetId, @Nonnull UUID inviterId) {
    PlayerRef targetRef = Universe.get().getPlayer(Objects.requireNonNull(targetId, "targetId"));
    PlayerRef inviterRef = Universe.get().getPlayer(Objects.requireNonNull(inviterId, "inviterId"));
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

  private static int skipCommandTokens(String[] tokens, String command) {
    int index = 0;
    if (index < tokens.length && command.equalsIgnoreCase(tokens[index])) {
      index++;
    }
    return index;
  }
}
