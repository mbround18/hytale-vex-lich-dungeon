package MBRound18.hytale.shared.interfaces.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerSubscriptionController {
  public static final class SubscriptionAction {
    public enum Type {
      SUBSCRIBE,
      UNSUBSCRIBE,
      TICK
    }

    private final @Nonnull Type type;
    private final long timestampMs;

    private SubscriptionAction(@Nonnull Type type, long timestampMs) {
      this.type = Objects.requireNonNull(type, "type");
      this.timestampMs = timestampMs;
    }

    @Nonnull
    public Type getType() {
      return Objects.requireNonNull(type, "type");
    }

    public long getTimestampMs() {
      return timestampMs;
    }

    public static @Nonnull SubscriptionAction subscribe() {
      return new SubscriptionAction(Type.SUBSCRIBE, System.currentTimeMillis());
    }

    public static @Nonnull SubscriptionAction unsubscribe() {
      return new SubscriptionAction(Type.UNSUBSCRIBE, System.currentTimeMillis());
    }

    public static @Nonnull SubscriptionAction tick() {
      return new SubscriptionAction(Type.TICK, System.currentTimeMillis());
    }
  }

  public static final class SubscriptionState {
    private final int activeSubscriptions;
    private final long lastTickMs;
    private final long lastUpdatedMs;

    private SubscriptionState(int activeSubscriptions, long lastTickMs, long lastUpdatedMs) {
      this.activeSubscriptions = activeSubscriptions;
      this.lastTickMs = lastTickMs;
      this.lastUpdatedMs = lastUpdatedMs;
    }

    public int getActiveSubscriptions() {
      return activeSubscriptions;
    }

    public long getLastTickMs() {
      return lastTickMs;
    }

    public long getLastUpdatedMs() {
      return lastUpdatedMs;
    }

    private static @Nonnull SubscriptionState empty() {
      return new SubscriptionState(0, 0L, 0L);
    }
  }

  private final WorldTickController tickController;
  private final ScheduledExecutorService scheduler;
  private final ConcurrentHashMap<UUID, ScheduledFuture<?>> timerTasks = new ConcurrentHashMap<>();
  private final PlayerStateStore<SubscriptionState, SubscriptionAction> stateStore;

  public PlayerSubscriptionController(@Nonnull Logger logger, @Nonnull String name) {
    this(logger, name, defaultStorageDir());
  }

  public PlayerSubscriptionController(@Nonnull Logger logger, @Nonnull String name, @Nullable Path storageDir) {
    Logger safeLogger = Objects.requireNonNull(logger, "logger");
    String safeName = Objects.requireNonNull(name, "name");
    this.stateStore = createStateStore(storageDir, safeLogger);
    this.stateStore.loadAll();
    this.tickController = new WorldTickController(safeName, safeLogger, uuid -> stateStore.dispatch(
        Objects.requireNonNull(uuid, "uuid"),
        Objects.requireNonNull(SubscriptionAction.tick(), "tick")));
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread thread = new Thread(r, safeName + "-Timer");
      thread.setDaemon(true);
      return thread;
    });
  }

  public void enqueue(@Nonnull PlayerRef playerRef, @Nonnull Runnable action) {
    tickController.enqueue(playerRef.getUuid(), action);
  }

  public void nextTick(@Nonnull PlayerRef playerRef, @Nonnull Runnable action) {
    tickController.enqueueDelay(playerRef.getUuid(), 1);
    tickController.enqueue(playerRef.getUuid(), action);
  }

  public void enqueueDelay(@Nonnull PlayerRef playerRef, int ticks) {
    tickController.enqueueDelay(playerRef.getUuid(), ticks);
  }

  public void clearQueue(@Nonnull PlayerRef playerRef) {
    tickController.clearQueue(playerRef.getUuid());
  }

  public void cancelTimer(@Nonnull PlayerRef playerRef) {
    ScheduledFuture<?> task = timerTasks.remove(playerRef.getUuid());
    if (task != null) {
      task.cancel(false);
    }
  }

  public ScheduledFuture<?> scheduleOnce(@Nonnull PlayerRef playerRef, long delay,
      @Nonnull TimeUnit unit, @Nonnull Runnable action) {
    ScheduledFuture<?> task = Objects.requireNonNull(scheduler.schedule(action, delay, unit), "task");
    timerTasks.put(playerRef.getUuid(), task);
    return task;
  }

  public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull PlayerRef playerRef, long initialDelay,
      long period, @Nonnull TimeUnit unit, @Nonnull Runnable action) {
    ScheduledFuture<?> task = Objects.requireNonNull(
        scheduler.scheduleAtFixedRate(action, initialDelay, period, unit),
        "task");
    timerTasks.put(playerRef.getUuid(), task);
    return task;
  }

  public Subscription subscribeAtFixedRate(@Nonnull PlayerRef playerRef, long initialDelay,
      long period, @Nonnull TimeUnit unit, @Nonnull Runnable action) {
    stateStore.dispatch(playerRef, Objects.requireNonNull(SubscriptionAction.subscribe(), "subscribe"));
    ScheduledFuture<?> task = Objects.requireNonNull(
        scheduleAtFixedRate(playerRef, initialDelay, period, unit, action),
        "scheduled");
    return new Subscription(this, playerRef, task);
  }

  public PlayerStateStore<SubscriptionState, SubscriptionAction> getStateStore() {
    return stateStore;
  }

  private static Path defaultStorageDir() {
    return Paths.get("data", "ImmortalEngine", "subscriptions");
  }

  private static PlayerStateStore<SubscriptionState, SubscriptionAction> createStateStore(
      @Nullable Path storageDir, @Nonnull Logger logger) {
    return new PlayerStateStore<>(
        storageDir,
        (uuid, ref, previous, action) -> {
          long now = action.getTimestampMs();
          SubscriptionState current = previous;
          return switch (action.getType()) {
            case SUBSCRIBE -> new SubscriptionState(
                current.getActiveSubscriptions() + 1,
                current.getLastTickMs(),
                now);
            case UNSUBSCRIBE -> new SubscriptionState(
                Math.max(0, current.getActiveSubscriptions() - 1),
                current.getLastTickMs(),
                now);
            case TICK -> new SubscriptionState(
                current.getActiveSubscriptions(),
                now,
                now);
          };
        },
        SubscriptionState::empty,
        new PlayerStateStore.StateSerializer<>() {
          @Override
          public @Nonnull JsonElement toJson(@Nonnull SubscriptionState state) {
            JsonObject obj = new JsonObject();
            obj.addProperty("activeSubscriptions", state.getActiveSubscriptions());
            obj.addProperty("lastTickMs", state.getLastTickMs());
            obj.addProperty("lastUpdatedMs", state.getLastUpdatedMs());
            return Objects.requireNonNull(obj, "json");
          }

          @Override
          public @Nonnull SubscriptionState fromJson(@Nonnull JsonElement json) {
            if (!json.isJsonObject()) {
              return SubscriptionState.empty();
            }
            JsonObject obj = json.getAsJsonObject();
            int active = obj.has("activeSubscriptions") ? obj.get("activeSubscriptions").getAsInt() : 0;
            long lastTick = obj.has("lastTickMs") ? obj.get("lastTickMs").getAsLong() : 0L;
            long lastUpdated = obj.has("lastUpdatedMs") ? obj.get("lastUpdatedMs").getAsLong() : 0L;
            return new SubscriptionState(active, lastTick, lastUpdated);
          }
        },
        logger,
        true);
  }

  public static final class Subscription {
    private final PlayerSubscriptionController controller;
    private final ScheduledFuture<?> task;
    private final PlayerRef playerRef;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private Subscription(@Nonnull PlayerSubscriptionController controller, @Nonnull PlayerRef playerRef,
        @Nonnull ScheduledFuture<?> task) {
      this.controller = controller;
      this.playerRef = playerRef;
      this.task = task;
    }

    public void cancel() {
      if (!cancelled.compareAndSet(false, true)) {
        return;
      }
      task.cancel(false);
      // Dispatch after cancel to keep counts consistent.
      controller.stateStore.dispatch(Objects.requireNonNull(playerRef, "playerRef"),
          Objects.requireNonNull(SubscriptionAction.unsubscribe(), "unsubscribe"));
    }
  }
}
