package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.vexlichdungeon.ui.core.AbstractCustomUIController;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VexDemoHudController extends AbstractCustomUIController<VexDemoHud> {
  private static final Logger LOGGER = Logger.getLogger(VexDemoHudController.class.getName());
  private static final VexDemoHudController INSTANCE = new VexDemoHudController();
  private static final ConcurrentHashMap<UUID, Subscription> SUBSCRIPTIONS = new ConcurrentHashMap<>();

  private VexDemoHudController() {
    super(LOGGER, "VexDemoHud-Ticker");
  }

  /*
   * =============================================================================
   * =====
   * PUBLIC API
   * =============================================================================
   * =====
   */

  public static boolean openDemo(@Nullable PlayerRef playerRef, int score, int seconds) {
    return INSTANCE.openDemoInternal(playerRef, score, seconds);
  }

  public static void close(@Nullable PlayerRef playerRef) {
    INSTANCE.closeInternal(playerRef);
  }

  private boolean openDemoInternal(@Nullable PlayerRef playerRef, int score, int seconds) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }

    UUID uuid = playerRef.getUuid();
    logInfoPrefixed("openDemo request uuid=%s", uuid);

    return openHud(playerRef,
        () -> new VexDemoHud(playerRef, score, seconds),
        hud -> {
          logInfoPrefixed("openDemo HUD armed and ready uuid=%s", uuid);
          startCountdown(playerRef, hud);
        });
  }

  private void closeInternal(@Nullable PlayerRef playerRef) {
    cancelSubscription(playerRef);
    closeHud(playerRef, VexDemoHud::clear);
  }

  /*
   * =============================================================================
   * =====
   * INTERNAL LOGIC
   * =============================================================================
   * =====
   */

  private void startCountdown(@Nonnull PlayerRef playerRef, @Nonnull VexDemoHud hud) {
    UUID uuid = playerRef.getUuid();
    cancelSubscription(playerRef);
    Subscription subscription = subscribePerSecond(playerRef,
        currentHud -> {
          int remaining = currentHud.getTimeRemaining();
          if (remaining <= 1) {
            logInfoPrefixed("tick remaining<=1 uuid=%s -> close", uuid);
            closeInternal(playerRef);
            return;
          }
          currentHud.updateTimeRemaining(remaining - 1);
        },
        () -> closeInternal(playerRef));
    SUBSCRIPTIONS.put(uuid, subscription);
  }

  /*
   * =============================================================================
   * =====
   * HELPERS
   * =============================================================================
   * =====
   */

  private void logInfoPrefixed(String format, Object... args) {
    logInfo("[VexDemoHud] " + format, args);
  }

  private void cancelSubscription(@Nullable PlayerRef playerRef) {
    if (playerRef == null) {
      return;
    }
    Subscription subscription = SUBSCRIPTIONS.remove(playerRef.getUuid());
    if (subscription != null) {
      subscription.cancel();
    }
  }
}