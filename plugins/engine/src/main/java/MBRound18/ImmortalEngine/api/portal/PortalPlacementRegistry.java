package MBRound18.ImmortalEngine.api.portal;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;

/**
 * Tracks temporary portal placements and closes them automatically.
 * <p>
 * Typical usage:
 * <ul>
 * <li>{@link #register(String, String, Vector3i, long, int)} when spawning a
 * portal block</li>
 * <li>{@link #tick()} from a server loop to close expired/overfull portals</li>
 * <li>{@link #closePortals(String)} to force-close for a specific instance</li>
 * </ul>
 */
public final class PortalPlacementRegistry {
  private static final Map<String, List<PortalPlacement>> PLACEMENTS = new ConcurrentHashMap<>();

  private PortalPlacementRegistry() {
  }

  /**
   * Registers a portal placement with no expiry/max-player conditions.
   */
  public static void register(@Nonnull String instanceId, @Nonnull String worldName,
      @Nonnull Vector3i position) {
    PLACEMENTS.computeIfAbsent(instanceId, key -> new CopyOnWriteArrayList<>())
        .add(new PortalPlacement(worldName, position, 0L, -1));
  }

  /**
   * Registers a portal placement with optional expiry and max-player limits.
   *
   * @param expiresAtMs epoch millis after which the portal should be closed (0 to
   *                    ignore)
   * @param maxPlayers  max players allowed in the instance before closing (<=0 to
   *                    ignore)
   */
  public static void register(@Nonnull String instanceId, @Nonnull String worldName,
      @Nonnull Vector3i position, long expiresAtMs, int maxPlayers) {
    PLACEMENTS.computeIfAbsent(instanceId, key -> new CopyOnWriteArrayList<>())
        .add(new PortalPlacement(worldName, position, expiresAtMs, maxPlayers));
  }

  /**
   * Immediately closes all tracked portals for an instance.
   */
  public static void closePortals(@Nonnull String instanceId) {
    List<PortalPlacement> placements = PLACEMENTS.get(instanceId);
    if (placements == null || placements.isEmpty()) {
      return;
    }
    for (PortalPlacement placement : placements) {
      removePortalPlacement(Objects.requireNonNull(placement, "placement"));
    }
    placements.clear();
  }

  /**
   * Ticks the registry, closing any portals that are expired or over player
   * limit.
   * This should be called periodically (e.g., once per server tick).
   */
  public static void tick() {
    long now = System.currentTimeMillis();
    for (Map.Entry<String, List<PortalPlacement>> entry : PLACEMENTS.entrySet()) {
      String instanceId = entry.getKey();
      if (instanceId == null) {
        continue;
      }
      List<PortalPlacement> placements = entry.getValue();
      if (placements == null || placements.isEmpty()) {
        continue;
      }

      World instanceWorld = Universe.get().getWorld(instanceId);
      if (instanceWorld == null) {
        continue;
      }

      instanceWorld.execute(() -> tickWorld(instanceId, instanceWorld, placements, now));
    }
  }

  private static void tickWorld(@Nonnull String instanceId, @Nonnull World instanceWorld,
      @Nonnull List<PortalPlacement> placements, long now) {
    int playerCount = instanceWorld.getPlayerCount();

    boolean closeForMaxPlayers = false;
    for (PortalPlacement placement : placements) {
      if (placement.maxPlayers > 0 && playerCount >= placement.maxPlayers) {
        closeForMaxPlayers = true;
        break;
      }
    }

    List<PortalPlacement> toRemove = new java.util.ArrayList<>();
    for (PortalPlacement placement : placements) {
      boolean expired = placement.expiresAtMs > 0 && now >= placement.expiresAtMs;
      if (expired || closeForMaxPlayers) {
        removePortalPlacement(placement);
        toRemove.add(placement);
      }
    }
    if (!toRemove.isEmpty()) {
      placements.removeAll(toRemove);
    }
    if (placements.isEmpty()) {
      PLACEMENTS.remove(instanceId);
    }
  }

  private static void removePortalPlacement(@Nonnull PortalPlacement placement) {
    World world = Universe.get().getWorld(Objects.requireNonNull(placement.worldName, "worldName"));
    if (world == null) {
      return;
    }
    world.execute(() -> removePortal(world,
        Objects.requireNonNull(placement.position, "position")));
  }

  private static void removePortal(@Nonnull World world, @Nonnull Vector3i pos) {
    long chunkKey = (((long) (pos.x >> 4)) << 32) | (((long) (pos.z >> 4)) & 0xFFFFFFFFL);
    BlockAccessor chunk = world.getChunkIfLoaded(chunkKey);
    if (chunk == null) {
      return;
    }
    chunk.setBlock(pos.x, pos.y, pos.z, 0);
  }

  private static final class PortalPlacement {
    private final String worldName;
    private final Vector3i position;
    private final long expiresAtMs;
    private final int maxPlayers;

    private PortalPlacement(String worldName, Vector3i position, long expiresAtMs, int maxPlayers) {
      this.worldName = worldName;
      this.position = position;
      this.expiresAtMs = expiresAtMs;
      this.maxPlayers = maxPlayers;
    }
  }
}
