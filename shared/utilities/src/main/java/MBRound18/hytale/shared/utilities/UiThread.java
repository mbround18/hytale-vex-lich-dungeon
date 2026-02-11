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

    World world = null;
    World refWorld = null;
    try {
      var ref = playerRef.getReference();
      if (ref != null && ref.isValid()) {
        var store = ref.getStore();
        if (store != null && store.getExternalData() != null) {
          refWorld = store.getExternalData().getWorld();
        }
      }
    } catch (Exception ignored) {
      // Fall through to UUID lookup
    }

    UUID worldUuid = playerRef.getWorldUuid();
    if (worldUuid != null) {
      // Universe.get() is thread-safe [3, 4]
      world = Universe.get().getWorld(worldUuid);
    }

    if (refWorld != null) {
      if (world != null && !world.equals(refWorld)) {
        log.warn("World mismatch for player %s. UUID=%s ref=%s; using ref world.",
            playerRef.getUsername(), world.getName(), refWorld.getName());
      }
      world = refWorld;
    }

    if (world == null) {
      try {
        PlayerRef refreshed = Universe.get().getPlayer(playerRef.getUuid());
        if (refreshed != null) {
          try {
            var ref = refreshed.getReference();
            if (ref != null && ref.isValid()) {
              var store = ref.getStore();
              if (store != null && store.getExternalData() != null) {
                world = store.getExternalData().getWorld();
              }
            }
          } catch (Exception ignored) {
            // Fall back to UUID lookup
          }

          if (world == null) {
            UUID refreshedWorld = refreshed.getWorldUuid();
            if (refreshedWorld != null) {
              world = Universe.get().getWorld(refreshedWorld);
            }
          }
        }
      } catch (Exception ignored) {
        // Fall through to warning
      }
    }

    if (world == null) {
      log.warn("World is null for playerRef: " + playerRef);
      return false;
    }

    // The Bridge: Schedule logic to run on the World's next tick [5, 6]
    // This prevents IllegalStateException when accessing ECS/UI.
    final World targetWorld = world;
    targetWorld.execute(() -> {
      try {
        action.run();
      } catch (Exception e) {
        String playerName = playerRef.getUsername();
        String worldName = targetWorld.getName();
        String actionName = action.getClass().getName();
        log.error("Error executing UI action for player %s in world %s (action=%s): %s",
            playerName, worldName, actionName, e.getMessage());
        e.printStackTrace();
      }
    });

    return true;
  }
}
