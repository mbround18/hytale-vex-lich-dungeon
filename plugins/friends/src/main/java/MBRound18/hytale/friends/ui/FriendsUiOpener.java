package MBRound18.hytale.friends.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nullable;

@SuppressWarnings("removal")
public final class FriendsUiOpener {
  private FriendsUiOpener() {
  }

  public static boolean open(@Nullable PlayerRef playerRef, CustomUIPage page) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    UUID uuid = playerRef.getUuid();
    for (World world : Universe.get().getWorlds().values()) {
      for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
        if (!uuid.equals(player.getUuid())) {
          continue;
        }
        Ref<EntityStore> entityRef = player.getReference();
        if (entityRef == null) {
          return false;
        }
        player.getPageManager().openCustomPage(entityRef, entityRef.getStore(), page);
        return true;
      }
    }
    return false;
  }
}
