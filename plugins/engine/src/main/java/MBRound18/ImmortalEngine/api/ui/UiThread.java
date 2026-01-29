package MBRound18.ImmortalEngine.api.ui;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nullable;

public final class UiThread {
  private UiThread() {
  }

  public static boolean runOnPlayerWorld(@Nullable PlayerRef playerRef, @Nullable Runnable action) {
    if (playerRef == null || action == null || !playerRef.isValid()) {
      return false;
    }
    World world = Universe.get().getWorld(playerRef.getWorldUuid());
    if (world == null) {
      return false;
    }
    world.execute(action);
    return true;
  }
}
