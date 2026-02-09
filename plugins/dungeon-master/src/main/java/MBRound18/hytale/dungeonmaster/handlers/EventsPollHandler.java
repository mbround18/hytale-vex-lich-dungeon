package MBRound18.hytale.dungeonmaster.handlers;

import MBRound18.hytale.dungeonmaster.helpers.EventEnvelope;
import MBRound18.hytale.dungeonmaster.helpers.WebContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nonnull;

public final class EventsPollHandler {
  private final @Nonnull Gson gson;
  private final @Nonnull Deque<EventEnvelope> events;

  public EventsPollHandler(@Nonnull Gson gson, @Nonnull Deque<EventEnvelope> events) {
    this.gson = java.util.Objects.requireNonNull(gson, "gson");
    this.events = java.util.Objects.requireNonNull(events, "events");
  }

  public void handle(@Nonnull HttpExchange exchange) throws IOException {
    WebContext ctx = new WebContext(exchange, gson);
    long since = ctx.queryLong("since", 0L);
    int limit = (int) Math.min(Math.max(ctx.queryLong("limit", 200L), 1L), 1000L);

    List<EventEnvelope> snapshot = new ArrayList<>();
    synchronized (events) {
      for (EventEnvelope envelope : events) {
        if (envelope.id() <= since) {
          continue;
        }
        snapshot.add(envelope);
        if (snapshot.size() >= limit) {
          break;
        }
      }
    }

    JsonObject payload = new JsonObject();
    payload.add("events", gson.toJsonTree(snapshot));
    long nextSince = snapshot.isEmpty() ? since : snapshot.get(snapshot.size() - 1).id();
    payload.addProperty("nextSince", nextSince);

    ctx.json(payload);
  }
}
