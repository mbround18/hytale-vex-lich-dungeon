package MBRound18.hytale.shared.utilities;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Objects;
import java.util.logging.Level;
import javax.annotation.Nonnull;

/**
 * Logging helper for consistent plugin logging throughout the application.
 * Provides convenient wrapper methods around HytaleLogger.
 */
public class LoggingHelper {
  private final HytaleLogger logger;

  /**
   * Create a new logging helper for the given class name.
   *
   * @param className The class name to use for the logger
   */
  public LoggingHelper(@Nonnull String className) {
    this.logger = HytaleLogger.get(className);
  }

  /**
   * Create a new logging helper for the given class.
   *
   * @param clazz The class to use for the logger name
   */
  public LoggingHelper(@Nonnull Class<?> clazz) {
    this.logger = HytaleLogger.get(clazz.getSimpleName());
  }

  /**
   * Log an info level message.
   *
   * @param message The message to log
   */
  @SuppressWarnings("null")
  public void info(@Nonnull String message) {
    logger.at(Level.INFO).log(message);
  }

  /**
   * Log a warning level message.
   *
   * @param message The message to log
   */
  @SuppressWarnings("null")
  public void warn(@Nonnull String message) {
    logger.at(Level.WARNING).log(message);
  }

  /**
   * Log a severe level message.
   *
   * @param message The message to log
   */
  @SuppressWarnings("null")
  public void error(@Nonnull String message) {
    logger.at(Level.SEVERE).log(message);
  }

  /**
   * Log a debug level message.
   *
   * @param message The message to log
   */
  @SuppressWarnings("null")
  public void debug(@Nonnull String message) {
    logger.at(Level.FINE).log(message);
  }

  /**
   * Get the underlying HytaleLogger instance.
   *
   * @return The HytaleLogger
   */
  @Nonnull
  public HytaleLogger getLogger() {
    return Objects.requireNonNull(logger);
  }
}
