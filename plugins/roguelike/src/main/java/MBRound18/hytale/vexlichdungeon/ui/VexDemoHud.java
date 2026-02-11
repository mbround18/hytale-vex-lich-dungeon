package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexdemohudUi;
import MBRound18.hytale.shared.utilities.UiThread;

import MBRound18.hytale.shared.interfaces.util.UiMessage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class VexDemoHud extends AbstractCustomUIHud<VexHudVexdemohudUi> {
  public VexDemoHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexdemohudUi.class, playerRef);
  }

  public static VexDemoHud open(@Nonnull PlayerRef playerRef) {
    return ensureActive(playerRef, VexDemoHud.class);
  }

  public static void open(@Nonnull PlayerRef playerRef, @Nonnull String scoreText,
      @Nonnull String timerText, @Nonnull String debugStat) {
    update(playerRef, scoreText, timerText, debugStat);
  }

  public static void update(@Nonnull PlayerRef playerRef, @Nonnull String scoreText,
      @Nonnull String timerText, @Nonnull String debugStat) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      VexDemoHud hud = VexDemoHud.open(playerRef);
      if (hud == null) {
        return;
      }
      hud.setScore(playerRef, scoreText);
      hud.setTimer(playerRef, timerText);
      hud.setDebugStat(playerRef, debugStat);
    });
  }

  public void setScore(@Nonnull PlayerRef playerRef, @Nonnull String scoreText) {
    VexHudVexdemohudUi ui = getUiModel();
    String value = HudTextSanitizer.sanitize(scoreText);
    set(playerRef, ui.demoScore, UiMessage.raw(value));
  }

  public void setTimer(@Nonnull PlayerRef playerRef, @Nonnull String timerText) {
    VexHudVexdemohudUi ui = getUiModel();
    String value = HudTextSanitizer.sanitize(timerText);
    set(playerRef, ui.demoTimer, UiMessage.raw(value));
  }

  public void setDebugStat(@Nonnull PlayerRef playerRef, @Nonnull String debugStat) {
    VexHudVexdemohudUi ui = getUiModel();
    String value = HudTextSanitizer.sanitize(debugStat);
    set(playerRef, ui.vexDebugStat, UiMessage.raw(value));
  }
}
