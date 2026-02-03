package MBRound18.hytale.shared.interfaces.abstracts;

import MBRound18.hytale.shared.interfaces.ui.PlayerSubscriptionController;
import MBRound18.hytale.shared.interfaces.ui.UiThread;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractCustomUIController<T extends AbstractCustomUIHud> {
  private final Logger logger;
  private final PlayerSubscriptionController subscriptions;
  private final ConcurrentHashMap<UUID, T> huds = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, Boolean> readyFlags = new ConcurrentHashMap<>();

  protected AbstractCustomUIController(@Nonnull Logger logger, @Nonnull String tickerName) {
    this.logger = Objects.requireNonNull(logger, "logger");
    this.subscriptions = new PlayerSubscriptionController(this.logger, tickerName);
  }

  protected final void enqueue(@Nonnull PlayerRef playerRef, @Nonnull Runnable action) {
    subscriptions.enqueue(playerRef, action);
  }

  protected final void enqueueDelay(@Nonnull PlayerRef playerRef, int ticks) {
    subscriptions.enqueueDelay(playerRef, ticks);
  }

  protected final void clearQueue(@Nonnull PlayerRef playerRef) {
    subscriptions.clearQueue(playerRef);
  }

  protected final void runOnWorld(@Nonnull PlayerRef playerRef, @Nonnull Runnable action) {
    UiThread.runOnPlayerWorld(playerRef, action);
  }

  protected final void putHud(@Nonnull PlayerRef playerRef, @Nonnull T hud) {
    huds.put(playerRef.getUuid(), hud);
  }

  @Nullable
  protected final T getHud(@Nonnull PlayerRef playerRef) {
    return huds.get(playerRef.getUuid());
  }

  @Nullable
  protected final T removeHud(@Nonnull PlayerRef playerRef) {
    readyFlags.remove(playerRef.getUuid());
    return huds.remove(playerRef.getUuid());
  }

  protected final void cancelTimer(@Nonnull PlayerRef playerRef) {
    subscriptions.cancelTimer(playerRef);
  }

  protected final boolean openHud(@Nonnull PlayerRef playerRef, @Nonnull Supplier<T> hudSupplier,
      @Nonnull Consumer<T> onReady) {
    if (!playerRef.isValid()) {
      return false;
    }

    closeHud(playerRef, AbstractCustomUIHud::clear);

    enqueue(playerRef, () -> runOnWorld(playerRef, () -> {
      if (!playerRef.isValid()) {
        return;
      }

      T hud = Objects.requireNonNull(hudSupplier.get(), "hud");
      putHud(playerRef, hud);

      if (!applyHud(playerRef, hud)) {
        removeHud(playerRef);
        return;
      }

      armHud(hud);
      readyFlags.put(playerRef.getUuid(), true);
      onReady.accept(hud);
    }));

    return true;
  }

  protected final void closeHud(@Nullable PlayerRef playerRef, @Nonnull Consumer<T> onRemoved) {
    if (playerRef == null) {
      return;
    }

    cancelTimer(playerRef);
    T removedHud = removeHud(playerRef);

    enqueue(playerRef, () -> runOnWorld(playerRef, () -> {
      if (!playerRef.isValid()) {
        return;
      }
      if (removedHud == null) {
        return;
      }
      HudManager hudManager = getHudManager(playerRef);
      if (hudManager == null || hudManager.getCustomHud() != removedHud) {
        return;
      }
      onRemoved.accept(removedHud);
    }));
  }

  protected final Subscription subscribePerSecond(@Nonnull PlayerRef playerRef, @Nonnull Consumer<T> onTick,
      @Nonnull Runnable onInvalid) {
    PlayerSubscriptionController.Subscription task = subscriptions.subscribeAtFixedRate(
        playerRef, 1, 1, TimeUnit.SECONDS,
        () -> enqueue(playerRef, () -> runOnWorld(playerRef, () -> {
          T hud = getReadyHud(playerRef, onInvalid);
          if (hud == null) {
            return;
          }
          onTick.accept(hud);
        })));
    return new Subscription(Objects.requireNonNull(task, "subscription"));
  }

  protected final Subscription subscribePerTick(@Nonnull PlayerRef playerRef, @Nonnull Consumer<T> onTick,
      @Nonnull Runnable onInvalid) {
    PlayerSubscriptionController.Subscription task = subscriptions.subscribeAtFixedRate(
        playerRef, 0, 50, TimeUnit.MILLISECONDS,
        () -> enqueue(playerRef, () -> runOnWorld(playerRef, () -> {
          T hud = getReadyHud(playerRef, onInvalid);
          if (hud == null) {
            return;
          }
          onTick.accept(hud);
        })));
    return new Subscription(Objects.requireNonNull(task, "subscription"));
  }

  protected final ScheduledFuture<?> scheduleAtFixedRate(@Nonnull PlayerRef playerRef,
      long initialDelay, long period, @Nonnull TimeUnit unit, @Nonnull Runnable action) {
    return subscriptions.scheduleAtFixedRate(playerRef, initialDelay, period, unit, action);
  }

  protected final ScheduledFuture<?> scheduleOnce(@Nonnull PlayerRef playerRef, long delay,
      @Nonnull TimeUnit unit, @Nonnull Runnable action) {
    return subscriptions.scheduleOnce(playerRef, delay, unit, action);
  }

  protected final boolean applyHud(@Nonnull PlayerRef playerRef, @Nonnull T hud) {
    HudManager hudManager = getHudManager(playerRef);
    if (hudManager == null) {
      return false;
    }
    hudManager.setCustomHud(playerRef, hud);
    return true;
  }

  @Nullable
  protected final Player getPlayer(@Nullable PlayerRef playerRef) {
    if (playerRef == null || !playerRef.isValid()) {
      return null;
    }
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return null;
    }
    Store<EntityStore> store = ref.getStore();
    return store.getComponent(ref, Player.getComponentType());
  }

  @Nullable
  protected final HudManager getHudManager(@Nullable PlayerRef playerRef) {
    Player player = getPlayer(playerRef);
    return player != null ? player.getHudManager() : null;
  }

  protected final void logInfo(@Nonnull String format, Object... args) {
    logger.info(String.format(format, args));
  }

  protected final void logWarn(@Nonnull String format, Object... args) {
    logger.warning(String.format(format, args));
  }

  private void armHud(@Nonnull T hud) {
    UICommandBuilder builder = new UICommandBuilder();
    if (builder.getCommands() != null) {
      hud.update(false, Objects.requireNonNull(builder, "builder"));
    }
  }

  @Nullable
  private T getReadyHud(@Nonnull PlayerRef playerRef, @Nonnull Runnable onInvalid) {
    if (!playerRef.isValid()) {
      onInvalid.run();
      return null;
    }

    T hud = getHud(playerRef);
    if (hud == null || !Boolean.TRUE.equals(readyFlags.get(playerRef.getUuid()))) {
      return null;
    }

    HudManager hudManager = getHudManager(playerRef);
    if (hudManager == null || hudManager.getCustomHud() != hud) {
      onInvalid.run();
      return null;
    }

    return hud;
  }

  protected static final class Subscription {
    private final PlayerSubscriptionController.Subscription subscription;

    private Subscription(@Nonnull PlayerSubscriptionController.Subscription subscription) {
      this.subscription = subscription;
    }

    public void cancel() {
      subscription.cancel();
    }
  }
}
