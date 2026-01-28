package MBRound18.PortalEngine;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import javax.annotation.Nonnull;

/**
 * Headless microgame engine plugin entrypoint.
 */
public class PortalEnginePlugin extends JavaPlugin {

  public PortalEnginePlugin(@Nonnull JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    // Engine runtime is bootstrapped by adapters in future revisions.
  }

  @Override
  protected void start() {
    // No-op for now.
  }

  @Override
  protected void shutdown() {
    // No-op for now.
  }
}