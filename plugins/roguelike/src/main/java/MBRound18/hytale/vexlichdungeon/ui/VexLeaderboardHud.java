package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexleaderboardhudUi;
import MBRound18.hytale.shared.utilities.UiThread;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class VexLeaderboardHud extends AbstractCustomUIHud<VexHudVexleaderboardhudUi> {
  public VexLeaderboardHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexleaderboardhudUi.class, playerRef);
  }

  public static VexLeaderboardHud open(@Nonnull PlayerRef playerRef) {
    return ensureActive(playerRef, VexLeaderboardHud.class);
  }

  public static void open(@Nonnull PlayerRef playerRef, @Nonnull String leaderboardText) {
    update(playerRef, leaderboardText);
  }

  public static void update(@Nonnull PlayerRef playerRef, @Nonnull String leaderboardText) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      VexLeaderboardHud hud = VexLeaderboardHud.open(playerRef);
      if (hud == null) {
        return;
      }
      hud.setLeaderboardText(playerRef, leaderboardText);
    });
  }

  public void setLeaderboardText(@Nonnull PlayerRef playerRef, @Nonnull String leaderboardText) {
    VexHudVexleaderboardhudUi ui = getUiModel();
    String value = leaderboardText != null ? leaderboardText : "---";
    if (value.isBlank()) {
      value = "---";
    }
    set(playerRef, ui.vexContentVexLeaderboardBody, Message.raw(value));
  }
}
