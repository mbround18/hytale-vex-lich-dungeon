package MBRound18.hytale.vexlichdungeon.ui.core;

import MBRound18.ImmortalEngine.api.ui.UiThread;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.thread.TickingThread;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractCustomUIController<T extends AbstractCustomUIHud> {
  private final Logger logger;
  private final HudTicker ticker;
  private final ScheduledExecutorService timerScheduler;
  private final ConcurrentHashMap<UUID, T> huds = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, ScheduledFuture<?>> timerTasks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, Boolean> readyFlags = new ConcurrentHashMap<>();

  protected AbstractCustomUIController(@Nonnull Logger logger, @Nonnull String tickerName) {
    this.logger = logger;
    this.ticker = new HudTicker(tickerName, logger);
    this.ticker.start();
    this.timerScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, tickerName + "-Timer");
      t.setDaemon(true);
      return t;
    });
  }

  protected final void enqueue(@Nonnull PlayerRef playerRef, @Nonnull Runnable action) {
    ticker.enqueue(playerRef.getUuid(), action);
  }

  protected final void enqueueDelay(@Nonnull PlayerRef playerRef, int ticks) {
    ticker.enqueueDelay(playerRef.getUuid(), ticks);
  }

  protected final void clearQueue(@Nonnull PlayerRef playerRef) {
    ticker.clearQueue(playerRef.getUuid());
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
    ScheduledFuture<?> task = timerTasks.remove(playerRef.getUuid());
    if (task != null) {
      task.cancel(false);
    }
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

      T hud = hudSupplier.get();
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
    ScheduledFuture<?> task = scheduleAtFixedRate(playerRef, 1, 1, TimeUnit.SECONDS,
        () -> enqueue(playerRef, () -> runOnWorld(playerRef, () -> {
          T hud = getReadyHud(playerRef, onInvalid);
          if (hud == null) {
            return;
          }
          onTick.accept(hud);
        })));
    return new Subscription(task);
  }

  protected final Subscription subscribePerTick(@Nonnull PlayerRef playerRef, @Nonnull Consumer<T> onTick,
      @Nonnull Runnable onInvalid) {
    ScheduledFuture<?> task = scheduleAtFixedRate(playerRef, 0, 50, TimeUnit.MILLISECONDS,
        () -> enqueue(playerRef, () -> runOnWorld(playerRef, () -> {
          T hud = getReadyHud(playerRef, onInvalid);
          if (hud == null) {
            return;
          }
          onTick.accept(hud);
        })));
    return new Subscription(task);
  }

  protected final ScheduledFuture<?> scheduleAtFixedRate(@Nonnull PlayerRef playerRef,
      long initialDelay, long period, @Nonnull TimeUnit unit, @Nonnull Runnable action) {
    ScheduledFuture<?> task = timerScheduler.scheduleAtFixedRate(action, initialDelay, period, unit);
    timerTasks.put(playerRef.getUuid(), task);
    return task;
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
      hud.update(false, builder);
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

  private static final class HudTicker extends TickingThread {
    private final ConcurrentHashMap<UUID, Deque<QueueOp>> queues = new ConcurrentHashMap<>();
    private final Logger logger;

    private HudTicker(@Nonnull String name, @Nonnull Logger logger) {
      super(name, 20, true);
      this.logger = logger;
    }

    @Override
    protected void tick(float delta) {
      queues.forEach((uuid, deque) -> {
        QueueOp op;
        synchronized (deque) {
          if (deque.isEmpty()) {
            return;
          }
          op = deque.peek();
          if (op instanceof DelayOp) {
            DelayOp delay = (DelayOp) op;
            delay.ticksRemaining--;
            if (delay.ticksRemaining <= 0) {
              deque.poll();
            }
            return;
          }
          if (op instanceof RunnableOp) {
            RunnableOp task = (RunnableOp) op;
            try {
              task.action.run();
            } catch (Exception e) {
              logger.log(Level.SEVERE, "Error executing queued HUD task for " + uuid, e);
            }
            deque.poll();
          }
        }

        if (deque.isEmpty()) {
          queues.remove(uuid, deque);
        }
      });
    }

    @Override
    protected void onShutdown() {
      queues.clear();
    }

    private void enqueue(@Nonnull UUID uuid, @Nonnull Runnable action) {
      Deque<QueueOp> deque = queues.computeIfAbsent(uuid, k -> new ArrayDeque<>());
      synchronized (deque) {
        deque.add(new RunnableOp(action));
      }
    }

    private void enqueueDelay(@Nonnull UUID uuid, int ticks) {
      Deque<QueueOp> deque = queues.computeIfAbsent(uuid, k -> new ArrayDeque<>());
      synchronized (deque) {
        deque.add(new DelayOp(ticks));
      }
    }

    private void clearQueue(@Nonnull UUID uuid) {
      queues.remove(uuid);
    }
  }

  private interface QueueOp {
  }

  private static final class RunnableOp implements QueueOp {
    private final Runnable action;

    private RunnableOp(Runnable action) {
      this.action = action;
    }
  }

  private static final class DelayOp implements QueueOp {
    private int ticksRemaining;

    private DelayOp(int ticks) {
      this.ticksRemaining = ticks;
    }
  }

  protected static final class Subscription {
    private final ScheduledFuture<?> task;

    private Subscription(@Nonnull ScheduledFuture<?> task) {
      this.task = task;
    }

    public void cancel() {
      task.cancel(false);
    }
  }
}
