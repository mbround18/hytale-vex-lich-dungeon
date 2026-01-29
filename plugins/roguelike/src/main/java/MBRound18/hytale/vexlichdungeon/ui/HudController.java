package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import MBRound18.ImmortalEngine.api.ui.EngineHud;
import MBRound18.ImmortalEngine.api.ui.HudRegistry;
import MBRound18.ImmortalEngine.api.ui.UiTemplate;
import MBRound18.ImmortalEngine.api.ui.UiThread;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HudController {
  private static final Logger LOGGER = Logger.getLogger(HudController.class.getName());
  private static final boolean DEBUG_HUD_COMMANDS = true;
  private static final long HUD_INITIAL_UPDATE_DELAY_MS = 1000L;
  private static final String PORTAL_COUNTDOWN_HUD_PATH = "Custom/Vex/Hud/VexPortalCountdownHud.ui";
  private static final boolean DISABLE_CUSTOM_HUD = false;
  private static final boolean DISABLE_HUD_UPDATES = false;
  private static final ScheduledExecutorService HUD_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "VexHudUpdate");
    thread.setDaemon(true);
    return thread;
  });

  private HudController() {
  }

  public static void registerDefaults() {
    HudRegistry.register(new UiTemplate(
        "hud",
      "Custom/Vex/Hud/VexScoreHud.ui",
      java.util.List.of("VexHudInstanceScore", "VexHudPlayerScore", "VexHudDelta", "VexHudPartyList")));
    HudRegistry.register(new UiTemplate(
      "welcome",
      "Custom/Vex/Hud/VexWelcomeHud.ui",
      java.util.List.of()));
    HudRegistry.register(new UiTemplate(
      "summaryHud",
      "Custom/Vex/Hud/VexSummaryHud.ui",
      java.util.List.of("VexSummaryStats", "VexSummaryBody")));
    HudRegistry.register(new UiTemplate(
      "leaderboardHud",
      "Custom/Vex/Hud/VexLeaderboardHud.ui",
      java.util.List.of("VexLeaderboardBody")));
    HudRegistry.register(new UiTemplate(
      "portalCountdown",
      PORTAL_COUNTDOWN_HUD_PATH,
      java.util.List.of("VexPortalCountdown", "VexPortalLocation")));
  }

  @Nullable
  public static UiTemplate getTemplate(@Nonnull String id) {
    return HudRegistry.getTemplate(id);
  }

  public static boolean openTemplate(@Nullable PlayerRef playerRef, @Nonnull UiTemplate template,
      @Nonnull Map<String, String> vars) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    if (!EngineHud.isCustomUiMode()) {
      EngineHud.show(playerRef, template.getPath(), vars);
      return true;
    }
    CustomUIHud hud = new VexDebugHudPage(playerRef, template.getPath(), vars);
    return applyHud(playerRef, hud);
  }

  public static boolean openHud(@Nullable PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    if (!EngineHud.isCustomUiMode()) {
      EngineHud.show(playerRef, uiPath, vars);
      return true;
    }
    CustomUIHud hud = new VexDebugHudPage(playerRef, uiPath, vars);
    return applyHud(playerRef, hud);
  }

  public static boolean openScoreHud(@Nullable PlayerRef playerRef, int instanceScore, int playerScore, int delta) {
    return openScoreHud(playerRef, instanceScore, playerScore, delta, "");
  }

  public static boolean openScoreHud(@Nullable PlayerRef playerRef, int instanceScore, int playerScore, int delta,
      @Nonnull String partyList) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    String deltaText = delta >= 0 ? "+" + delta : String.valueOf(delta);
    return openHud(playerRef, "Custom/Vex/Hud/VexScoreHud.ui",
        Map.of(
            "VexHudInstanceScore", EngineLang.t("customUI.vexHud.instanceScore", instanceScore),
            "VexHudPlayerScore", EngineLang.t("customUI.vexHud.playerScore", playerScore),
            "VexHudDelta", EngineLang.t("customUI.vexHud.delta", deltaText),
            "VexHudPartyList", partyList == null ? "" : partyList));
  }

  public static boolean openPortalCountdown(@Nullable PlayerRef playerRef, @Nonnull String timerText,
      @Nonnull String locationText) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    return openHud(playerRef, PORTAL_COUNTDOWN_HUD_PATH,
        Map.of(
            "VexPortalCountdown", timerText == null ? "" : timerText,
            "VexPortalLocation", locationText == null ? "" : locationText));
  }

  public static boolean clearHud(@Nullable PlayerRef playerRef) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    if (!EngineHud.isCustomUiMode()) {
      EngineHud.clear(playerRef);
      return true;
    }
    return resetHud(playerRef);
  }

  private static boolean applyHud(@Nonnull PlayerRef playerRef, @Nonnull CustomUIHud hud) {
    Ref<EntityStore> entityRef = playerRef.getReference();
    if (entityRef == null || !entityRef.isValid()) {
      return false;
    }
    Store<EntityStore> store = entityRef.getStore();
    if (store.isInThread()) {
      return applyHudOnThread(playerRef, entityRef, store, hud);
    }
    return UiThread.runOnPlayerWorld(playerRef, () -> applyHudOnThread(playerRef, entityRef, store, hud));
  }

  private static boolean resetHud(@Nonnull PlayerRef playerRef) {
    Ref<EntityStore> entityRef = playerRef.getReference();
    if (entityRef == null || !entityRef.isValid()) {
      return false;
    }
    Store<EntityStore> store = entityRef.getStore();
    if (store.isInThread()) {
      return resetHudOnThread(playerRef, entityRef, store);
    }
    return UiThread.runOnPlayerWorld(playerRef, () -> resetHudOnThread(playerRef, entityRef, store));
  }

  private static boolean applyHudOnThread(PlayerRef playerRef, Ref<EntityStore> entityRef,
      Store<EntityStore> store, CustomUIHud hud) {
    Player player = store.getComponent(entityRef, Player.getComponentType());
    if (player == null) {
      return false;
    }
    if (hud instanceof VexDebugHudPage vexHud) {
      if (tryUpdateExistingHud(playerRef, player, vexHud)) {
        return true;
      }
    }
    if (DISABLE_CUSTOM_HUD) {
      return true;
    }
    player.getHudManager().setCustomHud(playerRef, hud);
    if (!DISABLE_HUD_UPDATES && hud instanceof VexDebugHudPage vexHud) {
      scheduleInitialUpdate(playerRef, vexHud);
    }
    return true;
  }

  private static boolean tryUpdateExistingHud(@Nonnull PlayerRef playerRef, @Nonnull Player player,
      @Nonnull VexDebugHudPage nextHud) {
    CustomUIHud current = player.getHudManager().getCustomHud();
    if (!(current instanceof VexDebugHudPage currentHud)) {
      return false;
    }
    if (!currentHud.matchesPath(nextHud.getUiPath())) {
      return false;
    }
    if (DISABLE_HUD_UPDATES) {
      return true;
    }
    UICommandBuilder builder = new UICommandBuilder();
    nextHud.appendVarCommands(builder, nextHud.getVars());
    logHudCommands("Update", nextHud.getUiPath(), builder.getCommands());
    CustomUICommand[] commands = builder.getCommands();
    if (commands == null || commands.length == 0) {
      return true;
    }
    current.update(false, builder);
    return true;
  }

  private static boolean resetHudOnThread(PlayerRef playerRef, Ref<EntityStore> entityRef,
      Store<EntityStore> store) {
    Player player = store.getComponent(entityRef, Player.getComponentType());
    if (player == null) {
      return false;
    }
    player.getHudManager().resetHud(playerRef);
    return true;
  }

  private static void scheduleInitialUpdate(@Nonnull PlayerRef playerRef, @Nonnull VexDebugHudPage hud) {
    HUD_SCHEDULER.schedule(() -> UiThread.runOnPlayerWorld(playerRef, () -> {
      UICommandBuilder builder = new UICommandBuilder();
      hud.appendVarCommands(builder, hud.getVars());
      logHudCommands("InitialUpdate", hud.getUiPath(), builder.getCommands());
      CustomUICommand[] commands = builder.getCommands();
      if (commands == null || commands.length == 0) {
        return;
      }
      hud.update(false, builder);
    }), HUD_INITIAL_UPDATE_DELAY_MS, TimeUnit.MILLISECONDS);
  }

  private static void logHudCommands(String phase, String uiPath, CustomUICommand[] commands) {
    if (!DEBUG_HUD_COMMANDS || commands == null) {
      return;
    }
    for (CustomUICommand command : commands) {
      if (command == null) {
        continue;
      }
      LOGGER.info(String.format("[HUD] %s %s selector=%s data=%s text=%s",
          phase,
          uiPath,
          command.selector,
          command.data,
          command.text));
    }
  }
}
