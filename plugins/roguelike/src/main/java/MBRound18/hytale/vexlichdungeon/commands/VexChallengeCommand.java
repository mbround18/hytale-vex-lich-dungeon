package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.ImmortalEngine.api.prefab.PrefabEntityUtils;
import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.shared.utilities.PlayerPoller;
import MBRound18.hytale.shared.utilities.UiThread;
import MBRound18.hytale.vexlichdungeon.VexLichDungeonPlugin;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import MBRound18.hytale.vexlichdungeon.ui.VexPortalCountdownHud;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.data.PortalPlacementRecord;
import MBRound18.hytale.vexlichdungeon.portal.PortalSnapshotUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VexChallengeCommand extends AbstractCommand {
  private static final String PREFAB_PATH = "Base/Vex_Portal_Entrance";
  private static final int DEFAULT_COUNTDOWN_SECONDS = 20;
  private static final int MIN_COUNTDOWN_SECONDS = 1;
  private static final int MAX_COUNTDOWN_SECONDS = 300;
  private static final int MIN_PORTAL_DISTANCE = 50;
  private static final long POLL_INTERVAL_MS = 1000L;
  private static final ConcurrentHashMap<UUID, PlayerPoller> ACTIVE_POLLERS = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<UUID, PortalInstance> ACTIVE_PORTALS = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<UUID, ScheduledFuture<?>> ACTIVE_CLEANUPS = new ConcurrentHashMap<>();
  private static final ScheduledExecutorService PORTAL_CLEANUP_SCHEDULER = Executors.newScheduledThreadPool(1,
      Thread.ofVirtual().name("vex-portal-cleanup-", 0).factory());
  private static final LoggingHelper LOG = new LoggingHelper("VexChallengeCommand");

  public VexChallengeCommand() {
    super("challenge", "Spawn the Vex portal prefab and show countdown");
    setAllowsExtraArguments(true);
  }

  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    PlayerRef playerRef = requirePlayer(context);
    if (playerRef == null) {
      return CompletableFuture.completedFuture(null);
    }

    PrefabSpawner prefabSpawner = resolvePrefabSpawner(context);
    if (prefabSpawner == null) {
      return CompletableFuture.completedFuture(null);
    }

    int countdownSeconds = parseCountdownSeconds(context.getInputString());

    prefabSpawner.loadPrefab(PREFAB_PATH).whenComplete((prefab, error) -> {
      if (error != null || prefab == null) {
        context.sendMessage(Message.raw("Failed to load portal prefab: " +
            (error == null ? "unknown error" : error.getMessage())));
        return;
      }
      UiThread.runOnPlayerWorld(playerRef, () -> spawnPortalAndStartCountdown(
          context,
          playerRef,
          prefabSpawner,
          Objects.requireNonNull(prefab, "prefab"),
          countdownSeconds));
    });

    return CompletableFuture.completedFuture(null);
  }

  public static void stopPortalCountdown(@Nullable UUID playerId) {
    if (playerId == null) {
      return;
    }
    ScheduledFuture<?> cleanup = ACTIVE_CLEANUPS.remove(playerId);
    if (cleanup != null) {
      cleanup.cancel(false);
    }
    PlayerPoller existing = ACTIVE_POLLERS.remove(playerId);
    if (existing != null) {
      existing.stop();
    }
    clearHud(playerId);
    removePortal(playerId);
  }

  public static void forceRemovePortal(@Nullable UUID playerId) {
    if (playerId == null) {
      return;
    }
    ScheduledFuture<?> cleanup = ACTIVE_CLEANUPS.remove(playerId);
    if (cleanup != null) {
      cleanup.cancel(false);
    }
    PlayerPoller existing = ACTIVE_POLLERS.remove(playerId);
    if (existing != null) {
      existing.stop();
    }
    clearHud(playerId);
    removePortal(playerId);
  }

  public static void clearCountdownHud(@Nullable UUID playerId) {
    if (playerId == null) {
      return;
    }
    clearCountdownUi(playerId);
  }

  @Nullable
  private static PlayerRef requirePlayer(@Nonnull CommandContext context) {
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("This command can only be used by players."));
      return null;
    }
    UUID uuid = context.sender().getUuid();
    if (uuid == null) {
      context.sendMessage(Message.raw("Unable to resolve player."));
      return null;
    }
    PlayerRef playerRef = Universe.get().getPlayer(uuid);
    if (playerRef == null || !playerRef.isValid()) {
      context.sendMessage(Message.raw("Unable to resolve player."));
      return null;
    }
    return playerRef;
  }

  @Nullable
  private static PrefabSpawner resolvePrefabSpawner(@Nonnull CommandContext context) {
    VexLichDungeonPlugin plugin = VexLichDungeonPlugin.getInstance();
    if (plugin == null) {
      context.sendMessage(Message.raw("VexLichDungeon plugin is not ready."));
      return null;
    }
    PrefabSpawner spawner = plugin.getPrefabSpawner();
    if (spawner == null) {
      context.sendMessage(Message.raw("Prefab system is not initialized yet."));
      return null;
    }
    return spawner;
  }

  private static int parseCountdownSeconds(@Nullable String input) {
    if (input == null || input.isBlank()) {
      return DEFAULT_COUNTDOWN_SECONDS;
    }
    String[] tokens = input.trim().split("\\s+");
    if (tokens.length == 0) {
      return DEFAULT_COUNTDOWN_SECONDS;
    }
    String tail = tokens[tokens.length - 1];
    try {
      int value = Integer.parseInt(tail);
      return clampSeconds(value);
    } catch (NumberFormatException ignored) {
      return DEFAULT_COUNTDOWN_SECONDS;
    }
  }

  private static int clampSeconds(int value) {
    if (value < MIN_COUNTDOWN_SECONDS) {
      return MIN_COUNTDOWN_SECONDS;
    }
    if (value > MAX_COUNTDOWN_SECONDS) {
      return MAX_COUNTDOWN_SECONDS;
    }
    return value;
  }

  private static void spawnPortalAndStartCountdown(
      @Nonnull CommandContext context,
      @Nonnull PlayerRef playerRef,
      @Nonnull PrefabSpawner prefabSpawner,
      @Nonnull BlockSelection prefab,
      int countdownSeconds) {
    if (!playerRef.isValid()) {
      return;
    }
    UUID playerId = playerRef.getUuid();
    if (ACTIVE_PORTALS.containsKey(playerId)) {
      context.sendMessage(Message.raw("A portal is already active for you."));
      return;
    }

    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      context.sendMessage(Message.raw("Failed to resolve player entity."));
      return;
    }

    Store<EntityStore> store = ref.getStore();
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      context.sendMessage(Message.raw("Failed to resolve player entity."));
      return;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      context.sendMessage(Message.raw("Failed to resolve player position."));
      return;
    }

    Vector3d pos = transform.getPosition();
    int spawnY = (int) Math.floor(pos.y);
    int spawnX = (int) Math.floor(pos.x + 0.5d);
    int spawnZ = (int) Math.floor(pos.z + 0.5d);

    Vector3f rotation = transform.getRotation();
    if (rotation != null) {
      double yawRad = Math.toRadians(rotation.y);
      double pitchRad = Math.toRadians(rotation.x);
      double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad);
      double forwardZ = Math.cos(yawRad) * Math.cos(pitchRad);
      double targetX = pos.x + (forwardX * 5.0d);
      double targetZ = pos.z + (forwardZ * 5.0d);
      spawnX = (int) Math.floor(targetX + 0.5d);
      spawnZ = (int) Math.floor(targetZ + 0.5d);
    }

    Vector3i selectionMin;
    Vector3i selectionMax;
    int anchorX;
    int anchorY;
    int anchorZ;
    if (prefab.hasSelectionBounds()) {
      selectionMin = prefab.getSelectionMin();
      selectionMax = prefab.getSelectionMax();
      anchorX = prefab.getAnchorX();
      anchorY = prefab.getAnchorY();
      anchorZ = prefab.getAnchorZ();
    } else {
      var dims = prefabSpawner.getPrefabDimensions(PREFAB_PATH);
      selectionMin = new Vector3i(dims.minX, dims.minY, dims.minZ);
      selectionMax = new Vector3i(dims.maxX, dims.maxY, dims.maxZ);
      anchorX = 0;
      anchorY = 0;
      anchorZ = 0;
    }
    int placeY = spawnY - 1 - (selectionMin.y - anchorY);

    UUID worldUuid = playerRef.getWorldUuid();
    if (worldUuid == null) {
      context.sendMessage(Message.raw("Failed to resolve world."));
      return;
    }
    World world = Universe.get().getWorld(worldUuid);
    if (world == null) {
      context.sendMessage(Message.raw("Failed to resolve world."));
      return;
    }

    Vector3i placement = new Vector3i(spawnX, placeY, spawnZ);
    int minX = placement.x + (selectionMin.x - anchorX);
    int maxX = placement.x + (selectionMax.x - anchorX);
    int minY = placement.y + (selectionMin.y - anchorY);
    int maxY = placement.y + (selectionMax.y - anchorY);
    int minZ = placement.z + (selectionMin.z - anchorZ);
    int maxZ = placement.z + (selectionMax.z - anchorZ);
    LOG.info("[PORTAL] placement=(%d,%d,%d) bounds=[%d,%d,%d -> %d,%d,%d] anchor=(%d,%d,%d) sel=[%d,%d,%d -> %d,%d,%d]",
        placement.x, placement.y, placement.z,
        minX, minY, minZ, maxX, maxY, maxZ,
        anchorX, anchorY, anchorZ,
        selectionMin.x, selectionMin.y, selectionMin.z,
        selectionMax.x, selectionMax.y, selectionMax.z);
    DataStore dataStore = resolveDataStore();
    if (dataStore != null) {
      Optional<PortalPlacementRecord> nearby = dataStore.findNearbyPortal(worldUuid, spawnX, spawnZ,
          MIN_PORTAL_DISTANCE);
      if (nearby.isPresent()) {
        PortalPlacementRecord existing = nearby.get();
        context.sendMessage(Message.raw(
            "Portal too close to existing portal at X: " + existing.getX() +
                " Z: " + existing.getZ() + " (min " + MIN_PORTAL_DISTANCE + " blocks)"));
        return;
      }
    }
    long expiresAt = System.currentTimeMillis() + (Math.max(0, countdownSeconds) * 1000L);
    PortalSnapshotUtil.Snapshot snapshot = PortalSnapshotUtil.captureSnapshot(world, minX, maxX, minY, maxY, minZ,
        maxZ);
    recordPortal(playerRef.getUuid(), worldUuid, world.getName(), placement, minX, maxX, minY, maxY, minZ, maxZ,
        expiresAt,
        dataStore, snapshot);
    PortalSnapshotUtil.clearBounds(world, minX, maxX, minY, maxY, minZ, maxZ);
    prefab.place(
        ConsoleSender.INSTANCE,
        world,
        placement,
        null,
        entityRef -> {
          if (entityRef != null) {
            PrefabEntityUtils.unfreezePrefabNpc(entityRef, LOG.getLogger());
          }
        });

    String locationText = Objects.requireNonNull(formatLocation(spawnX, spawnY, spawnZ), "locationText");
    startCountdown(playerRef, countdownSeconds, locationText);
    context.sendMessage(Message.raw("Spawned portal and started countdown (" + countdownSeconds + "s)."));
  }

  private static void startCountdown(@Nonnull PlayerRef playerRef, int totalSeconds,
      @Nonnull String locationText) {
    int startValue = Math.max(0, totalSeconds);
    AtomicInteger remaining = new AtomicInteger(startValue);
    VexPortalCountdownHud.update(playerRef,
        Objects.requireNonNull(AbstractCustomUIHud.formatTime(startValue), "time"),
        locationText);

    PlayerPoller poller = new PlayerPoller();
    poller.start(playerRef, POLL_INTERVAL_MS, () -> {
      if (!playerRef.isValid()) {
        stopPortalCountdown(playerRef.getUuid());
        return;
      }
      PortalInstance activePortal = ACTIVE_PORTALS.get(playerRef.getUuid());
      if (activePortal != null) {
        UUID currentWorld = playerRef.getWorldUuid();
        if (currentWorld == null || !currentWorld.equals(activePortal.worldUuid())) {
          stopPortalCountdown(playerRef.getUuid());
          return;
        }
      }
      int value = remaining.decrementAndGet();
      if (value < 0) {
        stopPortalCountdown(playerRef.getUuid());
        return;
      }
      VexPortalCountdownHud.update(playerRef,
          Objects.requireNonNull(AbstractCustomUIHud.formatTime(value), "time"),
          locationText);
      if (value == 0) {
        stopPortalCountdown(playerRef.getUuid());
      }
    });

    ACTIVE_POLLERS.put(playerRef.getUuid(), poller);
    schedulePortalCleanup(playerRef.getUuid(), startValue);
  }

  private static void schedulePortalCleanup(@Nonnull UUID playerId, int totalSeconds) {
    ScheduledFuture<?> existing = ACTIVE_CLEANUPS.remove(playerId);
    if (existing != null) {
      existing.cancel(false);
    }
    long delayMs = Math.max(0, totalSeconds) * 1000L;
    ScheduledFuture<?> future = PORTAL_CLEANUP_SCHEDULER.schedule(
        () -> stopPortalCountdown(playerId), delayMs, TimeUnit.MILLISECONDS);
    ACTIVE_CLEANUPS.put(playerId, future);
  }

  private static String formatLocation(int x, int y, int z) {
    return "X: " + x + "  Y: " + y + "  Z: " + z;
  }

  private static void clearHud(@Nonnull UUID playerId) {
    PlayerRef playerRef = Universe.get().getPlayer(playerId);
    if (playerRef == null || !playerRef.isValid()) {
      return;
    }
    VexPortalCountdownHud.clear(playerRef);
  }

  private static void clearCountdownUi(@Nonnull UUID playerId) {
    PlayerPoller existing = ACTIVE_POLLERS.remove(playerId);
    if (existing != null) {
      existing.stop();
    }
    clearHud(playerId);
  }

  private static void recordPortal(@Nonnull UUID playerId, @Nonnull UUID worldUuid,
      @Nonnull String worldName, @Nonnull Vector3i placement,
      int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
      long expiresAt, @Nullable DataStore dataStore, @Nonnull PortalSnapshotUtil.Snapshot snapshot) {
    UUID portalId = Objects.requireNonNull(UUID.randomUUID(), "portalId");
    ACTIVE_PORTALS.put(playerId, new PortalInstance(portalId, worldUuid, minX, maxX, minY, maxY, minZ, maxZ));
    if (dataStore != null) {
      PortalPlacementRecord record = new PortalPlacementRecord(portalId, worldUuid,
          Objects.requireNonNull(worldName, "worldName"), placement.x, placement.y, placement.z,
          minX, maxX, minY, maxY, minZ, maxZ, System.currentTimeMillis(), expiresAt);
      record.setSizeX(snapshot.getSizeX());
      record.setSizeY(snapshot.getSizeY());
      record.setSizeZ(snapshot.getSizeZ());
      record.setSnapshotBlocks(snapshot.getBlocks());
      dataStore.recordPortalPlacement(record);
    }
  }

  private static void removePortal(@Nonnull UUID playerId) {
    PortalInstance portal = ACTIVE_PORTALS.remove(playerId);
    DataStore dataStore = resolveDataStore();
    PortalPlacementRecord record = null;
    if (dataStore != null) {
      if (portal != null && portal.portalId() != null) {
        record = dataStore.getPortalPlacement(portal.portalId()).orElse(null);
        dataStore.removePortalPlacement(portal.portalId());
      }
    }
    if (portal == null) {
      return;
    }
    World world = Universe.get().getWorld(portal.worldUuid());
    if (world == null) {
      return;
    }
    PortalPlacementRecord finalRecord = record;
    world.execute(() -> {
      if (finalRecord != null) {
        PortalSnapshotUtil.restore(world, finalRecord);
      } else {
        PortalSnapshotUtil.clearBounds(world, portal.minX(), portal.maxX(), portal.minY(), portal.maxY(),
            portal.minZ(), portal.maxZ());
      }
    });
  }

  public static void cleanupPersistedPortals(@Nonnull DataStore dataStore) {
    for (PortalPlacementRecord record : new java.util.ArrayList<>(dataStore.getPortalPlacements())) {
      if (record == null) {
        continue;
      }
      UUID portalId = record.getPortalId();
      if (portalId == null) {
        continue;
      }
      World world = resolveWorld(record);
      if (world != null) {
        world.execute(() -> PortalSnapshotUtil.restore(world, record));
      } else {
        LOG.warn("[PORTAL] World not loaded for persisted portal %s", portalId);
      }
      dataStore.removePortalPlacement(portalId);
    }
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

  // Snapshot restore handled by PortalSnapshotUtil

  @Nullable
  private static DataStore resolveDataStore() {
    VexLichDungeonPlugin plugin = VexLichDungeonPlugin.getInstance();
    if (plugin == null) {
      return null;
    }
    return plugin.getDataStore();
  }

  private record PortalInstance(
      @Nonnull UUID portalId,
      @Nonnull UUID worldUuid,
      int minX,
      int maxX,
      int minY,
      int maxY,
      int minZ,
      int maxZ) {
  }
}
