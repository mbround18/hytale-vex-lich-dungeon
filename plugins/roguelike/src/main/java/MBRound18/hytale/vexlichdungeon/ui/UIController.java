package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.ImmortalEngine.api.ui.UiRegistry;
import MBRound18.ImmortalEngine.api.ui.UiTemplate;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class UIController {

  private UIController() {
  }

  public static void registerDefaults() {
    UiRegistry.register(new UiTemplate(
        "summary",
      "UI/Custom/Vex/Pages/VexDungeonSummary.ui",
        java.util.List.of("SummaryStats", "SummaryBody")));
    UiRegistry.register(new UiTemplate(
        "scoreboard",
        "Custom/Vex/Pages/VexScoreboard.ui",
        java.util.List.of("ScoreBody")));
  }

  @Nullable
  public static UiTemplate getTemplate(@Nonnull String id) {
    return UiRegistry.getTemplate(id);
  }

  public static boolean openTemplate(@Nullable PlayerRef playerRef, @Nonnull UiTemplate template,
      @Nonnull Map<String, String> vars) {
    CustomUIPage page = new VexDebugUiPage(playerRef, template.getPath(), vars);
    return openPage(playerRef, page);
  }

  public static boolean openScoreboard(@Nullable PlayerRef playerRef, @Nullable String headerText,
      @Nullable String bodyText) {
    return openPage(playerRef, new VexScoreboardPage(playerRef, headerText, bodyText));
  }

  public static boolean openSummary(@Nullable PlayerRef playerRef, @Nullable String statsText,
      @Nullable String bodyText) {
    if (playerRef == null) {
      return false;
    }
    String stats = statsText == null ? "" : statsText;
    String body = bodyText == null ? "" : bodyText;
    return HudController.openHud(playerRef, "Custom/Vex/Hud/VexSummaryHud.ui",
        Map.of("VexSummaryStats", stats, "VexSummaryBody", body));
  }

  public static boolean openPage(@Nullable PlayerRef playerRef, @Nonnull CustomUIPage page) {
    return UiPageOpener.open(playerRef, page);
  }
}
