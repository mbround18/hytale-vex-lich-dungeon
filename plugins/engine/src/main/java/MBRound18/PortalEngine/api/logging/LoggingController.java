package MBRound18.PortalEngine.api.logging;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import javax.annotation.Nonnull;

public final class LoggingController {
  private LoggingController() {
  }

  public static EngineLog forPlugin(@Nonnull JavaPlugin plugin, @Nonnull String componentName) {
    HytaleLogger base = plugin.getLogger().getSubLogger(componentName);
    return new EngineLog(base);
  }

  public static EngineLog forLogger(@Nonnull HytaleLogger logger) {
    return new EngineLog(logger);
  }
}
