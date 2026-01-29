package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import MBRound18.hytale.vexlichdungeon.ui.UiAssetResolver;
import MBRound18.hytale.vexlichdungeon.ui.HudController;

/**
 * Command to create and join a Vex Lich Dungeon challenge instance.
 * Usage: /vex challenge
 * 
 * This command:
 * 1. Checks if the player has permission (same as /inst command -
 * hytale.instances.create)
 * 2. Executes "/inst Vex_The_Lich_Dungeon" to create a new instance
 * 3. The instance system teleports the player automatically
 * 4. Dungeon generation will automatically trigger via event handlers once
 * instance is created
 */
public class VexChallengeCommand extends AbstractAsyncCommand {

  private static final String WORLD_NAME = "Vex_The_Lich_Dungeon";
  private static final String[] PORTAL_BLOCK_CANDIDATES = new String[] {
      "Vex_Dungeon_Challenge_Enter",
      "Items/Portal/Vex/Vex_Dungeon_Challenge_Enter",
      "Portal_Device",
      "PortalDevice",
      "Portal_Device_Block",
      "Portal_Device_Base",
      "Portal_Device_Off",
      "Portal_Device_On"
  };
  private static final long PORTAL_TTL_MS = 30_000L;
  private static final String PORTAL_PREFAB_PATH = "Base/Vex_Portal_Entrance";
  private static final String PORTAL_PREFAB_FILENAME = "Vex_Portal_Entrance.prefab.json";
  private static final int PORTAL_MAX_SNAPSHOT_ATTEMPTS = 8;
  private static final long PORTAL_SNAPSHOT_RETRY_MS = 150L;
  private static final int PORTAL_MAX_RESTORE_ATTEMPTS = 6;
  private static final long PORTAL_RESTORE_RETRY_MS = 200L;
  private static final ConcurrentHashMap<UUID, ScheduledFuture<?>> PORTAL_COUNTDOWNS = new ConcurrentHashMap<>();
  private static final ScheduledExecutorService PORTAL_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "vex-portal-restore");
    thread.setDaemon(true);
    return thread;
  });

  private final EngineLog log;
  private final DataStore dataStore;

  public VexChallengeCommand(@Nonnull EngineLog log, @Nonnull DataStore dataStore) {
    super("challenge", "Create and join a Vex Lich Dungeon challenge");
    this.log = log;
    this.dataStore = dataStore;
  }

  @Override
  protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
    CommandSender sender = context.sender();

    // Ensure command is executed by a player
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.onlyPlayers")));
      return CompletableFuture.completedFuture(null);
    }

    try {
      log.info("[COMMAND] Player %s executed /vex challenge", sender.getDisplayName());

      PlayerContext playerContext = findPlayerContext(sender.getUuid());
      if (playerContext == null) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.playerMissing")));
        return CompletableFuture.completedFuture(null);
      }

      playerContext.world.execute(() -> {
        PortalPrefabData prefab = loadPortalPrefab(log);
        if (prefab == null) {
          log.warn("[PORTAL] Failed to load portal prefab.");
          context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.placeFailed")));
          return;
        }
        Vector3i origin = computePortalOrigin(playerContext.transform.getPosition(),
            playerContext.transform.getRotation(), prefab);
        if (origin == null) {
          log.warn("[PORTAL] Failed to compute portal origin.");
          context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.placeFailed")));
          return;
        }
        log.info("[PORTAL] Spawning portal prefab at origin (%d,%d,%d)", origin.x, origin.y, origin.z);
        trySpawnPortalWithRetries(playerContext.world, origin, prefab, context, 1);
      });

      log.info("[COMMAND] Completed /vex challenge for player %s", sender.getDisplayName());

    } catch (Exception e) {
      log.error("Failed to execute vex command: %s", e.getMessage());
      e.printStackTrace();
      context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.failed", e.getMessage())));
    }

    return CompletableFuture.completedFuture(null);
  }

  @Nullable
  private PlayerContext findPlayerContext(@Nonnull UUID uuid) {
    PlayerRef playerRef = Universe.get().getPlayer(uuid);
    if (playerRef == null || !playerRef.isValid()) {
      return null;
    }
    World world = Universe.get().getWorld(playerRef.getWorldUuid());
    if (world == null) {
      return null;
    }
    com.hypixel.hytale.math.vector.Transform transform = playerRef.getTransform();
    if (transform == null) {
      return null;
    }
    return new PlayerContext(world, transform);
  }

  private static final class PlayerContext {
    private final World world;
    private final com.hypixel.hytale.math.vector.Transform transform;

    private PlayerContext(World world, com.hypixel.hytale.math.vector.Transform transform) {
      this.world = world;
      this.transform = transform;
    }
  }

  private PortalPrefabData loadPortalPrefab(@Nonnull EngineLog log) {
    Path localPrefab = resolveLocalPrefabPath();
    if (localPrefab != null) {
      return loadPortalPrefabFromFile(localPrefab, log);
    }

    Path assetsZipPath = UiAssetResolver.getAssetsZipPath();
    if (assetsZipPath == null || !Files.exists(assetsZipPath)) {
      log.warn("[PORTAL] Assets zip not available for portal prefab.");
      return null;
    }
    String entryPath = "Server/Prefabs/" + PORTAL_PREFAB_PATH + ".prefab.json";
    Path tempFile = null;
    try (ZipFile zipFile = new ZipFile(assetsZipPath.toFile())) {
      ZipEntry entry = zipFile.getEntry(entryPath);
      if (entry == null) {
        log.warn("[PORTAL] Portal prefab not found in zip: %s", entryPath);
        return null;
      }
      tempFile = Files.createTempFile("vex_portal_", ".json");
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8));
          BufferedWriter writer = new BufferedWriter(
              new OutputStreamWriter(Files.newOutputStream(tempFile), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          writer.write(line);
          writer.write("\n");
        }
      }
      return loadPortalPrefabFromFile(tempFile, log);
    } catch (Exception e) {
      log.warn("[PORTAL] Failed to load portal prefab: %s", e.getMessage());
      return null;
    } finally {
      if (tempFile != null) {
        try {
          Files.deleteIfExists(tempFile);
        } catch (Exception ignored) {
        }
      }
    }
  }

  private Path resolveLocalPrefabPath() {
    Path serverPrefab = Path.of("data/server/Server/prefabs", PORTAL_PREFAB_FILENAME);
    if (Files.exists(serverPrefab)) {
      return serverPrefab;
    }
    Path assetsPrefab = Path.of("plugins/roguelike/assets/Server/Prefabs",
        PORTAL_PREFAB_PATH + ".prefab.json");
    if (Files.exists(assetsPrefab)) {
      return assetsPrefab;
    }
    return null;
  }

  private PortalPrefabData loadPortalPrefabFromFile(@Nonnull Path prefabPath, @Nonnull EngineLog log) {
    try {
      BlockSelection selection = PrefabStore.get().getPrefab(prefabPath);
      if (selection == null) {
        log.warn("[PORTAL] Failed to load portal prefab selection from %s", prefabPath);
        return null;
      }
      JsonObject root = JsonParser.parseReader(Files.newBufferedReader(prefabPath)).getAsJsonObject();
      int anchorX = root.has("anchorX") ? root.get("anchorX").getAsInt() : 0;
      int anchorY = root.has("anchorY") ? root.get("anchorY").getAsInt() : 0;
      int anchorZ = root.has("anchorZ") ? root.get("anchorZ").getAsInt() : 0;
      JsonArray blocks = root.getAsJsonArray("blocks");
      List<PrefabBlock> prefabBlocks = new ArrayList<>();
      if (blocks != null) {
        for (JsonElement element : blocks) {
          JsonObject block = element.getAsJsonObject();
          int x = block.get("x").getAsInt();
          int y = block.get("y").getAsInt();
          int z = block.get("z").getAsInt();
          String name = block.has("name") ? block.get("name").getAsString() : "";
          prefabBlocks.add(new PrefabBlock(x, y, z, name));
        }
      }
      log.info("[PORTAL] Loaded portal prefab %s (blocks=%d)", prefabPath.getFileName(), prefabBlocks.size());
      return new PortalPrefabData(selection, prefabBlocks, anchorX, anchorY, anchorZ);
    } catch (Exception e) {
      log.warn("[PORTAL] Failed to read portal prefab from %s: %s", prefabPath, e.getMessage());
      return null;
    }
  }

  @Nullable
  private Vector3i computePortalOrigin(@Nonnull Vector3d position, @Nonnull Vector3f rotation,
      @Nonnull PortalPrefabData prefab) {
    double yawRad = Math.toRadians(rotation.y);
    double forwardX = -Math.sin(yawRad);
    double forwardZ = Math.cos(yawRad);
    int targetX = (int) Math.floor(position.x + forwardX * 5.0);
    int targetZ = (int) Math.floor(position.z + forwardZ * 5.0);
    int targetY = (int) Math.floor(position.y) - 1;
    int originX = targetX - prefab.anchorX;
    int originY = targetY - prefab.anchorY;
    int originZ = targetZ - prefab.anchorZ;
    return new Vector3i(originX, originY, originZ);
  }

  private List<BlockSnapshot> snapshotAndClear(@Nonnull World world, @Nonnull Vector3i origin,
      @Nonnull PortalPrefabData prefab) {
    List<BlockSnapshot> snapshots = new ArrayList<>();
    int missingChunks = 0;
    for (PrefabBlock block : prefab.blocks) {
      int worldX = origin.x + block.x;
      int worldZ = origin.z + block.z;
      if (getChunkIfLoaded(world, worldX, worldZ) == null) {
        missingChunks++;
      }
    }
    if (missingChunks > 0) {
      log.warn("[PORTAL] Missing chunks while snapshotting portal area: %d", missingChunks);
      return List.of();
    }
    for (PrefabBlock block : prefab.blocks) {
      int worldX = origin.x + block.x;
      int worldY = origin.y + block.y;
      int worldZ = origin.z + block.z;
      BlockAccessor chunk = getChunkIfLoaded(world, worldX, worldZ);
      if (chunk == null) {
        continue;
      }
      int id = chunk.getBlock(worldX, worldY, worldZ);
      snapshots.add(new BlockSnapshot(worldX, worldY, worldZ, id));
      chunk.setBlock(worldX, worldY, worldZ, 0);
    }
    return snapshots;
  }

  private void trySpawnPortalWithRetries(@Nonnull World world, @Nonnull Vector3i origin,
      @Nonnull PortalPrefabData prefab, @Nonnull CommandContext context, int attempt) {
    List<BlockSnapshot> snapshots = snapshotAndClear(world, origin, prefab);
    if (snapshots.isEmpty()) {
      if (attempt >= PORTAL_MAX_SNAPSHOT_ATTEMPTS) {
        log.warn("[PORTAL] Failed to snapshot portal area after %d attempts.", attempt);
        context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.placeFailed")));
        return;
      }
      log.warn("[PORTAL] Snapshot attempt %d failed. Requesting chunks...", attempt);
      requestChunksLoaded(world, origin, prefab).whenComplete((ignored, error) -> world.execute(() -> {
        if (error != null) {
          log.warn("[PORTAL] Chunk load attempt %d failed: %s", attempt, error.getMessage());
        }
        log.warn("[PORTAL] Retrying snapshot in %dms.", PORTAL_SNAPSHOT_RETRY_MS);
        PORTAL_SCHEDULER.schedule(() -> world.execute(() ->
            trySpawnPortalWithRetries(world, origin, prefab, context, attempt + 1)),
            PORTAL_SNAPSHOT_RETRY_MS, TimeUnit.MILLISECONDS);
      }));
      return;
    }
    prefab.selection.place(ConsoleSender.INSTANCE, world, origin, null, null);
    if (!forcePlacePortalBlocks(world, origin, prefab)) {
      log.warn("[PORTAL] Forced portal block placement failed; continuing with despawn timer.");
    }
    if (!validatePortalBlocks(world, origin, prefab)) {
      log.warn("[PORTAL] Portal validation failed; continuing with despawn timer.");
    }
    Vector3i portalCenter = computePortalCenter(origin, prefab);
    scheduleRestore(world, snapshots, PORTAL_TTL_MS);
    startPortalCountdown(context.sender().getUuid(), world, portalCenter, PORTAL_TTL_MS);
    context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.portalPlaced")));
    context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.portalHint", WORLD_NAME)));
  }

  private void scheduleRestore(@Nonnull World world, @Nonnull List<BlockSnapshot> snapshots, long delayMs) {
    PORTAL_SCHEDULER.schedule(() -> world.execute(() ->
        tryRestore(world, snapshots, 1)),
        Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
  }

  private void tryRestore(@Nonnull World world, @Nonnull List<BlockSnapshot> snapshots, int attempt) {
    int missing = 0;
    for (BlockSnapshot snapshot : snapshots) {
      BlockAccessor chunk = getChunkIfLoaded(world, snapshot.x, snapshot.z);
      if (chunk == null) {
        missing++;
        continue;
      }
      if (snapshot.blockId == 0 || snapshot.blockId == BlockType.EMPTY_ID) {
        chunk.setBlock(snapshot.x, snapshot.y, snapshot.z, 0);
      } else {
        chunk.setBlock(snapshot.x, snapshot.y, snapshot.z, snapshot.blockId);
      }
    }
    if (missing == 0) {
      log.info("[PORTAL] Restored portal area (attempt %d).", attempt);
      stopPortalCountdowns(world);
      return;
    }
    if (attempt >= PORTAL_MAX_RESTORE_ATTEMPTS) {
      log.warn("[PORTAL] Restore incomplete after %d attempts (missing=%d).", attempt, missing);
      return;
    }
    requestChunksLoaded(world, snapshots).whenComplete((ignored, error) -> world.execute(() -> {
      if (error != null) {
        log.warn("[PORTAL] Restore chunk load failed: %s", error.getMessage());
      }
      PORTAL_SCHEDULER.schedule(() -> world.execute(() ->
          tryRestore(world, snapshots, attempt + 1)),
          PORTAL_RESTORE_RETRY_MS, TimeUnit.MILLISECONDS);
    }));
  }

  private void startPortalCountdown(@Nonnull UUID uuid, @Nonnull World world, @Nonnull Vector3i portalPos,
      long ttlMs) {
    stopPortalCountdown(uuid);
    long expiresAt = System.currentTimeMillis() + Math.max(0L, ttlMs);
    ScheduledFuture<?> future = PORTAL_SCHEDULER.scheduleAtFixedRate(() -> {
      world.execute(() -> {
        long remainingMs = Math.max(0L, expiresAt - System.currentTimeMillis());
        String timerText = formatCountdown(remainingMs);
        String locationText = String.format("X: %d  Y: %d  Z: %d", portalPos.x, portalPos.y, portalPos.z);
        HudController.openPortalCountdown(findPlayerRef(world, uuid), timerText, locationText);
        if (remainingMs <= 0L) {
          stopPortalCountdown(uuid);
        }
      });
    }, 0L, 1L, TimeUnit.SECONDS);
    PORTAL_COUNTDOWNS.put(uuid, future);
  }

  public static void stopPortalCountdown(@Nonnull UUID uuid) {
    ScheduledFuture<?> future = PORTAL_COUNTDOWNS.remove(uuid);
    if (future != null) {
      future.cancel(false);
    }
  }

  private void stopPortalCountdowns(@Nonnull World world) {
    for (PlayerRef playerRef : world.getPlayerRefs()) {
      stopPortalCountdown(playerRef.getUuid());
    }
  }

  private String formatCountdown(long remainingMs) {
    long totalSeconds = Math.max(0L, remainingMs / 1000L);
    long minutes = totalSeconds / 60L;
    long seconds = totalSeconds % 60L;
    return String.format("%02d:%02d", minutes, seconds);
  }

  private Vector3i computePortalCenter(@Nonnull Vector3i origin, @Nonnull PortalPrefabData prefab) {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    int maxZ = Integer.MIN_VALUE;
    boolean found = false;
    for (PrefabBlock block : prefab.blocks) {
      if (block.name == null || block.name.isBlank()) {
        continue;
      }
      boolean isPortal = false;
      for (String candidate : PORTAL_BLOCK_CANDIDATES) {
        if (block.name.equalsIgnoreCase(candidate)) {
          isPortal = true;
          break;
        }
      }
      if (!isPortal) {
        continue;
      }
      int wx = origin.x + block.x;
      int wy = origin.y + block.y;
      int wz = origin.z + block.z;
      minX = Math.min(minX, wx);
      minY = Math.min(minY, wy);
      minZ = Math.min(minZ, wz);
      maxX = Math.max(maxX, wx);
      maxY = Math.max(maxY, wy);
      maxZ = Math.max(maxZ, wz);
      found = true;
    }
    if (!found) {
      return origin;
    }
    return new Vector3i((minX + maxX) / 2, minY, (minZ + maxZ) / 2);
  }

  @Nullable
  private PlayerRef findPlayerRef(@Nonnull World world, @Nonnull UUID uuid) {
    for (PlayerRef playerRef : world.getPlayerRefs()) {
      if (uuid.equals(playerRef.getUuid())) {
        return playerRef;
      }
    }
    return null;
  }

  private CompletableFuture<Void> requestChunksLoaded(@Nonnull World world, @Nonnull Vector3i origin,
      @Nonnull PortalPrefabData prefab) {
    Set<Long> keys = new HashSet<>();
    for (PrefabBlock block : prefab.blocks) {
      int worldX = origin.x + block.x;
      int worldZ = origin.z + block.z;
      long chunkKey = (((long) (worldX >> 4)) << 32) | (((long) (worldZ >> 4)) & 0xFFFFFFFFL);
      keys.add(chunkKey);
    }
    List<CompletableFuture<?>> futures = new ArrayList<>(keys.size());
    for (long key : keys) {
      futures.add(world.getChunkAsync(key));
    }
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
  }

  private CompletableFuture<Void> requestChunksLoaded(@Nonnull World world, @Nonnull List<BlockSnapshot> snapshots) {
    Set<Long> keys = new HashSet<>();
    for (BlockSnapshot snapshot : snapshots) {
      long chunkKey = (((long) (snapshot.x >> 4)) << 32) | (((long) (snapshot.z >> 4)) & 0xFFFFFFFFL);
      keys.add(chunkKey);
    }
    List<CompletableFuture<?>> futures = new ArrayList<>(keys.size());
    for (long key : keys) {
      futures.add(world.getChunkAsync(key));
    }
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
  }

  private boolean validatePortalBlocks(@Nonnull World world, @Nonnull Vector3i origin,
      @Nonnull PortalPrefabData prefab) {
    boolean foundPortal = false;
    boolean placedOk = true;
    for (PrefabBlock block : prefab.blocks) {
      if (block.name == null || block.name.isBlank()) {
        continue;
      }
      boolean isPortal = false;
      for (String candidate : PORTAL_BLOCK_CANDIDATES) {
        if (block.name.equalsIgnoreCase(candidate)) {
          isPortal = true;
          break;
        }
      }
      if (!isPortal) {
        continue;
      }
      foundPortal = true;
      int worldX = origin.x + block.x;
      int worldY = origin.y + block.y;
      int worldZ = origin.z + block.z;
      BlockAccessor chunk = getChunkIfLoaded(world, worldX, worldZ);
      if (chunk == null) {
        log.warn("[PORTAL] Portal prefab chunk not loaded at (%d,%d,%d)", worldX, worldY, worldZ);
        placedOk = false;
        continue;
      }
      int id = chunk.getBlock(worldX, worldY, worldZ);
      BlockType placed = BlockType.getAssetMap().getAsset(id);
      String placedId = placed != null ? placed.getId() : null;
      if (id == 0 || id == BlockType.EMPTY_ID || id == BlockType.UNKNOWN_ID
          || placedId == null || !"Vex_Dungeon_Challenge_Enter".equalsIgnoreCase(placedId)) {
        log.warn("[PORTAL] Portal block missing after placement at (%d,%d,%d) (expected %s)",
            worldX, worldY, worldZ, block.name);
        placedOk = false;
      }
    }
    if (!foundPortal) {
      log.warn("[PORTAL] Portal prefab has no portal blocks to validate.");
    }
    return placedOk;
  }

  private boolean forcePlacePortalBlocks(@Nonnull World world, @Nonnull Vector3i origin,
      @Nonnull PortalPrefabData prefab) {
    BlockType portalBlock = BlockType.getAssetMap().getAsset("Vex_Dungeon_Challenge_Enter");
    if (portalBlock == null || portalBlock.isUnknown()) {
      log.warn("[PORTAL] Portal block type missing for Vex_Dungeon_Challenge_Enter.");
      return false;
    }
    boolean placed = true;
    for (PrefabBlock block : prefab.blocks) {
      if (block.name == null || block.name.isBlank()) {
        continue;
      }
      boolean isPortal = false;
      for (String candidate : PORTAL_BLOCK_CANDIDATES) {
        if (block.name.equalsIgnoreCase(candidate)) {
          isPortal = true;
          break;
        }
      }
      if (!isPortal) {
        continue;
      }
      int worldX = origin.x + block.x;
      int worldY = origin.y + block.y;
      int worldZ = origin.z + block.z;
      BlockAccessor chunk = getChunkIfLoaded(world, worldX, worldZ);
      if (chunk == null) {
        log.warn("[PORTAL] Portal chunk missing at (%d,%d,%d).", worldX, worldY, worldZ);
        placed = false;
        continue;
      }
      if (!chunk.setBlock(worldX, worldY, worldZ, portalBlock)) {
        log.warn("[PORTAL] Failed to set portal block at (%d,%d,%d).", worldX, worldY, worldZ);
        placed = false;
      }
    }
    return placed;
  }

  @Nullable
  private BlockAccessor getChunkIfLoaded(@Nonnull World world, int x, int z) {
    long chunkKey = (((long) (x >> 4)) << 32) | (((long) (z >> 4)) & 0xFFFFFFFFL);
    return world.getChunkIfLoaded(chunkKey);
  }

  private static final class PortalPrefabData {
    private final BlockSelection selection;
    private final List<PrefabBlock> blocks;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;

    private PortalPrefabData(BlockSelection selection, List<PrefabBlock> blocks, int anchorX, int anchorY,
        int anchorZ) {
      this.selection = selection;
      this.blocks = blocks;
      this.anchorX = anchorX;
      this.anchorY = anchorY;
      this.anchorZ = anchorZ;
    }
  }

  private static final class PrefabBlock {
    private final int x;
    private final int y;
    private final int z;
    private final String name;

    private PrefabBlock(int x, int y, int z, String name) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.name = name;
    }
  }

  private static final class BlockSnapshot {
    private final int x;
    private final int y;
    private final int z;
    private final int blockId;

    private BlockSnapshot(int x, int y, int z, int blockId) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.blockId = blockId;
    }
  }
}
