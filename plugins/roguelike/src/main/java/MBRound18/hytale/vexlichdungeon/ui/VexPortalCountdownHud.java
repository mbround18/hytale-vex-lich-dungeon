package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexportalcountdownhudUi;
import MBRound18.hytale.shared.utilities.UiThread;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class VexPortalCountdownHud extends AbstractCustomUIHud<VexHudVexportalcountdownhudUi> {
  public VexPortalCountdownHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexportalcountdownhudUi.class, playerRef);
  }

  public static VexPortalCountdownHud open(@Nonnull PlayerRef playerRef) {
    return ensureActive(
        playerRef,
        VexPortalCountdownHud.class);
  }

  // External how to clear the HUD
  public static void clear(@Nonnull PlayerRef playerRef) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      var hud = VexPortalCountdownHud.open(playerRef);
      if (hud == null)
        return;
      hud.clear();
    });
  }

  // External method to update the HUD
  public static void update(@Nonnull PlayerRef playerRef, @Nonnull String timeLeft,
      @Nonnull String locationText) {
    if (playerRef != null) {
      UiThread.runOnPlayerWorld(playerRef, () -> {
        VexPortalCountdownHud hud = VexPortalCountdownHud.open(playerRef);
        if (hud == null)
          return;
        hud.setTimeLeft(playerRef, timeLeft);
        hud.setLocation(playerRef, locationText);
      });
    }
  }

  // Internal method to set location text
  public void setLocation(@Nonnull PlayerRef playerRef, @Nonnull String locationText) {
    VexHudVexportalcountdownhudUi ui = getUiModel();
    set(playerRef, ui.vexPortalLocation, Message.raw(locationText != null ? locationText : "N/A"));
  }

  // Internal method to set time left text
  public void setTimeLeft(@Nonnull PlayerRef playerRef, @Nonnull String timeLeft) {
    VexHudVexportalcountdownhudUi ui = getUiModel();
    set(playerRef, ui.vexPortalCountdown, Message.raw(timeLeft != null ? timeLeft : "N/A"));
  }
}
