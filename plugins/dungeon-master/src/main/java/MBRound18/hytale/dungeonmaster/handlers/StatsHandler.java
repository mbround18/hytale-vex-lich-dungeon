package MBRound18.hytale.dungeonmaster.handlers;

import MBRound18.hytale.dungeonmaster.helpers.WebContext;
import com.google.gson.Gson;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

public final class StatsHandler {
  private final @Nonnull Gson gson;
  private final @Nonnull Supplier<Integer> registeredTypes;
  private final @Nonnull Supplier<Integer> clientsConnected;
  private final @Nonnull Supplier<Integer> bufferSize;

  public StatsHandler(@Nonnull Gson gson, @Nonnull Supplier<Integer> registeredTypes,
      @Nonnull Supplier<Integer> clientsConnected, @Nonnull Supplier<Integer> bufferSize) {
    this.gson = java.util.Objects.requireNonNull(gson, "gson");
    this.registeredTypes = java.util.Objects.requireNonNull(registeredTypes, "registeredTypes");
    this.clientsConnected = java.util.Objects.requireNonNull(clientsConnected, "clientsConnected");
    this.bufferSize = java.util.Objects.requireNonNull(bufferSize, "bufferSize");
  }

  public void handle(@Nonnull HttpExchange exchange) throws IOException {
    Runtime rt = Runtime.getRuntime();
    Map<String, Object> stats = new LinkedHashMap<>();

    stats.put("system", Map.of(
        "uptime_ms", ManagementFactory.getRuntimeMXBean().getUptime(),
        "threads_active", Thread.activeCount(),
        "memory_free", rt.freeMemory(),
        "memory_total", rt.totalMemory(),
        "memory_max", rt.maxMemory()));

    List<Map<String, Object>> worldStats = new ArrayList<>();
    if (Universe.get() != null && Universe.get().getWorlds() != null) {
      for (World w : Universe.get().getWorlds().values()) {
        if (w == null) {
          continue;
        }
        worldStats.add(Map.of(
            "name", w.getName(),
            "players", w.getPlayerCount(),
            "loaded_chunks", -1));
      }
    }
    stats.put("worlds", worldStats);

    stats.put("events", Map.of(
        "registered_types", registeredTypes.get(),
        "clients_connected", clientsConnected.get(),
        "buffer_size", bufferSize.get()));

    new WebContext(exchange, gson).json(stats);
  }
}
