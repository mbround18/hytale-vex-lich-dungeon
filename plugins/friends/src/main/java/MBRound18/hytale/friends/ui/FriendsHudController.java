package MBRound18.hytale.friends.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FriendsHudController {
  private FriendsHudController() {
  }

  public static boolean openPartyHud(@Nullable PlayerRef playerRef, @Nonnull String partyList) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    CustomUIHud hud = new FriendsHudPage(playerRef, "Custom/Friends/Hud/FriendsPartyHud.ui",
        java.util.Map.of("FriendsPartyList", partyList == null ? "" : partyList));
    return applyHud(playerRef, hud);
  }

  public static boolean clearHud(@Nullable PlayerRef playerRef) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    return resetHud(playerRef);
  }

  @SuppressWarnings("removal")
  private static boolean applyHud(@Nonnull PlayerRef playerRef, @Nonnull CustomUIHud hud) {
    com.hypixel.hytale.server.core.entity.entities.Player player = findPlayer(playerRef);
    if (player == null) {
      return false;
    }
    Ref<EntityStore> entityRef = player.getReference();
    if (entityRef == null) {
      return false;
    }
    player.getHudManager().setCustomHud(playerRef, hud);
    hud.show();
    return true;
  }

  private static boolean resetHud(@Nonnull PlayerRef playerRef) {
    com.hypixel.hytale.server.core.entity.entities.Player player = findPlayer(playerRef);
    if (player == null) {
      return false;
    }
    player.getHudManager().setCustomHud(playerRef, null);
    return true;
  }

  @Nullable
  private static com.hypixel.hytale.server.core.entity.entities.Player findPlayer(@Nonnull PlayerRef playerRef) {
    for (World world : Universe.get().getWorlds().values()) {
      for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
        if (playerRef.getUuid().equals(player.getUuid())) {
          return player;
        }
      }
    }
    return null;
  }
}
