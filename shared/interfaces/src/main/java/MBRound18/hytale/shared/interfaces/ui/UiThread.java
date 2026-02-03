package MBRound18.hytale.shared.interfaces.ui;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import MBRound18.hytale.shared.utilities.LoggingHelper;

import java.util.UUID;
import javax.annotation.Nullable;

public final class UiThread {
  private static LoggingHelper log = new LoggingHelper("UiThread");

  private UiThread() {
  }

  public static boolean runOnPlayerWorld(@Nullable PlayerRef playerRef, @Nullable Runnable action) {
    if (playerRef == null || action == null || !playerRef.isValid()) {
      log.warn("Invalid playerRef or action: " + playerRef + ", " + action);
      return false;
    }
    UUID worldUuid = playerRef.getWorldUuid();
    if (worldUuid == null) {
      log.warn("World UUID is null for playerRef: " + playerRef);
      return false;
    }
    World world = Universe.get().getWorld(worldUuid);
    if (world == null) {
      log.warn("World is null for UUID: " + worldUuid);
      return false;
    }
    world.execute(action);
    return true;
  }
}
