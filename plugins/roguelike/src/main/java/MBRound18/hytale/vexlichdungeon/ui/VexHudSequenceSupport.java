package MBRound18.hytale.vexlichdungeon.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class VexHudSequenceSupport {
  private VexHudSequenceSupport() {
  }

  public static void showWelcomeThenScore(@Nonnull PlayerRef playerRef, int instanceScore,
      int playerScore, int delta, @Nonnull String partyList) {
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return;
    }
    Store<EntityStore> store = ref.getStore();
    VexWelcomeHud.open(playerRef, "Prepare yourself. The portal opens soon.");
    VexScoreHud.open(store, ref, playerRef, instanceScore, playerScore, delta, partyList);
  }

  public static void showSummarySequence(@Nonnull PlayerRef playerRef, @Nonnull String statsLine,
      @Nonnull String summaryLine, @Nonnull String leaderboardText) {
    VexSummaryHud.open(playerRef, statsLine, summaryLine);
    VexLeaderboardHud.open(playerRef, leaderboardText);
  }
}
