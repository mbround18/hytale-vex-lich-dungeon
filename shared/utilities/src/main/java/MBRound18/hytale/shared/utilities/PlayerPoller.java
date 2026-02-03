package MBRound18.hytale.shared.utilities;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PlayerPoller {
    // Shared scheduler using virtual threads (Java 25)
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1,
            Thread.ofVirtual().name("player-poller-", 0).factory());
    private static LoggingHelper log = new LoggingHelper("PlayerPoller");

    private ScheduledFuture<?> activeTask;

    /**
     * Starts polling the action at the specified interval.
     */
    public void start(PlayerRef playerRef, long intervalMs, Runnable action) {
        stop(); // Cleanup previous task

        if (playerRef == null || !playerRef.isValid())
            return;

        activeTask = SCHEDULER.scheduleAtFixedRate(() -> {
            try {

                if (!playerRef.isValid()) {
                    stop();
                    return;
                }

                // Bridge to World Thread
                MBRound18.hytale.shared.utilities.UiThread.runOnPlayerWorld(playerRef, () -> {
                    try {
                        if (!playerRef.isValid()) {
                            stop();
                            return;
                        }

                        action.run();
                    } catch (Throwable t) {
                        log.error("Error executing PlayerPoller action:", t);
                        t.printStackTrace();
                    }
                });

            } catch (Throwable t) {
                // This catches errors in the scheduler itself (e.g. playerRef checks)
                log.error("Error in PlayerPoller task:", t);
                t.printStackTrace();

            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

    }

    public void stop() {
        if (activeTask != null && !activeTask.isCancelled()) {
            activeTask.cancel(false);
        }
        activeTask = null;
    }

}
