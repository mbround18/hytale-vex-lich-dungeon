package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexsummaryhudUi;
import MBRound18.hytale.shared.utilities.UiThread;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class VexSummaryHud extends AbstractCustomUIHud<VexHudVexsummaryhudUi> {
  public VexSummaryHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexsummaryhudUi.class, playerRef);
  }

  public static VexSummaryHud open(@Nonnull PlayerRef playerRef) {
    return ensureActive(playerRef, VexSummaryHud.class);
  }

  public static void open(@Nonnull PlayerRef playerRef, @Nonnull String statsLine,
      @Nonnull String summaryLine) {
    update(playerRef, statsLine, summaryLine);
  }

  public static void update(@Nonnull PlayerRef playerRef, @Nonnull String statsLine,
      @Nonnull String summaryLine) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      VexSummaryHud hud = VexSummaryHud.open(playerRef);
      if (hud == null) {
        return;
      }
      hud.setStatsLine(playerRef, statsLine);
      hud.setSummaryLine(playerRef, summaryLine);
    });
  }

  public void setStatsLine(@Nonnull PlayerRef playerRef, @Nonnull String statsLine) {
    VexHudVexsummaryhudUi ui = getUiModel();
    String value = statsLine != null ? statsLine : "---";
    if (value.isBlank()) {
      value = "---";
    }
    set(playerRef, ui.vexContentVexSummaryStats, Message.raw(value));
  }

  public void setSummaryLine(@Nonnull PlayerRef playerRef, @Nonnull String summaryLine) {
    VexHudVexsummaryhudUi ui = getUiModel();
    String value = summaryLine != null ? summaryLine : "---";
    if (value.isBlank()) {
      value = "---";
    }
    set(playerRef, ui.vexContentVexSummaryBody, Message.raw(value));
  }
}
