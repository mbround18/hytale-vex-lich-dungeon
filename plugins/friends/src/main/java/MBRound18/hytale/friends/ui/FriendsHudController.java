package MBRound18.hytale.friends.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIController;
import MBRound18.hytale.shared.interfaces.ui.EngineHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FriendsHudController extends AbstractCustomUIController<FriendsHudPage> {
  private static final Logger LOGGER = Logger.getLogger(FriendsHudController.class.getName());
  private static final FriendsHudController INSTANCE = new FriendsHudController();
  private static final long HUD_INITIAL_UPDATE_DELAY_MS = 1000L;
  private static final String HUD_PATH = "Custom/Friends/Hud/FriendsPartyHud.ui";

  private FriendsHudController() {
    super(LOGGER, "FriendsHud");
  }

  public static boolean openPartyHud(@Nullable PlayerRef playerRef, @Nullable String partyList) {
    Map<String, String> vars = new java.util.LinkedHashMap<>();
    vars.put("FriendsPartyList", partyList == null ? "" : partyList);
    return INSTANCE.openPartyHudInternal(playerRef, vars);
  }

  public static boolean openPartyHud(@Nullable PlayerRef playerRef, @Nonnull Map<String, String> vars) {
    return INSTANCE.openPartyHudInternal(playerRef, vars);
  }

  public static boolean clearHud(@Nullable PlayerRef playerRef) {
    return INSTANCE.clearHudInternal(playerRef);
  }

  private boolean openPartyHudInternal(@Nullable PlayerRef playerRef, @Nonnull Map<String, String> vars) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    if (!EngineHud.isCustomUiMode()) {
      EngineHud.show(playerRef, HUD_PATH, vars);
      return true;
    }

    return openHud(playerRef,
        () -> new FriendsHudPage(playerRef, HUD_PATH, vars),
        hud -> scheduleInitialUpdate(playerRef, hud));
  }

  private boolean clearHudInternal(@Nullable PlayerRef playerRef) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    if (!EngineHud.isCustomUiMode()) {
      EngineHud.clear(playerRef);
      return true;
    }
    closeHud(playerRef, FriendsHudPage::clear);
    return true;
  }

  private void scheduleInitialUpdate(@Nonnull PlayerRef playerRef, @Nonnull FriendsHudPage hud) {
    cancelTimer(playerRef);
    scheduleOnce(playerRef, HUD_INITIAL_UPDATE_DELAY_MS, TimeUnit.MILLISECONDS,
        () -> enqueue(playerRef, () -> runOnWorld(playerRef, () -> {
          UICommandBuilder builder = new UICommandBuilder();
          hud.appendVarCommands(builder, hud.getVars());
          hud.update(false, builder);
        })));
  }
}
