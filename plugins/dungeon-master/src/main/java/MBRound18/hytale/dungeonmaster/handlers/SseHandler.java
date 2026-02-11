package MBRound18.hytale.dungeonmaster.handlers;

import MBRound18.hytale.dungeonmaster.helpers.EventEnvelope;
import MBRound18.hytale.dungeonmaster.helpers.EventSerializationHelper;
import MBRound18.hytale.dungeonmaster.helpers.SseClient;
import MBRound18.hytale.dungeonmaster.helpers.WebContext;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;

public final class SseHandler {
  private final @Nonnull Gson gson;
  private final @Nonnull CopyOnWriteArrayList<SseClient> clients;
  private final @Nonnull Deque<EventEnvelope> events;

  public SseHandler(@Nonnull Gson gson, @Nonnull CopyOnWriteArrayList<SseClient> clients,
      @Nonnull Deque<EventEnvelope> events) {
    this.gson = java.util.Objects.requireNonNull(gson, "gson");
    this.clients = java.util.Objects.requireNonNull(clients, "clients");
    this.events = java.util.Objects.requireNonNull(events, "events");
  }

  public void handle(@Nonnull HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
    exchange.getResponseHeaders().set("Connection", "keep-alive");
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(200, 0);

    WebContext ctx = new WebContext(exchange, gson);
    String typesParam = ctx.queryParam("types");
    long since = ctx.queryLong("since", 0L);
    String skipSnapshotParam = ctx.queryParam("skipSnapshot");
    boolean skipSnapshot = "1".equals(skipSnapshotParam) || "true".equalsIgnoreCase(skipSnapshotParam);
    Set<String> allowedTypes = null;
    if (typesParam != null && !typesParam.isBlank()) {
      allowedTypes = new HashSet<>();
      Collections.addAll(allowedTypes, typesParam.split(","));
    }

    OutputStream out = java.util.Objects.requireNonNull(exchange.getResponseBody(), "body");
    SseClient client = new SseClient(exchange, out, allowedTypes);
    clients.add(client);
    client.send(java.util.Objects.requireNonNull(": connected\n\n".getBytes(StandardCharsets.UTF_8), "payload"));
    if (!skipSnapshot) {
      sendSnapshot(client, since);
    }
  }

  private void sendSnapshot(@Nonnull SseClient client, long since) {
    byte[] payload;
    synchronized (events) {
      for (EventEnvelope envelope : events) {
        if (envelope.id() <= since) {
          continue;
        }
        if (!client.accepts(envelope.type())) {
          continue;
        }
        String data = EventSerializationHelper.toJson(envelope);
        payload = ("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8);
        if (!client.send(payload)) {
          return;
        }
      }
    }
  }
}
