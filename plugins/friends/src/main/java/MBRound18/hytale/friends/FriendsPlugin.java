package MBRound18.hytale.friends;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import MBRound18.ImmortalEngine.api.logging.LoggingController;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

public class FriendsPlugin extends JavaPlugin {
  private final EngineLog log;
  private FriendsDataStore dataStore;
  private FriendsServiceImpl friendsService;
  private PartyServiceImpl partyService;
  private PartyHudUpdater hudUpdater;

  public FriendsPlugin(@Nonnull JavaPluginInit init) {
    super(init);
    this.log = LoggingController.forPlugin(this, "Friends");
  }

  @Override
  protected void setup() {
    Path pluginJarPath = getFile().toAbsolutePath();
    Path modsDirectory = pluginJarPath.getParent();
    if (modsDirectory == null) {
      throw new IllegalStateException("Plugin jar has no parent directory - cannot initialize");
    }

    Path dataDirectory = modsDirectory.resolve("Friends");
    dataStore = new FriendsDataStore(log, dataDirectory);
    dataStore.initialize();

    Path assetsZipPath = resolveAssetsZipPath(pluginJarPath, modsDirectory);
    FriendsAssetResolver.setAssetsZipPath(assetsZipPath);

    friendsService = new FriendsServiceImpl(dataStore);
    partyService = new PartyServiceImpl(dataStore, log);
    SocialServices.registerFriends(friendsService);
    SocialServices.registerParty(partyService);

    CommandManager.get().register(new FriendsCommand(dataStore, partyService));
    CommandManager.get().register(new FriendCommand(dataStore, friendsService));
    CommandManager.get().register(new PartyCommand(dataStore, partyService));

    hudUpdater = new PartyHudUpdater(partyService, log);
    hudUpdater.start();

    preflightUiAssets(assetsZipPath);

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

  private Path resolveAssetsZipPath(@Nonnull Path pluginJarPath, @Nonnull Path modsDirectory) {
    String jarName = pluginJarPath.getFileName().toString();
    String baseFull = jarName.endsWith(".jar")
        ? jarName.substring(0, jarName.length() - 4)
        : jarName;
    String versionedZip = baseFull + ".zip";
    Path zipPath = modsDirectory.resolve(versionedZip);
    if (Files.exists(zipPath)) {
      return zipPath;
    }

    String legacyBase = baseFull.split("-")[0];
    String legacyZip = legacyBase + ".zip";
    Path legacyPath = modsDirectory.resolve(legacyZip);
    if (Files.exists(legacyPath)) {
      return legacyPath;
    }

    Path fallback = findZipFallback(modsDirectory, versionedZip, legacyZip, baseFull, legacyBase);
    if (fallback != null) {
      return fallback;
    }

    return zipPath;
  }

  private void preflightUiAssets(@Nonnull Path assetsZipPath) {
    if (!Files.exists(assetsZipPath)) {
      log.error("UI preflight failed: assets zip not found at %s", assetsZipPath);
      return;
    }

    List<String> required = List.of(
        "Custom/Friends/Pages/FriendsList.ui",
        "Custom/Friends/Hud/FriendsPartyHud.ui",
        "Custom/Friends/FriendsCommon.ui");

    List<String> missing = new ArrayList<>();
    try (ZipFile zipFile = new ZipFile(assetsZipPath.toFile())) {
      for (String path : required) {
        ZipEntry entry = zipFile.getEntry(path);
        if (entry == null) {
          missing.add(path);
        }
      }
    } catch (Exception e) {
      log.error("UI preflight failed reading %s: %s", assetsZipPath, e.getMessage());
      return;
    }

    if (missing.isEmpty()) {
      log.lifecycle().atInfo().log("UI preflight OK: Friends UI documents found in %s",
          assetsZipPath.getFileName());
      return;
    }

    log.error("UI preflight failed: missing %d Friends UI document(s) in %s",
        missing.size(), assetsZipPath.getFileName());
    for (String path : missing) {
      log.error("- Missing UI: %s", path);
    }
  }

  private Path findZipFallback(@Nonnull Path modsDirectory, @Nonnull String versionedZip,
      @Nonnull String legacyZip, @Nonnull String baseFull, @Nonnull String legacyBase) {
    String versionedLower = versionedZip.toLowerCase();
    String legacyLower = legacyZip.toLowerCase();
    String baseLower = baseFull.toLowerCase();
    String legacyBaseLower = legacyBase.toLowerCase();
    try (java.util.stream.Stream<Path> stream = Files.list(modsDirectory)) {
      return stream
          .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zip"))
          .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
          .filter(path -> {
            String name = path.getFileName().toString().toLowerCase();
            return name.equals(versionedLower)
                || name.equals(legacyLower)
                || name.startsWith(baseLower)
                || name.startsWith(legacyBaseLower);
          })
          .findFirst()
          .orElse(null);
    } catch (Exception ignored) {
      return null;
    }
  }
}
