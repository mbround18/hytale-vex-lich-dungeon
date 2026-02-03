package MBRound18.hytale.friends.ui;

import MBRound18.hytale.shared.interfaces.ui.generated.PagesFriendslistpageUi;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.Objects;
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
    PagesFriendslistpageUi ui = new PagesFriendslistpageUi();
    CustomUIPage page = new FriendsUiPage(playerRef, PagesFriendslistpageUi.UI_PATH,
        Objects.requireNonNull(
            Map.of(
                ui.friendsListPagePanelHeaderTitle, partyStatus == null ? "" : partyStatus,
                ui.friendsListPagePanelListPanelFriendRowInfoStatus, listBody == null ? "" : listBody),
            "vars"));
    return FriendsUiOpener.open(playerRef, page);
  }
}
