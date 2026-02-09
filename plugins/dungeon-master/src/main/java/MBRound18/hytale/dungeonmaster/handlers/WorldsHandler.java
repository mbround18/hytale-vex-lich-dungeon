package MBRound18.hytale.dungeonmaster.handlers;

import MBRound18.hytale.dungeonmaster.helpers.WebContext;
import com.google.gson.Gson;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public final class WorldsHandler {
  private final @Nonnull Gson gson;

  public WorldsHandler(@Nonnull Gson gson) {
    this.gson = java.util.Objects.requireNonNull(gson, "gson");
  }

  public void handle(@Nonnull HttpExchange exchange) throws IOException {
    List<Map<String, Object>> worlds = new ArrayList<>();
    if (Universe.get() != null && Universe.get().getWorlds() != null) {
      for (World world : Universe.get().getWorlds().values()) {
        if (world == null) {
          continue;
        }
        Map<String, Object> entry = new HashMap<>();
        entry.put("name", world.getName());
        entry.put("playerCount", world.getPlayerCount());
        worlds.add(entry);
      }
    }
    new WebContext(exchange, gson).json(worlds);
  }
}
