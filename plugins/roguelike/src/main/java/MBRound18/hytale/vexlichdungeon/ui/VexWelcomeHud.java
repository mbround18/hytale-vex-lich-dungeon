package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexwelcomehudUi;
import MBRound18.hytale.shared.utilities.UiThread;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class VexWelcomeHud extends AbstractCustomUIHud<VexHudVexwelcomehudUi> {
  public VexWelcomeHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexwelcomehudUi.class, playerRef);
  }

  public static VexWelcomeHud open(@Nonnull PlayerRef playerRef) {
    return ensureActive(playerRef, VexWelcomeHud.class);
  }

  public static void open(@Nonnull PlayerRef playerRef, @Nonnull String bodyText) {
    update(playerRef, bodyText);
  }

  public static void update(@Nonnull PlayerRef playerRef, @Nonnull String bodyText) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      VexWelcomeHud hud = VexWelcomeHud.open(playerRef);
      if (hud == null) {
        return;
      }
      hud.setBody(playerRef, bodyText);
    });
  }

  public static void closeFor(@Nonnull PlayerRef playerRef) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      AbstractCustomUIHud.closeHud(playerRef, VexWelcomeHud.class, hud -> {
        // Welcome HUD closed safely
      });
    });
  }

  public void setBody(@Nonnull PlayerRef playerRef, @Nonnull String bodyText) {
    VexHudVexwelcomehudUi ui = getUiModel();
    if (ui == null) {
      return;
    }
    String value = bodyText != null ? bodyText : "---";
    if (value.isBlank()) {
      value = "---";
    }
    set(playerRef, ui.vexContentVexWelcomeBody, Message.raw(value));
  }
}
