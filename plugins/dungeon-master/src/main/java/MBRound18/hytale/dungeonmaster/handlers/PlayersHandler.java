package MBRound18.hytale.dungeonmaster.handlers;

import MBRound18.hytale.dungeonmaster.helpers.PlayerPositionResolver;
import MBRound18.hytale.dungeonmaster.helpers.WebContext;
import com.google.gson.Gson;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

public final class PlayersHandler {
  private final @Nonnull Gson gson;
  private final long timeoutMs;

  public PlayersHandler(@Nonnull Gson gson, long timeoutMs) {
    this.gson = java.util.Objects.requireNonNull(gson, "gson");
    this.timeoutMs = timeoutMs;
  }

  public void handle(@Nonnull HttpExchange exchange) throws IOException {
    List<Map<String, Object>> players = new ArrayList<>();
    if (Universe.get() != null && Universe.get().getWorlds() != null) {
      for (World world : Universe.get().getWorlds().values()) {
        if (world == null) {
          continue;
        }
        for (PlayerRef ref : world.getPlayerRefs()) {
          if (ref == null) {
            continue;
          }
          Map<String, Object> entry = new HashMap<>();
          entry.put("uuid", ref.getUuid());
          entry.put("name", ref.getUsername());
          entry.put("world", world.getName());
          Vector3d pos = null;
          try {
            pos = PlayerPositionResolver.resolvePositionAsync(ref, world)
                .get(timeoutMs, TimeUnit.MILLISECONDS);
          } catch (Exception ignored) {
            pos = null;
          }
          if (pos != null) {
            entry.put("position", Map.of("x", pos.x, "y", pos.y, "z", pos.z));
          }
          players.add(entry);
        }
      }
    }
    new WebContext(exchange, gson).json(players);
  }
}
