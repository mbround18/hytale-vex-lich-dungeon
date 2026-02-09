package MBRound18.hytale.dungeonmaster.helpers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public final class WebContext {
  private final HttpExchange exchange;
  private final Map<String, String> queryParams;
  private final Gson gson;

  public WebContext(@Nonnull HttpExchange exchange, @Nonnull Gson gson) {
    this.exchange = exchange;
    this.gson = gson;
    this.queryParams = parseQuery(exchange.getRequestURI());
  }

  public String queryParam(String key) {
    return queryParams.get(key);
  }

  public long queryLong(String key, long defaultValue) {
    String val = queryParams.get(key);
    if (val == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(val);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public String pathParam(String routePrefix) {
    String path = exchange.getRequestURI().getRawPath();
    if (path == null || !path.startsWith(routePrefix)) {
      return null;
    }
    String val = path.substring(routePrefix.length());
    return URLDecoder.decode(val, StandardCharsets.UTF_8);
  }

  public void json(Object object) throws IOException {
    String json = gson.toJson(object);
    respond(200, json, "application/json");
  }

  public void text(int status, String text) throws IOException {
    respond(status, text, "text/plain");
  }

  private void respond(int status, String body, String contentType) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  private Map<String, String> parseQuery(URI uri) {
    Map<String, String> params = new HashMap<>();
    String query = uri.getRawQuery();
    if (query == null || query.isBlank()) {
      return params;
    }
    for (String pair : query.split("&")) {
      int idx = pair.indexOf('=');
      if (idx > 0) {
        try {
          String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
          String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
          params.put(key, value);
        } catch (Exception ignored) {
          // ignore malformed pairs
        }
      }
    }
    return params;
  }
}
