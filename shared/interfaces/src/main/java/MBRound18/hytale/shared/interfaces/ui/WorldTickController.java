package MBRound18.hytale.shared.interfaces.ui;

import com.hypixel.hytale.server.core.util.thread.TickingThread;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WorldTickController {
  private final TickQueue tickQueue;

  public WorldTickController(@Nonnull String name, @Nonnull Logger logger) {
    this(name, logger, null);
  }

  public WorldTickController(@Nonnull String name, @Nonnull Logger logger, @Nullable Consumer<UUID> tickListener) {
    this.tickQueue = new TickQueue(name, logger, tickListener);
    this.tickQueue.start();
  }

  public void enqueue(@Nonnull UUID key, @Nonnull Runnable action) {
    tickQueue.enqueue(key, action);
  }

  public void enqueueDelay(@Nonnull UUID key, int ticks) {
    tickQueue.enqueueDelay(key, ticks);
  }

  public void clearQueue(@Nonnull UUID key) {
    tickQueue.clearQueue(key);
  }

  private static final class TickQueue extends TickingThread {
    private final ConcurrentHashMap<UUID, Deque<QueueOp>> queues = new ConcurrentHashMap<>();
    private final Logger logger;
    private final Consumer<UUID> tickListener;

    private TickQueue(@Nonnull String name, @Nonnull Logger logger, @Nullable Consumer<UUID> tickListener) {
      super(name, 20, true);
      this.logger = logger;
      this.tickListener = tickListener;
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
              if (tickListener != null) {
                tickListener.accept(uuid);
              }
            } catch (Exception e) {
              logger.log(Level.SEVERE, "Error executing queued tick task for " + uuid, e);
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
}
