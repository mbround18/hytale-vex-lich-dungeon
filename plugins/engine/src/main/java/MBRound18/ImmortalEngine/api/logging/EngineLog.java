package MBRound18.ImmortalEngine.api.logging;

import com.hypixel.hytale.logger.HytaleLogger;
import javax.annotation.Nonnull;

public final class EngineLog {
  private final HytaleLogger root;
  private final HytaleLogger lifecycle;

  EngineLog(@Nonnull HytaleLogger root) {
    this.root = root;
    this.lifecycle = root.getSubLogger("Lifecycle");
  }

  public HytaleLogger root() {
    return root;
  }

  public HytaleLogger lifecycle() {
    return lifecycle;
  }

  public HytaleLogger sub(@Nonnull String name) {
    return root.getSubLogger(name);
  }

  public void info(@Nonnull String message, Object... args) {
    if (args == null || args.length == 0) {
      root.atInfo().log(message);
    } else {
      root.atInfo().log(String.format(message, args));
    }
  }

  public void warn(@Nonnull String message, Object... args) {
    if (args == null || args.length == 0) {
      root.atWarning().log(message);
    } else {
      root.atWarning().log(String.format(message, args));
    }
  }

  public void error(@Nonnull String message, Object... args) {
    if (args == null || args.length == 0) {
      root.atSevere().log(message);
    } else {
      root.atSevere().log(String.format(message, args));
    }
  }
}
