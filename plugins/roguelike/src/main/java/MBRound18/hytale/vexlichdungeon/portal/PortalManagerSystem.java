package MBRound18.hytale.vexlichdungeon.portal;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.VexLichDungeonPlugin;
import MBRound18.hytale.vexlichdungeon.commands.VexChallengeCommand;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.data.PortalPlacementRecord;
import MBRound18.hytale.vexlichdungeon.events.PortalEnteredEvent;
import MBRound18.hytale.vexlichdungeon.events.WorldEventQueue;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PortalManagerSystem extends TickingSystem<ChunkStore> {
  private static final long SWEEP_INTERVAL_MS = 1000L;
  private static final ConcurrentHashMap<UUID, PortalState> PORTAL_STATES = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<UUID, UUID> OWNER_TO_PORTAL = new ConcurrentHashMap<>();
  private static final ExecutorService EVENT_QUEUE = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "vex-portal-events");
    t.setDaemon(true);
    return t;
  });
  private final LoggingHelper log = new LoggingHelper("PortalManagerSystem");
  private long lastSweepMs = 0L;

  public static void registerPortalOwner(@Nonnull UUID portalId, @Nonnull UUID ownerId) {
    int maxEntries = resolveMaxEntries();
    PORTAL_STATES.computeIfAbsent(portalId, key -> new PortalState(portalId, ownerId, maxEntries));
    OWNER_TO_PORTAL.put(ownerId, portalId);
  }

  public static void handlePortalEntry(@Nullable PlayerRef playerRef, @Nullable World instanceWorld) {
    if (playerRef == null || instanceWorld == null) {
      return;
    }
    UUID playerId = playerRef.getUuid();
    String instanceName = instanceWorld.getName();
    if (instanceName == null || instanceName.isBlank()) {
      return;
    }

    UUID portalId = OWNER_TO_PORTAL.get(playerId);
    if (portalId != null) {
      PortalState state = PORTAL_STATES.get(portalId);
      if (state != null) {
        if (state.instanceWorldName == null) {
          state.instanceWorldName = instanceName;
        }
        registerEntry(state, playerId);
        WorldEventQueue.get().dispatch(instanceWorld,
            new PortalEnteredEvent(instanceWorld, playerRef, portalId));
        return;
      }
    }

    for (PortalState state : PORTAL_STATES.values()) {
      if (instanceName.equals(state.instanceWorldName)) {
        registerEntry(state, playerId);
        WorldEventQueue.get().dispatch(instanceWorld,
            new PortalEnteredEvent(instanceWorld, playerRef, state.portalId));
        return;
      }
    }
  }

  public static void requestPortalClose(@Nonnull UUID portalId) {
    expirePortal(portalId);
  }

  public static boolean isActivePortalForPlayer(@Nonnull UUID playerId, @Nonnull UUID portalId) {
    return portalId.equals(OWNER_TO_PORTAL.get(playerId));
  }

  static void enqueue(@Nonnull Runnable action) {
    Objects.requireNonNull(action, "action");
    EVENT_QUEUE.execute(action);
  }

  public static void shutdown() {
    EVENT_QUEUE.shutdownNow();
  }

  @Override
  public void tick(float deltaSeconds, int systemIndex, @Nonnull Store<ChunkStore> store) {
    long now = System.currentTimeMillis();
    if (now - lastSweepMs < SWEEP_INTERVAL_MS) {
      return;
    }
    lastSweepMs = now;

    Object external = store.getExternalData();
    if (!(external instanceof World)) {
      return;
    }
    World world = (World) external;
    String worldName = world.getName();
    if (worldName == null || worldName.isBlank()) {
      return;
    }

    VexLichDungeonPlugin plugin = VexLichDungeonPlugin.getInstance();
    if (plugin == null) {
      return;
    }
    DataStore dataStore = plugin.getDataStore();
    if (dataStore == null) {
      return;
    }

    List<UUID> expired = new ArrayList<>();
    for (PortalPlacementRecord record : dataStore.getPortalPlacements()) {
      if (record == null) {
        continue;
      }
      String recordWorld = record.getWorldName();
      if (recordWorld == null || !recordWorld.equals(worldName)) {
        continue;
      }
      long expiresAt = record.getExpiresAt();
      if (expiresAt > 0 && now >= expiresAt) {
        UUID portalId = record.getPortalId();
        if (portalId != null) {
          expired.add(portalId);
        }
        world.execute(() -> PortalSnapshotUtil.restore(world, record));
        continue;
      }
      UUID portalId = record.getPortalId();
      if (portalId == null) {
        continue;
      }
      PortalState state = PORTAL_STATES.get(portalId);
      if (state != null && state.instanceWorldName != null) {
        World instanceWorld = Universe.get().getWorld(state.instanceWorldName);
        int playerCount = instanceWorld != null ? instanceWorld.getPlayerCount() : 0;
        if (playerCount >= state.maxEntries) {
          expired.add(portalId);
          world.execute(() -> PortalSnapshotUtil.restore(world, record));
        }
      }
    }

    if (!expired.isEmpty()) {
      for (UUID portalId : expired) {
        expirePortal(Objects.requireNonNull(portalId, "portalId"));
      }
      log.info("Removed %d expired portal(s) in world %s", expired.size(), worldName);
    }
  }

  private static void registerEntry(@Nonnull PortalState state, @Nonnull UUID playerId) {
    if (!state.entryIds.add(playerId)) {
      return;
    }
    int entries = state.entryCount.incrementAndGet();
    if (entries >= state.maxEntries) {
      expirePortal(Objects.requireNonNull(state.portalId, "portalId"));
    }
  }

  private static void expirePortal(@Nonnull UUID portalId) {
    PortalState state = PORTAL_STATES.remove(portalId);
    if (state != null && state.ownerId != null) {
      UUID currentPortal = OWNER_TO_PORTAL.get(state.ownerId);
      if (portalId.equals(currentPortal)) {
        OWNER_TO_PORTAL.remove(state.ownerId);
        VexChallengeCommand.clearCountdownHud(state.ownerId);
      }
    }

    DataStore dataStore = resolveDataStore();
    if (dataStore == null) {
      return;
    }
    PortalPlacementRecord record = dataStore.getPortalPlacement(portalId).orElse(null);
    if (record != null) {
      World world = resolveWorld(record);
      if (world != null) {
        world.execute(() -> PortalSnapshotUtil.restore(world, record));
      }
      dataStore.removePortalPlacement(portalId);
    }
  }

  @Nullable
  private static DataStore resolveDataStore() {
    VexLichDungeonPlugin plugin = VexLichDungeonPlugin.getInstance();
    if (plugin == null) {
      return null;
    }
    return plugin.getDataStore();
  }

  @Nullable
  private static World resolveWorld(@Nonnull PortalPlacementRecord record) {
    UUID worldUuid = record.getWorldUuid();
    if (worldUuid != null) {
      World world = Universe.get().getWorld(worldUuid);
      if (world != null) {
        return world;
      }
    }
    String worldName = record.getWorldName();
    if (worldName != null && !worldName.isBlank()) {
      return Universe.get().getWorld(worldName);
    }
    return null;
  }

  private static final class PortalState {
    private final UUID portalId;
    private final UUID ownerId;
    private final int maxEntries;
    private final AtomicInteger entryCount;
    private final java.util.Set<UUID> entryIds;
    private volatile String instanceWorldName;

    private PortalState(@Nonnull UUID portalId, @Nonnull UUID ownerId, int maxEntries) {
      this.portalId = portalId;
      this.ownerId = ownerId;
      this.maxEntries = Math.max(1, maxEntries);
      this.entryCount = new AtomicInteger(0);
      this.entryIds = ConcurrentHashMap.newKeySet();
    }
  }

  private static int resolveMaxEntries() {
    DataStore dataStore = resolveDataStore();
    if (dataStore == null) {
      return 4;
    }
    return Math.max(1, dataStore.getConfig().getMaxPlayersPerInstance());
  }

  // Snapshot restore handled by PortalSnapshotUtil
}
