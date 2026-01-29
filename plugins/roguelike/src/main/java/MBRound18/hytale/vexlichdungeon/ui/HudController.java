package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import MBRound18.ImmortalEngine.api.ui.HudRegistry;
import MBRound18.ImmortalEngine.api.ui.UiTemplate;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import java.util.UUID;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HudController {

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
      "Custom/Vex/Hud/VexPortalCountdownHud.ui",
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
    CustomUIHud hud = new VexDebugHudPage(playerRef, template.getPath(), vars);
    return applyHud(playerRef, hud);
  }

  public static boolean openHud(@Nullable PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
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
    return openHud(playerRef, "Custom/Vex/Hud/VexPortalCountdownHud.ui",
        Map.of(
            "VexPortalCountdown", timerText == null ? "" : timerText,
            "VexPortalLocation", locationText == null ? "" : locationText));
  }

  public static boolean clearHud(@Nullable PlayerRef playerRef) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    return resetHud(playerRef);
  }

  @SuppressWarnings("removal")
  private static boolean applyHud(@Nonnull PlayerRef playerRef, @Nonnull CustomUIHud hud) {
    com.hypixel.hytale.server.core.entity.entities.Player player = findPlayer(playerRef);
    if (player == null) {
      return false;
    }
    Ref<EntityStore> entityRef = player.getReference();
    if (entityRef == null) {
      return false;
    }
    player.getHudManager().setCustomHud(playerRef, hud);
    hud.show();
    return true;
  }

  private static boolean resetHud(@Nonnull PlayerRef playerRef) {
    com.hypixel.hytale.server.core.entity.entities.Player player = findPlayer(playerRef);
    if (player == null) {
      return false;
    }
    player.getHudManager().resetHud(playerRef);
    return true;
  }

  @SuppressWarnings("removal")
  private static com.hypixel.hytale.server.core.entity.entities.Player findPlayer(
      @Nonnull PlayerRef playerRef) {
    UUID uuid = playerRef.getUuid();
    for (World world : Universe.get().getWorlds().values()) {
      for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
        if (uuid.equals(player.getUuid())) {
          return player;
        }
      }
    }
    return null;
  }
}
