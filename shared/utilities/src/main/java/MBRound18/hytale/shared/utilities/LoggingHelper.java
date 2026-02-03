package MBRound18.hytale.shared.utilities;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Locale;
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

  public void info(@Nonnull String message, Object... args) {
    logger.at(Level.INFO).log(format(message, args));
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

  public void warn(@Nonnull String message, Object... args) {
    logger.at(Level.WARNING).log(format(message, args));
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

  public void error(@Nonnull String message, Object... args) {
    logger.at(Level.SEVERE).log(format(message, args));
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

  public void debug(@Nonnull String message, Object... args) {
    logger.at(Level.FINE).log(format(message, args));
  }

  public void fine(@Nonnull String message) {
    debug(message);
  }

  public void fine(@Nonnull String message, Object... args) {
    debug(message, args);
  }

  private static String format(@Nonnull String message, Object... args) {
    if (args == null || args.length == 0) {
      return message;
    }
    return String.format(Locale.ROOT, message, args);
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
