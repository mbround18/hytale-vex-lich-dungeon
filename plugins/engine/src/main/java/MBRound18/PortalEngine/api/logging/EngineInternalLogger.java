package MBRound18.PortalEngine.api.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nonnull;

public class EngineInternalLogger {
  private static final long MAX_BYTES = 25L * 1024L * 1024L;
  private static final int MAX_LOGS = 10;

  private final Path logDir;
  private final Path logFile;
  private final String baseName;
  private BufferedWriter writer;

  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
      .withZone(ZoneId.systemDefault());

  public EngineInternalLogger(@Nonnull Path dataDirectory) {
    this(dataDirectory, "internals");
  }

  public EngineInternalLogger(@Nonnull Path dataDirectory, @Nonnull String baseName) {
    this.logDir = dataDirectory.resolve("logs");
    this.baseName = baseName;
    this.logFile = logDir.resolve(baseName + ".log");
  }

  public synchronized void start() {
    start("Internal logger started");
  }

  public synchronized void start(@Nonnull String startMessage) {
    try {
      Files.createDirectories(logDir);
      if (Files.exists(logFile)) {
        rotateLogs();
      }
      openWriter(false);
      info(startMessage);
    } catch (IOException e) {
      // no-op: avoid crashing plugin on logger failure
    }
  }

  public synchronized void close() {
    try {
      if (writer != null) {
        writer.flush();
        writer.close();
        writer = null;
      }
    } catch (IOException ignored) {
      // ignore
    }
  }

  public void info(@Nonnull String message) {
    log("INFO", message);
  }

  public void warn(@Nonnull String message) {
    log("WARN", message);
  }

  public void error(@Nonnull String message) {
    log("ERROR", message);
  }

  private synchronized void log(@Nonnull String level, @Nonnull String message) {
    try {
      ensureWriter();
      rotateIfOversize();
      String line = String.format("%s [%s] %s", TS.format(Instant.now()), level, message);
      writer.write(line);
      writer.newLine();
      writer.flush();
    } catch (IOException ignored) {
      // ignore
    }
  }

  private void ensureWriter() throws IOException {
    if (writer == null) {
      openWriter(false);
    }
  }

  private void openWriter(boolean truncate) throws IOException {
    if (truncate) {
      writer = Files.newBufferedWriter(
          logFile,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE);
    } else {
      writer = Files.newBufferedWriter(
          logFile,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    }
  }

  private void rotateIfOversize() throws IOException {
    if (Files.exists(logFile) && Files.size(logFile) >= MAX_BYTES) {
      rotateLogs();
      openWriter(false);
    }
  }

  private void rotateLogs() throws IOException {
    close();

    for (int i = MAX_LOGS - 1; i >= 1; i--) {
      Path src = logDir.resolve(baseName + "-" + i + ".log");
      Path dst = logDir.resolve(baseName + "-" + (i + 1) + ".log");
      if (Files.exists(src)) {
        Files.move(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
    }

    if (Files.exists(logFile)) {
      Path rotated = logDir.resolve(baseName + "-1.log");
      Files.copy(logFile, rotated, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    Files.newBufferedWriter(
        logFile,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE).close();

    Path overflow = logDir.resolve(baseName + "-" + (MAX_LOGS + 1) + ".log");
    if (Files.exists(overflow)) {
      Files.deleteIfExists(overflow);
    }
  }
}
