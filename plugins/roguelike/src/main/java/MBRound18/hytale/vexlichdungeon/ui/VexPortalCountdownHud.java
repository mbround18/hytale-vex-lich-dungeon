package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexportalcountdownhudUi;
import MBRound18.hytale.shared.utilities.UiThread;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class VexPortalCountdownHud extends AbstractCustomUIHud<VexHudVexportalcountdownhudUi> {
  private volatile boolean cleared = false;

  public VexPortalCountdownHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexportalcountdownhudUi.class, playerRef);
  }

  public static VexPortalCountdownHud open(@Nonnull PlayerRef playerRef) {
    return ensureActive(
        playerRef,
        VexPortalCountdownHud.class);
  }

  private static VexPortalCountdownHud forceOpen(@Nonnull PlayerRef playerRef) {
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return null;
    }
    Store<EntityStore> store = ref.getStore();
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      return null;
    }
    HudManager hudManager = player.getHudManager();
    VexPortalCountdownHud hud = new VexPortalCountdownHud(playerRef);
    hudManager.setCustomHud(playerRef, hud);
    return hud;
  }

  public static void closeFor(@Nonnull PlayerRef playerRef) {
    if (playerRef != null) {
      UiThread.runOnPlayerWorld(playerRef, () -> {
        AbstractCustomUIHud.closeHud(playerRef, VexPortalCountdownHud.class, hud -> {
          if (hud instanceof VexPortalCountdownHud countdownHud) {
            countdownHud.cleared = true;
          }
        });
      });
    }
  }

  // External method to update the HUD
  public static void update(@Nonnull PlayerRef playerRef, @Nonnull String timeLeft,
      @Nonnull String locationText) {
    if (playerRef != null) {
      UiThread.runOnPlayerWorld(playerRef, () -> {
        VexPortalCountdownHud hud = VexPortalCountdownHud.open(playerRef);
        if (hud == null || !hud.isActiveHud(playerRef))
          return;
        if (hud.cleared) {
          hud = VexPortalCountdownHud.forceOpen(playerRef);
          if (hud == null) {
            return;
          }
        }
        hud.setTimeLeft(playerRef, timeLeft);
        hud.setLocation(playerRef, locationText);
      });
    }
  }

  @Override
  public void onClear() {
    cleared = true;
  }

  // Internal method to set location text
  public void setLocation(@Nonnull PlayerRef playerRef, @Nonnull String locationText) {
    VexHudVexportalcountdownhudUi ui = getUiModel();
    if (ui == null) {
      return;
    }
    locationText = locationText != null ? locationText : "---";
    if (locationText.isEmpty()) {
      locationText = "---";
    }
    set(playerRef, ui.vexPortalLocation, Message.raw(locationText));
  }

  // Internal method to set time left text
  public void setTimeLeft(@Nonnull PlayerRef playerRef, @Nonnull String timeLeft) {
    VexHudVexportalcountdownhudUi ui = getUiModel();
    if (ui == null) {
      return;
    }
    timeLeft = timeLeft != null ? timeLeft : "---";
    if (timeLeft.isEmpty()) {
      timeLeft = "---";
    }
    set(playerRef, ui.vexPortalCountdown, Message.raw(timeLeft));
  }
}
