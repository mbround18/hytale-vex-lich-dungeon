package MBRound18.hytale.friends.ui;

import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FriendsUiController {
  private FriendsUiController() {
  }

  public static boolean openFriendsList(@Nullable PlayerRef playerRef, @Nonnull String partyStatus,
      @Nonnull String listBody) {
    if (playerRef == null || !playerRef.isValid()) {
      return false;
    }
    CustomUIPage page = new FriendsUiPage(playerRef, "Custom/Friends/Pages/FriendsList.ui",
        Map.of(
            "FriendsPartyStatus", partyStatus == null ? "" : partyStatus,
            "FriendsListBody", listBody == null ? "" : listBody));
    return FriendsUiOpener.open(playerRef, page);
  }
}
