package MBRound18.ImmortalEngine;

import MBRound18.ImmortalEngine.api.events.AssetPacksLoadedEvent;
import MBRound18.ImmortalEngine.api.events.EventDispatcher;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.AssetPackRegisterEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

/**
 * Headless microgame engine plugin entrypoint.
 */
public class ImmortalEnginePlugin extends JavaPlugin {
  private final AtomicBoolean assetsLoaded = new AtomicBoolean(false);

  public ImmortalEnginePlugin(@Nonnull JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    // Engine runtime is bootstrapped by adapters in future revisions.
  }

  @Override
  protected void start() {
    EventBus eventBus = HytaleServer.get().getEventBus();
    if (eventBus == null) {
      return;
    }

    // Listen for asset pack registration and fire AssetPacksLoadedEvent
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Object listener = (java.util.function.Consumer) (Object e) -> {
      if (assetsLoaded.compareAndSet(false, true)) {
        EventDispatcher.dispatch(eventBus, new AssetPacksLoadedEvent());
      }
    };

    eventBus.register((Class) AssetPackRegisterEvent.class, (java.util.function.Consumer) listener);
  }

  @Override
  protected void shutdown() {
    // No-op for now.
  }
}
