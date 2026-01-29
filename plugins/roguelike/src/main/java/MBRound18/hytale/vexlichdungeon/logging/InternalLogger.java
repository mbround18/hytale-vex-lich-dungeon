package MBRound18.hytale.vexlichdungeon.logging;

import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * Simple internal logger that writes to the plugin data directory.
 */
public class InternalLogger {
  private final MBRound18.ImmortalEngine.api.logging.EngineInternalLogger delegate;

  public InternalLogger(@Nonnull Path dataDirectory) {
    this.delegate = new MBRound18.ImmortalEngine.api.logging.EngineInternalLogger(dataDirectory);
  }

  public InternalLogger(@Nonnull Path dataDirectory, @Nonnull String baseName) {
    this.delegate = new MBRound18.ImmortalEngine.api.logging.EngineInternalLogger(dataDirectory, baseName);
  }

  public synchronized void start() {
    delegate.start();
  }

  public synchronized void start(@Nonnull String startMessage) {
    delegate.start(startMessage);
  }

  public synchronized void close() {
    delegate.close();
  }

  public void info(@Nonnull String message) {
    delegate.info(message);
  }

  public void warn(@Nonnull String message) {
    delegate.warn(message);
  }

  public void error(@Nonnull String message) {
    delegate.error(message);
  }
}
