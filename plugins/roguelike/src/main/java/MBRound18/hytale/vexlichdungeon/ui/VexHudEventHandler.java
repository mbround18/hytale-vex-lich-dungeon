package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.shared.utilities.UiThread;
import MBRound18.hytale.vexlichdungeon.events.PortalCountdownHudUpdateRequestedEvent;
import MBRound18.hytale.vexlichdungeon.portal.PortalManagerSystem;
import MBRound18.hytale.vexlichdungeon.events.VexDemoHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexLeaderboardHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexScoreHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexSummaryHudRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.VexWelcomeHudRequestedEvent;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VexHudEventHandler {
  private static final LoggingHelper logger = new LoggingHelper(VexHudEventHandler.class);
  private final UiExecutor uiExecutor;
  private final HudUpdateSink hudSink;

  public interface UiExecutor {
    boolean run(PlayerRef playerRef, Runnable handler);
  }

  public interface HudUpdateSink {
    void updateWelcome(PlayerRef playerRef, String bodyText);

    void updateScore(PlayerRef playerRef, int instanceScore, int playerScore, int delta, String partyList);

    void updateSummary(PlayerRef playerRef, String statsLine, String summaryLine);

    void updateLeaderboard(PlayerRef playerRef, String leaderboardText);

    void updateDemo(PlayerRef playerRef, String scoreText, String timerText, String debugStat);

    void updatePortalCountdown(PlayerRef playerRef, String timeLeft, String locationText);
  }

  private static final UiExecutor DEFAULT_UI_EXECUTOR = (playerRef, handler) -> UiThread.runOnPlayerWorld(playerRef,
      handler);

  private static final HudUpdateSink DEFAULT_HUD_SINK = new HudUpdateSink() {
    @Override
    public void updateWelcome(PlayerRef playerRef, String bodyText) {
      VexWelcomeHud.update(Objects.requireNonNull(playerRef, "playerRef"),
          Objects.requireNonNull(bodyText, "bodyText"));
    }

    @Override
    public void updateScore(PlayerRef playerRef, int instanceScore, int playerScore, int delta, String partyList) {
      VexScoreHud.update(Objects.requireNonNull(playerRef, "playerRef"), instanceScore, playerScore, delta,
          Objects.requireNonNull(partyList, "partyList"));
    }

    @Override
    public void updateSummary(PlayerRef playerRef, String statsLine, String summaryLine) {
      VexSummaryHud.update(Objects.requireNonNull(playerRef, "playerRef"),
          Objects.requireNonNull(statsLine, "statsLine"),
          Objects.requireNonNull(summaryLine, "summaryLine"));
    }

    @Override
    public void updateLeaderboard(PlayerRef playerRef, String leaderboardText) {
      VexLeaderboardHud.update(Objects.requireNonNull(playerRef, "playerRef"),
          Objects.requireNonNull(leaderboardText, "leaderboardText"));
    }

    @Override
    public void updateDemo(PlayerRef playerRef, String scoreText, String timerText, String debugStat) {
      VexDemoHud.update(Objects.requireNonNull(playerRef, "playerRef"),
          Objects.requireNonNull(scoreText, "scoreText"),
          Objects.requireNonNull(timerText, "timerText"),
          Objects.requireNonNull(debugStat, "debugStat"));
    }

    @Override
    public void updatePortalCountdown(PlayerRef playerRef, String timeLeft, String locationText) {
      VexPortalCountdownHud.update(Objects.requireNonNull(playerRef, "playerRef"),
          Objects.requireNonNull(timeLeft, "timeLeft"),
          Objects.requireNonNull(locationText, "locationText"));
    }
  };

  public VexHudEventHandler() {
    this(Objects.requireNonNull(DEFAULT_UI_EXECUTOR, "DEFAULT_UI_EXECUTOR"),
        Objects.requireNonNull(DEFAULT_HUD_SINK, "DEFAULT_HUD_SINK"));
  }

  public VexHudEventHandler(@Nonnull UiExecutor uiExecutor, @Nonnull HudUpdateSink hudSink) {
    this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
    this.hudSink = Objects.requireNonNull(hudSink, "hudSink");
  }

  // @SuppressWarnings({ "unchecked", "rawtypes" })
  // public void register(@Nonnull EventRegistry eventRegistry) {
  // Objects.requireNonNull(eventRegistry, "eventRegistry").registerGlobal(
  // (Class) PlayerReadyEvent.class,
  // (java.util.function.Consumer) (Object e) -> onPlayerReady(e));
  // }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    // Objects.requireNonNull(eventBus, "eventBus").register(
    // (Class) PlayerReadyEvent.class,
    // (java.util.function.Consumer) (Object e) -> onPlayerReady(e));

    Objects.requireNonNull(eventBus, "eventBus").register(
        (Class) VexWelcomeHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof VexWelcomeHudRequestedEvent event) {
            handleWelcome(event);
          }
        });

    eventBus.register((Class) VexScoreHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof VexScoreHudRequestedEvent event) {
            handleScore(event);
          }
        });

    eventBus.register((Class) VexSummaryHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof VexSummaryHudRequestedEvent event) {
            handleSummary(event);
          }
        });

    eventBus.register((Class) VexLeaderboardHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof VexLeaderboardHudRequestedEvent event) {
            handleLeaderboard(event);
          }
        });

    eventBus.register((Class) VexDemoHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof VexDemoHudRequestedEvent event) {
            handleDemo(event);
          }
        });

    eventBus.register((Class) PortalCountdownHudUpdateRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof PortalCountdownHudUpdateRequestedEvent event) {
            handlePortalCountdown(event);
          }
        });
  }

  void onEvent(@Nullable Object e) {
    if (e instanceof VexWelcomeHudRequestedEvent event) {
      handleWelcome(event);
    }
    if (e instanceof VexScoreHudRequestedEvent event) {
      handleScore(event);
    }
    if (e instanceof VexSummaryHudRequestedEvent event) {
      handleSummary(event);
    }
    if (e instanceof VexLeaderboardHudRequestedEvent event) {
      handleLeaderboard(event);
    }
    if (e instanceof VexDemoHudRequestedEvent event) {
      handleDemo(event);
    }
    if (e instanceof PortalCountdownHudUpdateRequestedEvent event) {
      handlePortalCountdown(event);
    }
  }

  private void handleWelcome(@Nonnull VexWelcomeHudRequestedEvent event) {
    PlayerRef playerRef = resolvePlayerRef(event.getPlayerRef());
    if (playerRef == null) {
      return;
    }
    handleUiInteraction(playerRef, () -> {
      hudSink.updateWelcome(playerRef, event.getBodyText());
    });
  }

  private void handleScore(@Nonnull VexScoreHudRequestedEvent event) {
    PlayerRef playerRef = resolvePlayerRef(event.getPlayerRef());
    if (playerRef == null) {
      return;
    }
    handleUiInteraction(playerRef, () -> {
      hudSink.updateScore(playerRef, event.getInstanceScore(), event.getPlayerScore(), event.getDelta(),
          event.getPartyList());
    });
  }

  private void handleSummary(@Nonnull VexSummaryHudRequestedEvent event) {
    PlayerRef playerRef = resolvePlayerRef(event.getPlayerRef());
    if (playerRef == null) {
      return;
    }
    handleUiInteraction(playerRef, () -> {
      hudSink.updateSummary(playerRef, event.getStatsLine(), event.getSummaryLine());
    });
  }

  private void handleLeaderboard(@Nonnull VexLeaderboardHudRequestedEvent event) {
    PlayerRef playerRef = resolvePlayerRef(event.getPlayerRef());
    if (playerRef == null) {
      return;
    }
    handleUiInteraction(playerRef, () -> {
      hudSink.updateLeaderboard(playerRef, event.getLeaderboardText());
    });
  }

  private void handleDemo(@Nonnull VexDemoHudRequestedEvent event) {
    PlayerRef playerRef = resolvePlayerRef(event.getPlayerRef());
    if (playerRef == null) {
      return;
    }
    handleUiInteraction(playerRef, () -> {
      hudSink.updateDemo(playerRef, event.getScoreText(), event.getTimerText(), event.getDebugStat());
    });
  }

  private void handlePortalCountdown(@Nonnull PortalCountdownHudUpdateRequestedEvent event) {
    PlayerRef playerRef = resolvePlayerRef(event.getPlayerRef());
    if (playerRef == null) {
      return;
    }
    if (!PortalManagerSystem.isActivePortalForPlayer(playerRef.getUuid(), event.getPortalId())) {
      return;
    }
    handleUiInteraction(playerRef, () -> {
      hudSink.updatePortalCountdown(playerRef, event.getTimeLeft(), event.getLocationText());
    });
  }

  @Nullable
  private PlayerRef resolvePlayerRef(@Nullable PlayerRef playerRef) {
    if (playerRef == null) {
      return null;
    }
    if (!playerRef.isValid()) {
      return null;
    }
    try {
      Universe universe = Universe.get();
      if (universe == null) {
        return playerRef;
      }
      PlayerRef refreshed = universe.getPlayer(playerRef.getUuid());
      return refreshed != null ? refreshed : playerRef;
    } catch (Throwable ignored) {
      return playerRef;
    }
  }

  private void handleUiInteraction(@Nonnull PlayerRef playerRef, Runnable handler) {
    if (!isReady(playerRef)) {
      logger.warn("Player not ready for UI interaction: " + playerRef.getUsername());
      return;
    }
    uiExecutor.run(playerRef, handler);
  }

  private boolean isReady(@Nullable PlayerRef playerRef) {
    return !(playerRef == null || !playerRef.isValid());
  }

}
