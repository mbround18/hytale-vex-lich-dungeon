package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.EventDispatcher;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;

public final class WorldEventQueue {
  private static final WorldEventQueue INSTANCE = new WorldEventQueue();
  private final LoggingHelper log = new LoggingHelper("WorldEventQueue");
  private final ExecutorService globalQueue;

  private WorldEventQueue() {
    this.globalQueue = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "vex-global-events");
      t.setDaemon(true);
      return t;
    });
  }

  public static WorldEventQueue get() {
    return INSTANCE;
  }

  public void dispatch(@Nullable World world, @Nullable IEvent<Void> event) {
    if (event == null) {
      return;
    }
    if (world == null) {
      dispatchGlobal(event);
      return;
    }
    String worldName = world.getName();
    if (worldName == null || worldName.isBlank()) {
      dispatchGlobal(event);
      return;
    }
    try {
      world.execute(() -> EventDispatcher.dispatch(HytaleServer.get().getEventBus(), event));
    } catch (Exception e) {
      try {
        EventDispatcher.dispatch(HytaleServer.get().getEventBus(), event);
      } catch (Exception ignored) {
        // ignore
      }
      log.warn("World event dispatch fallback for %s: %s", worldName, e.getMessage());
    }
  }

  public void dispatchGlobal(@Nullable IEvent<Void> event) {
    if (event == null) {
      return;
    }
    globalQueue.execute(() -> EventDispatcher.dispatch(HytaleServer.get().getEventBus(), event));
  }

  public void releaseWorld(@Nullable String worldName) {
    if (worldName == null || worldName.isBlank()) {
      return;
    }
  }

  public void shutdown() {
    globalQueue.shutdownNow();
  }
}
