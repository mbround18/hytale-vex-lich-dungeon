package MBRound18.ImmortalEngine.api.events;

import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class WorldCreatedEvent extends DebugEvent {
  @Nonnull
  private final World world;

  public WorldCreatedEvent(@Nonnull World world) {
    this.world = Objects.requireNonNull(world, "world");
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Override
  public Object toPayload() {
    return withCorrelation(onWorldThread(world, () -> {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("world", worldMeta(world));
      return data;
    }));
  }
}
