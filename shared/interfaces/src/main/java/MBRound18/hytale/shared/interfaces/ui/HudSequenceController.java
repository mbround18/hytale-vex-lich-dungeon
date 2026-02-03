package MBRound18.hytale.shared.interfaces.ui;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Generic HUD sequencing helper.
 * <p>
 * This class schedules timed HUD transitions without coupling to any specific UI
 * assets. Consumers provide a {@link HudPresenter} and optional {@link DismissFactory}
 * to integrate with their own HUD system and dismiss UI.
 */
public final class HudSequenceController {

  /**
   * Presents and clears HUDs for a player.
   */
  public interface HudPresenter {
    void show(@Nonnull PlayerRef playerRef, @Nonnull String uiPath, @Nonnull Map<String, String> vars);
    void clear(@Nonnull PlayerRef playerRef);
  }

  /**
   * Handle for dismiss UI pages.
   */
  public interface DismissHandle {
    void requestClose();
  }

  /**
   * Factory for creating dismiss UI pages.
   */
  public interface DismissFactory {
    @Nullable
    DismissHandle open(@Nonnull PlayerRef playerRef);
  }

  private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "ImmortalEngineHudSequence");
    thread.setDaemon(true);
    return thread;
  });

  private static final ConcurrentHashMap<UUID, Sequence> ACTIVE = new ConcurrentHashMap<>();

  private HudSequenceController() {
  }

  /**
   * Shows a welcome HUD for a fixed duration and then clears it.
   */
  public static void showWelcome(@Nullable PlayerRef playerRef,
      @Nonnull String welcomeUiPath,
      @Nonnull Map<String, String> welcomeVars,
      long durationMs,
      @Nonnull HudPresenter presenter,
      @Nullable DismissFactory dismissFactory) {
    PlayerRef resolved = resolvePlayer(playerRef);
    if (resolved == null) {
      return;
    }
    UUID uuid = resolved.getUuid();
    cancelSequence(uuid, resolved, presenter);

    Sequence sequence = new Sequence(false);
    ACTIVE.put(uuid, sequence);
    if (dismissFactory != null) {
      sequence.dismissHandle = dismissFactory.open(resolved);
    }

    presenter.show(resolved, welcomeUiPath, welcomeVars);
    sequence.endFuture = SCHEDULER.schedule(() -> endSequence(uuid, presenter), durationMs, TimeUnit.MILLISECONDS);
  }

  /**
   * Shows a welcome HUD and then a persistent HUD (kept on screen).
   */
  public static void showWelcomeThenHud(@Nullable PlayerRef playerRef,
      @Nonnull String welcomeUiPath,
      @Nonnull Map<String, String> welcomeVars,
      long welcomeMs,
      @Nonnull String nextHudPath,
      @Nonnull Map<String, String> nextVars,
      @Nonnull HudPresenter presenter,
      @Nullable DismissFactory dismissFactory,
      boolean keepHud) {
    PlayerRef resolved = resolvePlayer(playerRef);
    if (resolved == null) {
      return;
    }
    UUID uuid = resolved.getUuid();
    cancelSequence(uuid, resolved, presenter);

    Sequence sequence = new Sequence(keepHud);
    ACTIVE.put(uuid, sequence);
    if (dismissFactory != null) {
      sequence.dismissHandle = dismissFactory.open(resolved);
    }

    presenter.show(resolved, welcomeUiPath, welcomeVars);
    sequence.nextFuture = SCHEDULER.schedule(() -> {
      PlayerRef nextRef = resolvePlayerById(uuid);
      if (nextRef == null) {
        endSequence(uuid, presenter);
        return;
      }
      presenter.show(nextRef, nextHudPath, nextVars);
    }, welcomeMs, TimeUnit.MILLISECONDS);
    sequence.endFuture = SCHEDULER.schedule(() -> endSequence(uuid, presenter), welcomeMs, TimeUnit.MILLISECONDS);
  }

  /**
   * Shows a two-step sequence (e.g., summary then leaderboard).
   */
  public static void showSequence(@Nullable PlayerRef playerRef,
      @Nonnull String firstPath,
      @Nonnull Map<String, String> firstVars,
      long firstMs,
      @Nonnull String secondPath,
      @Nonnull Map<String, String> secondVars,
      long secondMs,
      @Nonnull HudPresenter presenter,
      @Nullable DismissFactory dismissFactory) {
    PlayerRef resolved = resolvePlayer(playerRef);
    if (resolved == null) {
      return;
    }
    UUID uuid = resolved.getUuid();
    cancelSequence(uuid, resolved, presenter);

    Sequence sequence = new Sequence(false);
    ACTIVE.put(uuid, sequence);
    if (dismissFactory != null) {
      sequence.dismissHandle = dismissFactory.open(resolved);
    }

    presenter.show(resolved, firstPath, firstVars);

    sequence.nextFuture = SCHEDULER.schedule(() -> {
      PlayerRef nextRef = resolvePlayerById(uuid);
      if (nextRef == null) {
        endSequence(uuid, presenter);
        return;
      }
      presenter.show(nextRef, secondPath, secondVars);
    }, firstMs, TimeUnit.MILLISECONDS);

    sequence.endFuture = SCHEDULER.schedule(() -> endSequence(uuid, presenter), firstMs + secondMs,
        TimeUnit.MILLISECONDS);
  }

  /**
   * Dismisses any active sequence for a player.
   */
  public static void dismiss(@Nullable PlayerRef playerRef, @Nonnull HudPresenter presenter) {
    PlayerRef resolved = resolvePlayer(playerRef);
    if (resolved == null) {
      return;
    }
    cancelSequence(resolved.getUuid(), resolved, presenter);
  }

  private static void endSequence(@Nonnull UUID uuid, @Nonnull HudPresenter presenter) {
    PlayerRef ref = resolvePlayerById(uuid);
    cancelSequence(uuid, ref, presenter);
  }

  private static void cancelSequence(@Nonnull UUID uuid, @Nullable PlayerRef ref, @Nonnull HudPresenter presenter) {
    Sequence sequence = ACTIVE.remove(uuid);
    if (sequence != null) {
      if (sequence.nextFuture != null) {
        sequence.nextFuture.cancel(false);
      }
      if (sequence.endFuture != null) {
        sequence.endFuture.cancel(false);
      }
      if (sequence.dismissHandle != null) {
        sequence.dismissHandle.requestClose();
      }
    }
    if (ref != null && (sequence == null || !sequence.keepHud)) {
      presenter.clear(ref);
    }
  }

  @Nullable
  private static PlayerRef resolvePlayer(@Nullable PlayerRef ref) {
    if (ref == null || !ref.isValid()) {
      return null;
    }
    return ref;
  }

  @Nullable
  private static PlayerRef resolvePlayerById(@Nonnull UUID uuid) {
    PlayerRef ref = Universe.get().getPlayer(uuid);
    if (ref == null || !ref.isValid()) {
      return null;
    }
    return ref;
  }

  private static final class Sequence {
    private ScheduledFuture<?> nextFuture;
    private ScheduledFuture<?> endFuture;
    private DismissHandle dismissHandle;
    private final boolean keepHud;

    private Sequence(boolean keepHud) {
      this.keepHud = keepHud;
    }
  }
}
