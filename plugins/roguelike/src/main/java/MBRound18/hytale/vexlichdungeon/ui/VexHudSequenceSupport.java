package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import MBRound18.ImmortalEngine.api.ui.HudSequenceController;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Vex-specific wrapper for the generic HUD sequencer.
 */
public final class VexHudSequenceSupport {
  private static final long WELCOME_MS = 5_000L;
  private static final long SUMMARY_MS = 5_000L;
  private static final long LEADERBOARD_MS = 5_000L;

  private static final String WELCOME_UI = "Custom/Vex/Hud/VexWelcomeHud.ui";
  private static final String SUMMARY_UI = "Custom/Vex/Hud/VexSummaryHud.ui";
  private static final String LEADERBOARD_UI = "Custom/Vex/Hud/VexLeaderboardHud.ui";
  private static final String SCORE_UI = "Custom/Vex/Hud/VexScoreHud.ui";

  private static final HudSequenceController.HudPresenter PRESENTER = new HudSequenceController.HudPresenter() {
    @Override
    public void show(@Nonnull PlayerRef playerRef, @Nonnull String uiPath, @Nonnull Map<String, String> vars) {
      HudController.openHud(playerRef, uiPath, vars);
    }

    @Override
    public void clear(@Nonnull PlayerRef playerRef) {
      HudController.clearHud(playerRef);
    }
  };

  private static final HudSequenceController.DismissFactory DISMISS_FACTORY = playerRef -> {
    VexDismissPage page = new VexDismissPage(playerRef, () -> HudSequenceController.dismiss(playerRef, PRESENTER));
    UiPageOpener.open(playerRef, page);
    return page::requestClose;
  };

  private VexHudSequenceSupport() {
  }

  public static void showWelcomeThenScore(@Nullable PlayerRef playerRef, int instanceScore,
      int playerScore, int delta, @Nonnull String partyList) {
    HudSequenceController.showWelcomeThenHud(
        playerRef,
        WELCOME_UI,
        Map.of(),
        WELCOME_MS,
        SCORE_UI,
        Map.of(
            "VexHudInstanceScore", EngineLang.t("customUI.vexHud.instanceScore", instanceScore),
            "VexHudPlayerScore", EngineLang.t("customUI.vexHud.playerScore", playerScore),
            "VexHudDelta", EngineLang.t("customUI.vexHud.delta",
                delta >= 0 ? "+" + delta : String.valueOf(delta)),
            "VexHudPartyList", partyList == null ? "" : partyList),
        PRESENTER,
        DISMISS_FACTORY,
        true);
  }

  public static void showSummarySequence(@Nullable PlayerRef playerRef, @Nonnull String statsText,
      @Nonnull String bodyText, @Nonnull String leaderboardText) {
    HudSequenceController.showSequence(
        playerRef,
        SUMMARY_UI,
        Map.of("VexSummaryStats", statsText, "VexSummaryBody", bodyText),
        SUMMARY_MS,
        LEADERBOARD_UI,
        Map.of("VexLeaderboardBody", leaderboardText),
        LEADERBOARD_MS,
        PRESENTER,
        DISMISS_FACTORY);
  }
}
