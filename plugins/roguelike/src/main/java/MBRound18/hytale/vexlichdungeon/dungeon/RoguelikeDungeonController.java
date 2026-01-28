package MBRound18.hytale.vexlichdungeon.dungeon;

import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.data.SpawnPool;
import MBRound18.hytale.vexlichdungeon.data.SpawnPoolEntry;
import MBRound18.hytale.vexlichdungeon.engine.PortalEngineAdapter;
import MBRound18.PortalEngine.api.logging.InternalLogger;
import MBRound18.PortalEngine.api.logging.EngineLog;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import MBRound18.PortalEngine.api.i18n.EngineLang;
import MBRound18.PortalEngine.api.portal.PortalPlacementRegistry;
import MBRound18.hytale.vexlichdungeon.ui.HudController;
import MBRound18.hytale.vexlichdungeon.ui.VexHudSequenceSupport;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Rogue-like dungeon controller that generates rooms on-demand.
 */
@SuppressWarnings({ "removal", "unused" })
public class RoguelikeDungeonController {

  private static final String TREASURE_PREFAB = "Base/Vex_Courtyard_Base";
  private static final String RETURN_PORTAL_BLOCK_ID = "Vex_Dungeon_Challenge_Return";
  private static final int ROOM_Y_OFFSET = 14;
  private static final int RETURN_PORTAL_LOCAL_Y = 2;

  private final EngineLog log;
  private final DungeonGenerator generator;
  private final PrefabSpawner prefabSpawner;
  private final PrefabSelector selector;
  private final DataStore dataStore;
  private final PortalEngineAdapter engineAdapter;
  private final EnemySpawnPlanner spawnPlanner;
  private final InternalLogger eventsLogger;
  private final Map<String, RoguelikeWorldState> worldStates = new HashMap<>();

  public RoguelikeDungeonController(
      @Nonnull EngineLog log,
      @Nonnull DungeonGenerator generator,
      @Nonnull PrefabDiscovery discovery,
      @Nonnull PrefabSpawner prefabSpawner,
      @Nonnull DataStore dataStore,
      @Nonnull PortalEngineAdapter engineAdapter,
      @Nonnull InternalLogger eventsLogger) {
    this.log = log;
    this.generator = generator;
    this.selector = new PrefabSelector(generator.getConfig().getSeed(), discovery);
    this.prefabSpawner = prefabSpawner;
    this.dataStore = dataStore;
    this.engineAdapter = engineAdapter;
    this.spawnPlanner = new EnemySpawnPlanner(generator.getConfig().getSeed());
    this.eventsLogger = eventsLogger;
  }

  public void initializeWorld(@Nonnull World world) {
    String worldName = world.getName();
    if (worldStates.containsKey(worldName)) {
      return;
    }

    RoguelikeWorldState state = new RoguelikeWorldState();
    worldStates.put(worldName, state);

    dataStore.getInstance(worldName).ifPresent(data -> {
      state.totalScore = data.getTotalScore();
      state.totalKills = data.getTotalKills();
      state.roomsCleared = data.getRoomsCleared();
      state.roomsClearedThisRound = data.getRoomsClearedThisRound();
      state.roundsCleared = data.getRoundsCleared();
      state.safeRoomsVisited = data.getSafeRoomsVisited();
      state.safeRoomPending = data.getRoomsClearedThisRound() >= getRoomsRequiredForSafeRoom(state);
    });

    int spawnX = generator.getSpawnCenterX();
    int spawnY = generator.getSpawnCenterY();
    int spawnZ = generator.getSpawnCenterZ();

    DungeonTile baseTile = new DungeonTile(0, 0, selector.getBasePrefab(), 0, DungeonTile.TileType.BASE);
    state.rooms.put(new RoomKey(0, 0), baseTile);

    if (!generator.isSkipBaseTile()) {
      prefabSpawner.spawnTile(baseTile, world, spawnX, spawnY, spawnZ, false);
    }

    // Pre-generate 4 adjacent rooms.
    for (CardinalDirection direction : CardinalDirection.all()) {
      RoomKey neighborKey = new RoomKey(direction.getOffsetX(), direction.getOffsetZ());
      if (!state.rooms.containsKey(neighborKey)) {
        DungeonTile room = createRoomTile(direction.getOffsetX(), direction.getOffsetZ(), state);
        state.rooms.put(neighborKey, room);
        int[] worldPos = gridToWorld(direction.getOffsetX(), direction.getOffsetZ());
        prefabSpawner.spawnTile(room, world, worldPos[0], spawnY, worldPos[1], false);
      }
    }

    log.info("[ROGUELIKE] Initialized world %s with base and 4 adjacent rooms", worldName);
  }

  public void initializePlayer(@Nonnull World world,
      @Nonnull com.hypixel.hytale.server.core.entity.entities.Player player,
      boolean showWelcome) {
    RoguelikeWorldState state = worldStates.get(world.getName());
    if (state == null) {
      return;
    }
    UUID uuid = player.getUuid();
    state.playerScores.putIfAbsent(uuid, 0);
    state.playerNames.put(uuid, player.getDisplayName());
    if (showWelcome) {
      VexHudSequenceSupport.showWelcomeThenScore(player.getPlayerRef(), state.totalScore,
          state.playerScores.getOrDefault(uuid, 0), 0);
    } else {
      HudController.openScoreHud(player.getPlayerRef(), state.totalScore,
          state.playerScores.getOrDefault(uuid, 0), 0);
    }
  }

  public void pollWorld(@Nonnull World world) {
    String worldName = world.getName();
    RoguelikeWorldState state = worldStates.get(worldName);
    if (state == null) {
      return;
    }

    Map<String, String> currentPlayers = new HashMap<>();
    for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
      String uuid = player.getUuid().toString();
      String name = player.getDisplayName();
      currentPlayers.put(uuid, name);
    }
    dataStore.updateCurrentPlayers(worldName, currentPlayers);

    int maxPlayers = dataStore.getConfig().getMaxPlayersPerInstance();
    if (maxPlayers > 0 && currentPlayers.size() >= maxPlayers) {
      PortalPlacementRegistry.closePortals("Vex_The_Lich_Dungeon");
    }

    for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
      com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform = player
          .getTransformComponent();
      if (transform == null) {
        continue;
      }
      Vector3d pos = transform.getPosition();
      RoomKey key = toGridKey(pos);
      RoomKey previous = state.playerRooms.put(player, key);
      if (!key.equals(previous)) {
        onEnterRoom(world, state, key, previous);
      }
    }
  }

  private void onEnterRoom(@Nonnull World world, @Nonnull RoguelikeWorldState state, @Nonnull RoomKey key,
      RoomKey previous) {
    DungeonTile room = state.rooms.get(key);
    if (room == null) {
      room = createRoomTile(key.x, key.z, state);
      state.rooms.put(key, room);
      int[] worldPos = gridToWorld(key.x, key.z);
      prefabSpawner.spawnTile(room, world, worldPos[0], generator.getSpawnCenterY(), worldPos[1], false);
      log.info("[ROGUELIKE] Spawned room at grid (%d, %d)", key.x, key.z);
    }

    RoomState roomState = state.roomStates.computeIfAbsent(key, k -> new RoomState());
    if (roomState.activated) {
      return;
    }

    if (room.getType() == DungeonTile.TileType.BASE || room.getType() == DungeonTile.TileType.BASE_CORNER) {
      roomState.activated = true;
      generateAdjacentRooms(world, state, key, previous);
      if (eventsLogger != null) {
        eventsLogger.info("Activated base room at grid (" + key.x + ", " + key.z + ")");
      }
      maybeRemoveReturnPortal(world, state, previous);
      return;
    }

    roomState.activated = true;
    roomState.entryFrom = previous;
    if (isSafeRoom(room) && !roomState.safeCounted) {
      roomState.safeCounted = true;
      state.safeRoomsVisited++;
      state.roundsCleared++;
      state.roomsClearedThisRound = 0;
      state.safeRoomPending = false;
      engineAdapter.onRoundCleared(world.getName());
      engineAdapter.onSafeRoomVisited(world.getName());
      spawnReturnPortal(world, state, key);
      if (eventsLogger != null) {
        eventsLogger.info("Safe room visited. Total safe rooms: " + state.safeRoomsVisited);
      }
    }
    if (!roomState.counted) {
      roomState.counted = true;
      state.roomsEntered++;
      if (eventsLogger != null) {
        eventsLogger.info("Rooms entered: " + state.roomsEntered);
      }
    }
    log.info("[ROGUELIKE] Activated room at grid (%d, %d)", key.x, key.z);
    if (eventsLogger != null) {
      eventsLogger.info("Activated room at grid (" + key.x + ", " + key.z + ")");
    }
    generateAdjacentRooms(world, state, key, previous);
    spawnEnemiesForRoom(world, state, key);
    maybeRemoveReturnPortal(world, state, previous);
  }

  private void generateAdjacentRooms(@Nonnull World world, @Nonnull RoguelikeWorldState state, @Nonnull RoomKey key,
      RoomKey previous) {
    for (CardinalDirection direction : CardinalDirection.all()) {
      RoomKey neighborKey = new RoomKey(key.x + direction.getOffsetX(), key.z + direction.getOffsetZ());
      if (previous != null && neighborKey.equals(previous)) {
        continue; // keep the room they came from unchanged
      }

      if (!state.rooms.containsKey(neighborKey)) {
        DungeonTile room = createRoomTile(neighborKey.x, neighborKey.z, state);
        state.rooms.put(neighborKey, room);
        int[] worldPos = gridToWorld(neighborKey.x, neighborKey.z);
        prefabSpawner.spawnTile(room, world, worldPos[0], generator.getSpawnCenterY(), worldPos[1], false);
      }
    }
  }

  private DungeonTile createRoomTile(int gridX, int gridZ, RoguelikeWorldState state) {
    boolean spawnSafeRoom = state.safeRoomPending;
    if (spawnSafeRoom) {
      state.safeRoomPending = false;
    }
    String prefabPath = spawnSafeRoom ? TREASURE_PREFAB : selector.selectRandomRoom();
    if (prefabPath == null) {
      prefabPath = TREASURE_PREFAB;
    }
    DungeonTile tile = new DungeonTile(gridX, gridZ, prefabPath, selector.selectRandomRotation(),
        DungeonTile.TileType.ROOM);
    return tile;
  }

  private void spawnEnemiesForRoom(@Nonnull World world, @Nonnull RoguelikeWorldState state, @Nonnull RoomKey key) {
    RoomState roomState = state.roomStates.computeIfAbsent(key, k -> new RoomState());
    if (roomState.cleared || roomState.enemiesRemaining > 0) {
      return;
    }

    int scoreBudget = Math.max(10, state.totalScore);
    SpawnPool pool = dataStore.getSpawnPool();
    java.util.List<SpawnPoolEntry> enemyPlan = spawnPlanner.planEnemies(pool, scoreBudget);

    if (enemyPlan.isEmpty()) {
      roomState.cleared = true;
      return;
    }

    int[] worldPos = gridToWorld(key.x, key.z);
    int baseX = worldPos[0] + 4;
    int baseZ = worldPos[1] + 4;
    int y = generator.getSpawnCenterY() + 1;

    int spawned = 0;
    for (int i = 0; i < enemyPlan.size(); i++) {
      SpawnPoolEntry enemy = enemyPlan.get(i);
      int offsetX = (i % 3) * 3;
      int offsetZ = (i / 3) * 3;
      Vector3d spawnPos = new Vector3d(baseX + offsetX, y, baseZ + offsetZ);
      if (spawnEnemy(world, state, key, enemy, spawnPos)) {
        spawned++;
      }
    }

    roomState.enemiesRemaining = spawned;
    log.info("[ROGUELIKE] Spawned %d enemies in room (%d, %d)", spawned, key.x, key.z);
    if (eventsLogger != null) {
      eventsLogger.info("Spawned " + spawned + " enemies in room (" + key.x + ", " + key.z + ")");
    }
  }

  private boolean spawnEnemy(@Nonnull World world, @Nonnull RoguelikeWorldState state, @Nonnull RoomKey key,
      @Nonnull SpawnPoolEntry enemy, @Nonnull Vector3d position) {
    try {
      String roleName = enemy.getEnemy();
      String modelId = enemy.getEnemy();
      NPCPlugin npcPlugin = NPCPlugin.get();
      if (!npcPlugin.hasRoleName(roleName)) {
        log.warn("[ROGUELIKE] Unknown NPC role: %s", roleName);
        return false;
      }

      Pair<com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>, INonPlayerCharacter> pair = npcPlugin
          .spawnNPC(
              world.getEntityStore().getStore(),
              roleName,
              modelId,
              position,
              new Vector3f(0.0f, 0.0f, 0.0f));

      if (pair != null && pair.right() != null) {
        INonPlayerCharacter npc = pair.right();
        UUID uuid = null;
        if (npc instanceof NPCEntity) {
          uuid = ((NPCEntity) npc).getUuid();
        } else if (npc instanceof com.hypixel.hytale.server.core.entity.Entity) {
          uuid = ((com.hypixel.hytale.server.core.entity.Entity) npc).getUuid();
        }

        if (uuid != null) {
          state.enemyPoints.put(uuid, Math.max(1, enemy.getPoints()));
          state.enemyRooms.put(uuid, key);
          log.info("[ROGUELIKE] Spawned NPC %s (%s) at (%.1f, %.1f, %.1f)", roleName, uuid,
              position.x, position.y, position.z);
          if (eventsLogger != null) {
            eventsLogger.info("Spawned NPC " + roleName + " (" + uuid + ") at ("
                + position.x + ", " + position.y + ", " + position.z + ")");
          }
          return true;
        }
      }
    } catch (Exception e) {
      log.warn("[ROGUELIKE] Failed to spawn NPC %s: %s", enemy.getEnemy(), e.getMessage());
      if (eventsLogger != null) {
        eventsLogger.warn("Failed to spawn NPC " + enemy.getEnemy() + ": " + e.getMessage());
      }
    }
    return false;
  }

  public void recordEnemyKilled(@Nonnull World world, @Nonnull RoomKey key, int points, UUID killerUuid) {
    RoguelikeWorldState state = worldStates.get(world.getName());
    if (state == null) {
      return;
    }

    RoomState roomState = state.roomStates.get(key);
    if (roomState == null || roomState.cleared) {
      return;
    }

    roomState.enemiesRemaining = Math.max(0, roomState.enemiesRemaining - 1);
    state.totalScore += Math.max(0, points);
    state.totalKills += 1;
    if (killerUuid != null) {
      state.playerScores.merge(killerUuid, Math.max(0, points), Integer::sum);
    }
    if (killerUuid != null) {
      engineAdapter.onKill(world.getName(), killerUuid.toString(), "Enemy", points);
    }
    updateScoreHud(world, state, killerUuid, points);

    if (roomState.enemiesRemaining == 0) {
      roomState.cleared = true;
      state.roomsCleared++;
      state.roomsClearedThisRound++;
      engineAdapter.onRoomCleared(world.getName());
      if (state.roomsClearedThisRound >= getRoomsRequiredForSafeRoom(state)) {
        state.safeRoomPending = true;
      }
      log.info("[ROGUELIKE] Room (%d, %d) cleared", key.x, key.z);
      if (eventsLogger != null) {
        eventsLogger.info("Room (" + key.x + ", " + key.z + ") cleared");
      }
    }
  }

  public void handleEntityRemoved(@Nonnull com.hypixel.hytale.server.core.entity.Entity entity) {
    World world = entity.getWorld();
    if (world == null) {
      return;
    }

    RoguelikeWorldState state = worldStates.get(world.getName());
    if (state == null) {
      return;
    }

    UUID uuid = entity.getUuid();
    Integer points = state.enemyPoints.remove(uuid);
    RoomKey key = state.enemyRooms.remove(uuid);
    if (points != null && key != null) {
      UUID killerUuid = findNearestPlayerInRoom(world, state, key, entity);
      recordEnemyKilled(world, key, points, killerUuid);
    }
  }

  public void removeWorldState(@Nonnull String worldName) {
    worldStates.remove(worldName);
  }

  private boolean isSafeRoom(@Nonnull DungeonTile room) {
    return room.getType() == DungeonTile.TileType.ROOM && TREASURE_PREFAB.equals(room.getPrefabPath());
  }

  public void showExitSummary(@Nonnull PlayerRef playerRef, @Nonnull World world) {
    RoguelikeWorldState state = worldStates.get(world.getName());
    if (state == null) {
      return;
    }
    int totalScore = state.totalScore;
    int totalKills = state.totalKills;
    int roomsCleared = state.roomsCleared;
    int roundsCleared = state.roundsCleared;
    int safeRooms = state.safeRoomsVisited;

    String statsLine = EngineLang.t(
        "customUI.vexSummary.statsLine",
        totalScore,
        totalKills,
        roomsCleared,
        roundsCleared,
        safeRooms);

    StringBuilder leaderboard = new StringBuilder();
    state.playerScores.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .forEach(entry -> {
          String name = state.playerNames.getOrDefault(entry.getKey(), entry.getKey().toString());
          leaderboard.append(EngineLang.t("customUI.vexSummary.leaderboardEntry", name, entry.getValue()))
              .append("\n");
        });

    String leaderboardText = leaderboard.length() == 0
        ? EngineLang.t("customUI.vexSummary.leaderboardEmpty")
        : leaderboard.toString();

    VexHudSequenceSupport.showSummarySequence(playerRef, statsLine, statsLine, leaderboardText);
  }

  private UUID findNearestPlayerInRoom(@Nonnull World world, @Nonnull RoguelikeWorldState state, @Nonnull RoomKey key,
      @Nonnull com.hypixel.hytale.server.core.entity.Entity entity) {
    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform = entity
        .getTransformComponent();
    if (transform == null) {
      return null;
    }
    Vector3d pos = transform.getPosition();
    double bestDistance = Double.MAX_VALUE;
    UUID bestUuid = null;
    for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
      RoomKey playerRoom = state.playerRooms.get(player);
      if (playerRoom == null || !playerRoom.equals(key)) {
        continue;
      }
      com.hypixel.hytale.server.core.modules.entity.component.TransformComponent playerTransform = player
          .getTransformComponent();
      if (playerTransform == null) {
        continue;
      }
      Vector3d playerPos = playerTransform.getPosition();
      double dx = playerPos.x - pos.x;
      double dy = playerPos.y - pos.y;
      double dz = playerPos.z - pos.z;
      double dist = dx * dx + dy * dy + dz * dz;
      if (dist < bestDistance) {
        bestDistance = dist;
        bestUuid = player.getUuid();
      }
    }
    return bestUuid;
  }

  private int[] gridToWorld(int gridX, int gridZ) {
    return generator.gridToWorld(gridX, gridZ);
  }

  private RoomKey toGridKey(Vector3d pos) {
    int gridStep = generator.getConfig().getGridStep();
    int gridX = (int) Math.round((pos.x - generator.getSpawnCenterX()) / (double) gridStep);
    int gridZ = (int) Math.round((pos.z - generator.getSpawnCenterZ()) / (double) gridStep);
    return new RoomKey(gridX, gridZ);
  }

  private void updateScoreHud(@Nonnull World world, @Nonnull RoguelikeWorldState state, UUID killerUuid, int delta) {
    int instanceScore = state.totalScore;
    for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
      UUID uuid = player.getUuid();
      state.playerNames.put(uuid, player.getDisplayName());
      int playerScore = state.playerScores.getOrDefault(uuid, 0);
      int playerDelta = (killerUuid != null && killerUuid.equals(uuid)) ? delta : 0;
      HudController.openScoreHud(player.getPlayerRef(), instanceScore, playerScore, playerDelta);
    }
  }

  private int getRoomsRequiredForSafeRoom(@Nonnull RoguelikeWorldState state) {
    int base = Math.max(1, dataStore.getConfig().getSafeRoomBaseRooms());
    double scale = dataStore.getConfig().getSafeRoomScaleFactor();
    int scaled = (int) Math.round(state.roundsCleared * Math.max(0.0, scale));
    return base + scaled;
  }

  private void spawnReturnPortal(@Nonnull World world, @Nonnull RoguelikeWorldState state, @Nonnull RoomKey key) {
    if (state.returnPortalRoom != null) {
      return;
    }
    int[] worldPos = gridToWorld(key.x, key.z);
    int portalX = worldPos[0];
    int portalZ = worldPos[1];
    int portalY = generator.getSpawnCenterY() + ROOM_Y_OFFSET + RETURN_PORTAL_LOCAL_Y;

    long chunkKey = (((long) (portalX >> 4)) << 32) | (((long) (portalZ >> 4)) & 0xFFFFFFFFL);
    BlockAccessor chunk = world.getChunkIfLoaded(chunkKey);
    if (chunk == null) {
      return;
    }
    BlockType block = BlockType.getAssetMap().getAsset(RETURN_PORTAL_BLOCK_ID);
    if (block == null || block.isUnknown()) {
      log.warn("[ROGUELIKE] Return portal block type not found: %s", RETURN_PORTAL_BLOCK_ID);
      return;
    }
    BlockType placed = block;
    String defaultState = block.getDefaultStateKey();
    if (defaultState != null && !defaultState.isBlank()) {
      BlockType stateBlock = block.getBlockForState(defaultState);
      if (stateBlock != null) {
        placed = stateBlock;
      }
    }
    if (!chunk.setBlock(portalX, portalY, portalZ, placed)) {
      log.warn("[ROGUELIKE] Failed to place return portal at (%d,%d,%d)", portalX, portalY, portalZ);
      return;
    }
    state.returnPortalRoom = key;
    state.returnPortalPos = new Vector3i(portalX, portalY, portalZ);
    log.info("[ROGUELIKE] Return portal spawned at (%d,%d,%d)", portalX, portalY, portalZ);
  }

  private void maybeRemoveReturnPortal(@Nonnull World world, @Nonnull RoguelikeWorldState state, RoomKey previousRoom) {
    if (state.returnPortalRoom == null || previousRoom == null) {
      return;
    }
    if (!state.returnPortalRoom.equals(previousRoom)) {
      return;
    }
    for (RoomKey room : state.playerRooms.values()) {
      if (state.returnPortalRoom.equals(room)) {
        return;
      }
    }
    Vector3i pos = state.returnPortalPos;
    if (pos == null) {
      state.returnPortalRoom = null;
      return;
    }
    long chunkKey = (((long) (pos.x >> 4)) << 32) | (((long) (pos.z >> 4)) & 0xFFFFFFFFL);
    BlockAccessor chunk = world.getChunkIfLoaded(chunkKey);
    if (chunk != null) {
      chunk.setBlock(pos.x, pos.y, pos.z, 0);
    }
    state.returnPortalRoom = null;
    state.returnPortalPos = null;
    log.info("[ROGUELIKE] Return portal removed");
  }

  private static class RoguelikeWorldState {
    private final Map<RoomKey, DungeonTile> rooms = new HashMap<>();
    private final Map<RoomKey, RoomState> roomStates = new HashMap<>();
    private final Map<com.hypixel.hytale.server.core.entity.entities.Player, RoomKey> playerRooms = new HashMap<>();
    private final Map<UUID, Integer> playerScores = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();
    private int roomsEntered = 0;
    private int totalScore = 0;
    private int totalKills = 0;
    private int roomsCleared = 0;
    private int roomsClearedThisRound = 0;
    private int roundsCleared = 0;
    private int safeRoomsVisited = 0;
    private boolean safeRoomPending = false;
    private final Map<UUID, Integer> enemyPoints = new HashMap<>();
    private final Map<UUID, RoomKey> enemyRooms = new HashMap<>();
    private RoomKey returnPortalRoom = null;
    private Vector3i returnPortalPos = null;
  }

  private static class RoomState {
    private boolean activated = false;
    private boolean cleared = false;
    private int enemiesRemaining = 0;
    private RoomKey entryFrom = null;
    private boolean counted = false;
    private boolean safeCounted = false;
  }

  private static class RoomKey {
    private final int x;
    private final int z;

    private RoomKey(int x, int z) {
      this.x = x;
      this.z = z;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RoomKey roomKey = (RoomKey) o;
      return x == roomKey.x && z == roomKey.z;
    }

    @Override
    public int hashCode() {
      return Objects.hash(x, z);
    }
  }
}
