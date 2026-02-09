package MBRound18.hytale.dungeonmaster.helpers;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public final class PlayerWorldExecutor {
  private PlayerWorldExecutor() {
  }

  public static CompletableFuture<Boolean> runOnPlayerWorld(@Nullable PlayerRef playerRef,
      @Nullable World world, @Nullable Runnable action) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (playerRef == null || action == null || world == null || !playerRef.isValid()) {
      future.complete(false);
      return future;
    }

    world.execute(() -> {
      try {
        action.run();
        future.complete(true);
      } catch (Exception e) {
        future.complete(false);
      }
    });

    return future;
  }
}
