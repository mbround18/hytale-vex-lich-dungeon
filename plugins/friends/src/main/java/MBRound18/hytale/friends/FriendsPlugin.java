package MBRound18.hytale.friends;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.ImmortalEngine.api.social.SocialServices;
import MBRound18.hytale.friends.commands.FriendCommand;
import MBRound18.hytale.friends.commands.FriendsCommand;
import MBRound18.hytale.friends.commands.PartyCommand;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.party.PartyHudUpdater;
import MBRound18.hytale.friends.services.FriendsServiceImpl;
import MBRound18.hytale.friends.services.PartyServiceImpl;
import MBRound18.hytale.friends.ui.FriendsAssetResolver;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

public class FriendsPlugin extends JavaPlugin {
  private final LoggingHelper log;
  private FriendsDataStore dataStore;
  private FriendsServiceImpl friendsService;
  private PartyServiceImpl partyService;
  private PartyHudUpdater hudUpdater;

  public FriendsPlugin(@Nonnull JavaPluginInit init) {
    super(init);
    this.log = Objects.requireNonNull(new LoggingHelper("Friends"), "log");
  }

  @Override
  protected void setup() {
    Path pluginJarPath = getFile().toAbsolutePath();
    Path modsDirectory = pluginJarPath.getParent();
    if (modsDirectory == null) {
      throw new IllegalStateException("Plugin jar has no parent directory - cannot initialize");
    }

    Path dataDirectory = modsDirectory.resolve("Friends");
    dataStore = new FriendsDataStore(
        Objects.requireNonNull(log, "log"),
        Objects.requireNonNull(dataDirectory, "dataDirectory"));
    dataStore.initialize();

    friendsService = new FriendsServiceImpl(Objects.requireNonNull(dataStore, "dataStore"));
    partyService = new PartyServiceImpl(
        Objects.requireNonNull(dataStore, "dataStore"),
        Objects.requireNonNull(log, "log"));
    SocialServices.registerFriends(friendsService);
    SocialServices.registerParty(partyService);

    CommandManager.get().register(new FriendsCommand(
        Objects.requireNonNull(dataStore, "dataStore"),
        Objects.requireNonNull(partyService, "partyService")));
    CommandManager.get().register(new FriendCommand(
        Objects.requireNonNull(dataStore, "dataStore"),
        Objects.requireNonNull(friendsService, "friendsService")));
    CommandManager.get().register(new PartyCommand(
        Objects.requireNonNull(dataStore, "dataStore"),
        Objects.requireNonNull(partyService, "partyService")));

    hudUpdater = new PartyHudUpdater(
        Objects.requireNonNull(partyService, "partyService"),
        Objects.requireNonNull(log, "log"));
    hudUpdater.start();

    log.info("[FRIENDS] Friends plugin initialized at %s", dataDirectory);
  }

  @Override
  protected void shutdown() {
    if (hudUpdater != null) {
      hudUpdater.stop();
    }
    if (dataStore != null) {
      dataStore.saveAll();
    }
    SocialServices.clear();
  }

  
}
