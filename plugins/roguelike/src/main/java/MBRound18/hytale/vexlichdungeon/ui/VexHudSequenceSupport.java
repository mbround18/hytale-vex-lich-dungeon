package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.vexlichdungeon.events.VexLeaderboardHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexScoreHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexSummaryHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexWelcomeHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.WorldEventQueue;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.HytaleServer;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VexHudSequenceSupport {
  private static final long HUD_SEQUENCE_DELAY_MS = 150L;

  private VexHudSequenceSupport() {
  }

  public static void showWelcomeThenScore(@Nonnull PlayerRef playerRef, int instanceScore,
      int playerScore, int delta, @Nonnull String partyList) {
    VexPortalCountdownHud.closeFor(playerRef);
    VexWelcomeHud.closeFor(playerRef);
    VexScoreHud.closeFor(playerRef);
    dispatch(null, new VexWelcomeHudRequestedEvent(playerRef, "Prepare yourself. The portal opens soon."));
    dispatchDelayed(null, new VexScoreHudRequestedEvent(playerRef, instanceScore, playerScore, delta, partyList),
        HUD_SEQUENCE_DELAY_MS);
  }

  public static void showWelcomeThenScore(@Nonnull World world, @Nonnull PlayerRef playerRef, int instanceScore,
      int playerScore, int delta, @Nonnull String partyList) {
    VexPortalCountdownHud.closeFor(playerRef);
    VexWelcomeHud.closeFor(playerRef);
    VexScoreHud.closeFor(playerRef);
    dispatch(world, new VexWelcomeHudRequestedEvent(playerRef, "Prepare yourself. The portal opens soon."));
    dispatchDelayed(world, new VexScoreHudRequestedEvent(playerRef, instanceScore, playerScore, delta, partyList),
        HUD_SEQUENCE_DELAY_MS);
  }

  public static void showSummarySequence(@Nonnull PlayerRef playerRef, @Nonnull String statsLine,
      @Nonnull String summaryLine, @Nonnull String leaderboardText) {
    dispatch(null, new VexSummaryHudRequestedEvent(playerRef, statsLine, summaryLine));
    dispatchDelayed(null, new VexLeaderboardHudRequestedEvent(playerRef, leaderboardText),
        HUD_SEQUENCE_DELAY_MS);
  }

  public static void showSummarySequence(@Nonnull World world, @Nonnull PlayerRef playerRef,
      @Nonnull String statsLine, @Nonnull String summaryLine, @Nonnull String leaderboardText) {
    dispatch(world, new VexSummaryHudRequestedEvent(playerRef, statsLine, summaryLine));
    dispatchDelayed(world, new VexLeaderboardHudRequestedEvent(playerRef, leaderboardText),
        HUD_SEQUENCE_DELAY_MS);
  }

  private static void dispatch(@Nullable World world, @Nonnull MBRound18.ImmortalEngine.api.events.DebugEvent event) {
    WorldEventQueue.get().dispatch(world, event);
  }

  private static void dispatchDelayed(@Nullable World world,
      @Nonnull MBRound18.ImmortalEngine.api.events.DebugEvent event,
      long delayMs) {
    try {
      HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> dispatch(world, event), delayMs, TimeUnit.MILLISECONDS);
    } catch (Exception ignored) {
      dispatch(world, event);
    }
  }
}
