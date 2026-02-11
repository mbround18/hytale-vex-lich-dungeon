package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexportalcountdownhudMinUi;
import MBRound18.hytale.shared.utilities.UiThread;

import MBRound18.hytale.shared.interfaces.util.UiMessage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class VexPortalCountdownHudMin extends AbstractCustomUIHud<VexHudVexportalcountdownhudMinUi> {
  public VexPortalCountdownHudMin(@Nonnull PlayerRef playerRef) {
    super(VexHudVexportalcountdownhudMinUi.class, playerRef);
  }

  public static VexPortalCountdownHudMin open(@Nonnull PlayerRef playerRef) {
    return ensureActive(playerRef, VexPortalCountdownHudMin.class);
  }

  public static void open(@Nonnull PlayerRef playerRef, @Nonnull String countdown,
      @Nonnull String locationText) {
    update(playerRef, countdown, locationText);
  }

  public static void update(@Nonnull PlayerRef playerRef, @Nonnull String countdown,
      @Nonnull String locationText) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      VexPortalCountdownHudMin hud = VexPortalCountdownHudMin.open(playerRef);
      if (hud == null) {
        return;
      }
      hud.setTimeLeft(playerRef, countdown);
      hud.setLocation(playerRef, locationText);
    });
  }

  public void setLocation(@Nonnull PlayerRef playerRef, @Nonnull String locationText) {
    VexHudVexportalcountdownhudMinUi ui = getUiModel();
    if (ui == null) {
      return;
    }
    String value = HudTextSanitizer.sanitize(locationText);
    set(playerRef, ui.vexPortalLocation, UiMessage.raw(value));
  }

  public void setTimeLeft(@Nonnull PlayerRef playerRef, @Nonnull String timeLeft) {
    VexHudVexportalcountdownhudMinUi ui = getUiModel();
    if (ui == null) {
      return;
    }
    String value = HudTextSanitizer.sanitize(timeLeft);
    set(playerRef, ui.vexPortalCountdown, UiMessage.raw(value));
  }
}
