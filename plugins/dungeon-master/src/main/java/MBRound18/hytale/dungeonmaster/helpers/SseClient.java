package MBRound18.hytale.dungeonmaster.helpers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SseClient {
  private final HttpExchange exchange;
  private final OutputStream out;
  private final Set<String> allowedTypes;

  public SseClient(@Nonnull HttpExchange exchange, @Nonnull OutputStream out, @Nullable Set<String> allowedTypes) {
    this.exchange = exchange;
    this.out = out;
    this.allowedTypes = allowedTypes;
  }

  public boolean accepts(String type) {
    if (allowedTypes == null || allowedTypes.isEmpty()) {
      return true;
    }
    for (String allowed : allowedTypes) {
      if (type.equals(allowed) || type.endsWith("." + allowed)) {
        return true;
      }
    }
    return false;
  }

  public boolean send(@Nonnull byte[] bytes) {
    try {
      out.write(bytes);
      out.flush();
      return true;
    } catch (IOException e) {
      close();
      return false;
    }
  }

  public void close() {
    try {
      out.close();
    } catch (IOException ignored) {
      // ignore
    }
    try {
      exchange.close();
    } catch (Exception ignored) {
      // ignore
    }
  }
}
