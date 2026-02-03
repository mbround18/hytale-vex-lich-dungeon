package MBRound18.hytale.friends.commands;

import MBRound18.ImmortalEngine.api.social.PartyService;
import MBRound18.hytale.shared.interfaces.ui.PlayerSubscriptionController;
import MBRound18.hytale.shared.utilities.UiThread;
import MBRound18.hytale.friends.data.FriendRecord;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.ui.FriendsUiController;
import MBRound18.hytale.friends.ui.FriendsUiOpener;
import MBRound18.hytale.friends.ui.FriendsHudController;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FriendsCommand extends AbstractCommand {
  private static final String PERMISSION_USE = "friends.use";
  private static final Logger LOGGER = Logger.getLogger(FriendsCommand.class.getName());
  private static final PlayerSubscriptionController TEST_SUBSCRIPTIONS = new PlayerSubscriptionController(
      Objects.requireNonNull(LOGGER, "LOGGER"), "FriendsUiTest");
  private static final ConcurrentHashMap<UUID, PlayerSubscriptionController.Subscription> ACTIVE_TESTS = new ConcurrentHashMap<>();
  private final FriendsDataStore dataStore;
  private final PartyService partyService;

  public FriendsCommand(@Nonnull FriendsDataStore dataStore, @Nonnull PartyService partyService) {
    super("friends", "Open your friends list");
    this.dataStore = dataStore;
    this.partyService = partyService;
    addSubCommand(new FriendsUiCommand());
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    if (!checkPermission(context, PERMISSION_USE)) {
      return CompletableFuture.completedFuture(null);
    }
    @Nonnull
    String[] tokens = tokenize(context.getInputString());
    int index = skipCommandTokens(tokens, "friends");
    if (index < tokens.length) {
      context.sendMessage(Message.raw("Usage: /friends [ui test]"));
      return CompletableFuture.completedFuture(null);
    }
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("This command can only be used by players."));
      return CompletableFuture.completedFuture(null);
    }

    UUID playerId = context.sender().getUuid();
    if (playerId == null) {
      context.sendMessage(Message.raw("Unable to open friends list."));
      return CompletableFuture.completedFuture(null);
    }
    PlayerRef playerRef = Universe.get().getPlayer(playerId);
    if (playerRef == null) {
      context.sendMessage(Message.raw("Unable to open friends list."));
      return CompletableFuture.completedFuture(null);
    }

    String partyStatus = Objects.requireNonNull(buildPartyStatus(playerId), "partyStatus");
    String friendList = Objects.requireNonNull(buildFriendList(playerId), "friendList");
    UUID worldUuid = playerRef.getWorldUuid();
    if (worldUuid == null) {
      context.sendMessage(Message.raw("Failed to open friends UI."));
      return CompletableFuture.completedFuture(null);
    }
    World world = Universe.get().getWorld(worldUuid);
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

  private boolean checkPermission(@Nonnull CommandContext context, @Nonnull String permission) {
    if (context.sender().hasPermission(permission)) {
      return true;
    }
    context.sendMessage(Message.raw("Missing permission: " + permission));
    return false;
  }

  private final class FriendsUiCommand extends AbstractCommand {
    private FriendsUiCommand() {
      super("ui", "Friends UI utilities");
      addSubCommand(new FriendsUiTestCommand());
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!checkPermission(context, PERMISSION_USE)) {
        return CompletableFuture.completedFuture(null);
      }
      context.sendMessage(Message.raw("Usage: /friends ui test [seconds]"));
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class FriendsUiTestCommand extends AbstractCommand {
    private FriendsUiTestCommand() {
      super("test", "Cycle friends UI in a test loop");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!checkPermission(context, PERMISSION_USE)) {
        return CompletableFuture.completedFuture(null);
      }
      if (!context.isPlayer()) {
        context.sendMessage(Message.raw("This command can only be used by players."));
        return CompletableFuture.completedFuture(null);
      }
      UUID senderId = context.sender().getUuid();
      if (senderId == null) {
        context.sendMessage(Message.raw("Unable to run UI test."));
        return CompletableFuture.completedFuture(null);
      }
      PlayerRef playerRef = Universe.get().getPlayer(senderId);
      if (playerRef == null || !playerRef.isValid()) {
        context.sendMessage(Message.raw("Unable to run UI test."));
        return CompletableFuture.completedFuture(null);
      }
      @Nonnull
      String[] tokens = tokenize(context.getInputString());
      int index = skipCommandTokens(tokens, "friends", "ui", "test");
      int delaySeconds = parseDelaySeconds(tokens, index);
      startUiTest(context, playerRef, delaySeconds);
      return CompletableFuture.completedFuture(null);
    }
  }

  private void startUiTest(@Nonnull CommandContext context, @Nonnull PlayerRef playerRef, int delaySeconds) {
    cancelActiveTest(playerRef);
    List<TestEntry> entries = List.of(
        TestEntry.ui("friends-list"),
        TestEntry.hud("party-hud"));
    context.sendMessage(Message.raw("Starting Friends UI test (" + entries.size() + " entries)."));
    AtomicInteger index = new AtomicInteger(0);
    PlayerSubscriptionController.Subscription subscription = TEST_SUBSCRIPTIONS.subscribeAtFixedRate(
        playerRef, 0, delaySeconds, TimeUnit.SECONDS, () -> {
          if (!playerRef.isValid()) {
            cancelActiveTest(playerRef);
            return;
          }
          int current = index.getAndIncrement();
          if (current >= entries.size()) {
            FriendsHudController.clearHud(playerRef);
            FriendsUiOpener.close(playerRef);
            cancelActiveTest(playerRef);
            return;
          }
          TestEntry entry = entries.get(current);
          context.sendMessage(Message.raw("Friends UI test: " + entry.label));
          if (entry.isHud) {
            FriendsUiOpener.close(playerRef);
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("FriendsPartyList", "Party test: Alice, Bob, Chen");
            vars.put("Member1Name", "Alice");
            vars.put("Member1Leader", "LEAD");
            vars.put("Member1HpBar", "HP [########--]");
            vars.put("Member1StamBar", "ST [######----]");
            vars.put("Member1Item", "Bow");
            vars.put("Member2Name", "Bob");
            vars.put("Member2Leader", "");
            vars.put("Member2HpBar", "HP [#####-----]");
            vars.put("Member2StamBar", "ST [########--]");
            vars.put("Member2Item", "Axe");
            vars.put("Member3Name", "Chen");
            vars.put("Member3Leader", "");
            vars.put("Member3HpBar", "HP [##########]");
            vars.put("Member3StamBar", "ST [####------]");
            vars.put("Member3Item", "Staff");
            vars.put("Member4Name", "");
            vars.put("Member4Leader", "");
            vars.put("Member4HpBar", "HP [----------]");
            vars.put("Member4StamBar", "ST [----------]");
            vars.put("Member4Item", "");
            FriendsHudController.openPartyHud(playerRef, vars);
          } else {
            FriendsHudController.clearHud(playerRef);
            String partyStatus = "Party: 3 member(s)";
            String friendList = "Alice (online)\nBob (offline)\nChen (online)";
            UiThread.runOnPlayerWorld(playerRef,
                () -> FriendsUiController.openFriendsList(playerRef, partyStatus, friendList));
          }
        });
    ACTIVE_TESTS.put(playerRef.getUuid(), subscription);
  }

  private static void cancelActiveTest(@Nonnull PlayerRef playerRef) {
    PlayerSubscriptionController.Subscription existing = ACTIVE_TESTS.remove(playerRef.getUuid());
    if (existing != null) {
      existing.cancel();
    }
  }

  private static int parseDelaySeconds(@Nonnull String[] tokens, int index) {
    if (index >= tokens.length) {
      return 3;
    }
    try {
      int value = Integer.parseInt(tokens[index]);
      if (value < 1) {
        return 1;
      }
      if (value > 30) {
        return 30;
      }
      return value;
    } catch (NumberFormatException e) {
      return 3;
    }
  }

  @Nonnull
  private static String[] tokenize(@Nullable String input) {
    String trimmed = input == null ? "" : input.trim();
    if (trimmed.isEmpty()) {
      return new String[0];
    }
    return Objects.requireNonNull(trimmed.split("\\s+"), "tokens");
  }

  private static int skipCommandTokens(@Nonnull String[] tokens, @Nonnull String... expected) {
    int index = 0;
    for (String token : expected) {
      if (index < tokens.length && tokenEquals(Objects.requireNonNull(token, "token"), tokens[index])) {
        index++;
      }
    }
    return index;
  }

  @Nullable
  private static String normalizeToken(@Nullable String token) {
    if (token == null) {
      return null;
    }
    String trimmed = token.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.startsWith("/")) {
      return trimmed.substring(1);
    }
    return trimmed;
  }

  private static boolean tokenEquals(@Nonnull String expected, @Nullable String token) {
    String normalized = normalizeToken(token);
    return normalized != null && expected.equalsIgnoreCase(normalized);
  }

  private static final class TestEntry {
    private final String label;
    private final boolean isHud;

    private TestEntry(String label, boolean isHud) {
      this.label = label;
      this.isHud = isHud;
    }

    private static TestEntry ui(String label) {
      return new TestEntry(label, false);
    }

    private static TestEntry hud(String label) {
      return new TestEntry(label, true);
    }
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
