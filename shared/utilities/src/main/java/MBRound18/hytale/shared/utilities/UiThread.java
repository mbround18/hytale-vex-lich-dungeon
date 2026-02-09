package MBRound18.hytale.shared.utilities;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.UUID;
import javax.annotation.Nullable;

public final class UiThread {
  private static final LoggingHelper log = new LoggingHelper("UiThread");

  private UiThread() {
  }

  /**
   * Executes the specific action on the player's world thread.
   * 
   * @param playerRef The target player.
   * @param action    The logic to run (e.g. UI updates, ECS modifications).
   * @return true if the task was successfully scheduled.
   */
  public static boolean runOnPlayerWorld(@Nullable PlayerRef playerRef, @Nullable Runnable action) {
    if (playerRef == null || action == null || !playerRef.isValid()) {
      log.warn("Invalid playerRef or action.");
      return false;
    }

    UUID worldUuid = playerRef.getWorldUuid();
    if (worldUuid == null) {
      log.warn("World UUID is null for playerRef: " + playerRef);
      return false;
    }

    // Universe.get() is thread-safe [3, 4]
    World world = Universe.get().getWorld(worldUuid);
    if (world == null) {
      log.warn("World is null for UUID: " + worldUuid);
      return false;
    }

    // The Bridge: Schedule logic to run on the World's next tick [5, 6]
    // This prevents IllegalStateException when accessing ECS/UI.
    world.execute(() -> {
      try {
        action.run();
      } catch (Exception e) {
        String playerName = playerRef.getUsername();
        String worldName = world.getName();
        String actionName = action.getClass().getName();
        log.error("Error executing UI action for player %s in world %s (action=%s): %s",
            playerName, worldName, actionName, e.getMessage());
        e.printStackTrace();
      }
    });

    return true;
  }
}
