package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.vexlichdungeon.events.VexLeaderboardHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexScoreHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexSummaryHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexWelcomeHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.WorldEventQueue;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VexHudSequenceSupport {
  private VexHudSequenceSupport() {
  }

  public static void showWelcomeThenScore(@Nonnull PlayerRef playerRef, int instanceScore,
      int playerScore, int delta, @Nonnull String partyList) {
    VexPortalCountdownHud.closeFor(playerRef);
    VexWelcomeHud.closeFor(playerRef);
    VexScoreHud.closeFor(playerRef);
    dispatch(null, new VexWelcomeHudRequestedEvent(playerRef, "Prepare yourself. The portal opens soon."));
    dispatch(null, new VexScoreHudRequestedEvent(playerRef, instanceScore, playerScore, delta, partyList));
  }

  public static void showWelcomeThenScore(@Nonnull World world, @Nonnull PlayerRef playerRef, int instanceScore,
      int playerScore, int delta, @Nonnull String partyList) {
    VexPortalCountdownHud.closeFor(playerRef);
    VexWelcomeHud.closeFor(playerRef);
    VexScoreHud.closeFor(playerRef);
    dispatch(world, new VexWelcomeHudRequestedEvent(playerRef, "Prepare yourself. The portal opens soon."));
    dispatch(world, new VexScoreHudRequestedEvent(playerRef, instanceScore, playerScore, delta, partyList));
  }

  public static void showSummarySequence(@Nonnull PlayerRef playerRef, @Nonnull String statsLine,
      @Nonnull String summaryLine, @Nonnull String leaderboardText) {
    dispatch(null, new VexSummaryHudRequestedEvent(playerRef, statsLine, summaryLine));
    dispatch(null, new VexLeaderboardHudRequestedEvent(playerRef, leaderboardText));
  }

  public static void showSummarySequence(@Nonnull World world, @Nonnull PlayerRef playerRef,
      @Nonnull String statsLine, @Nonnull String summaryLine, @Nonnull String leaderboardText) {
    dispatch(world, new VexSummaryHudRequestedEvent(playerRef, statsLine, summaryLine));
    dispatch(world, new VexLeaderboardHudRequestedEvent(playerRef, leaderboardText));
  }

  private static void dispatch(@Nullable World world, @Nonnull MBRound18.ImmortalEngine.api.events.DebugEvent event) {
    WorldEventQueue.get().dispatch(world, event);
  }
}
