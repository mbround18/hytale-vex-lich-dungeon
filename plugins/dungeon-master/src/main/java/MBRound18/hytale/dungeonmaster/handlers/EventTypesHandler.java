package MBRound18.hytale.dungeonmaster.handlers;

import MBRound18.hytale.dungeonmaster.helpers.WebContext;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class EventTypesHandler {
  private final @Nonnull Gson gson;
  private final @Nonnull Set<Class<?>> registeredEventClasses;

  public EventTypesHandler(@Nonnull Gson gson, @Nonnull Set<Class<?>> registeredEventClasses) {
    this.gson = java.util.Objects.requireNonNull(gson, "gson");
    this.registeredEventClasses = java.util.Objects.requireNonNull(registeredEventClasses, "registeredEventClasses");
  }

  public void handle(@Nonnull HttpExchange exchange) throws IOException {
    List<String> types = registeredEventClasses.stream()
        .map(Class::getName)
        .sorted()
        .collect(Collectors.toList());
    new WebContext(exchange, gson).json(types);
  }
}
