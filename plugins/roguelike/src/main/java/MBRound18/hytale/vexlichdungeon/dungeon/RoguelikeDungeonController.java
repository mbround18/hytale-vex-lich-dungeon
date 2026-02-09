package MBRound18.hytale.vexlichdungeon.dungeon;

import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.data.SpawnPool;
import MBRound18.hytale.vexlichdungeon.data.SpawnPoolEntry;
import MBRound18.hytale.vexlichdungeon.engine.PortalEngineAdapter;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.ImmortalEngine.api.prefab.StitchIndex;
import MBRound18.ImmortalEngine.api.prefab.PrefabInspector;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabEdgeIndex;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import MBRound18.hytale.vexlichdungeon.loot.LootService;
import MBRound18.hytale.vexlichdungeon.events.EntityEliminatedEvent;
import MBRound18.hytale.vexlichdungeon.events.EntitySpawnedEvent;
import MBRound18.hytale.vexlichdungeon.events.InstanceCapacityReachedEvent;
import MBRound18.hytale.vexlichdungeon.events.LootRolledEvent;
import MBRound18.hytale.vexlichdungeon.events.ReturnPortalRemovedEvent;
import MBRound18.hytale.vexlichdungeon.events.ReturnPortalSpawnedEvent;
import MBRound18.hytale.vexlichdungeon.events.RoomClearedEvent;
import MBRound18.hytale.vexlichdungeon.events.RoomCoordinate;
import MBRound18.hytale.vexlichdungeon.events.RoomEnteredEvent;
import MBRound18.hytale.vexlichdungeon.events.RoomGeneratedEvent;
import MBRound18.hytale.vexlichdungeon.events.RoomTileSpawnRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.RoomEnemiesSpawnRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.RoundClearedEvent;
import MBRound18.hytale.vexlichdungeon.events.SafeRoomNeededEvent;
import MBRound18.hytale.vexlichdungeon.events.SafeRoomVisitedEvent;
import MBRound18.hytale.vexlichdungeon.events.WorldEventQueue;
import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import MBRound18.ImmortalEngine.api.portal.PortalPlacementRegistry;
import MBRound18.hytale.vexlichdungeon.ui.VexScoreHud;
import MBRound18.hytale.vexlichdungeon.ui.VexHudSequenceSupport;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import MBRound18.ImmortalEngine.api.participants.ParticipantSnapshot;
import MBRound18.ImmortalEngine.api.participants.ParticipantTracker;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Rogue-like dungeon controller that generates rooms on-demand.
 */
@SuppressWarnings({ "removal", "unused" })
public class RoguelikeDungeonController {

  private static final String RETURN_PORTAL_BLOCK_ID = "Vex_Dungeon_Challenge_Return";
  private static final int ROOM_Y_OFFSET = 14;
  private static final int RETURN_PORTAL_LOCAL_Y = 2;

  private final LoggingHelper log;
  private final DungeonGenerator generator;
  private final PrefabSpawner prefabSpawner;
  private final PrefabSelector selector;
  private final DataStore dataStore;
  private final PortalEngineAdapter engineAdapter;
  private final EnemySpawnPlanner spawnPlanner;
  private final LoggingHelper eventsLogger;
  private StitchIndex stitchIndex;
  private PrefabEdgeIndex edgeIndex;
  private final LootService lootService;
  private final Map<String, RoguelikeWorldState> worldStates = new HashMap<>();

  public RoguelikeDungeonController(
      @Nonnull LoggingHelper log,
      @Nonnull DungeonGenerator generator,
      @Nonnull PrefabDiscovery discovery,
      @Nonnull PrefabSpawner prefabSpawner,
      @Nonnull DataStore dataStore,
      @Nonnull PortalEngineAdapter engineAdapter,
      @Nonnull LoggingHelper eventsLogger,
      StitchIndex stitchIndex,
      LootService lootService) {
    this.log = log;
    this.generator = generator;
    this.selector = new PrefabSelector(generator.getConfig().getSeed(), discovery);
    this.prefabSpawner = prefabSpawner;
    this.dataStore = dataStore;
    this.engineAdapter = engineAdapter;
    this.spawnPlanner = new EnemySpawnPlanner(generator.getConfig().getSeed());
    this.eventsLogger = eventsLogger;
    this.stitchIndex = stitchIndex;
    this.lootService = lootService;
  }

  public void setEdgeIndex(@Nullable PrefabEdgeIndex edgeIndex) {
    this.edgeIndex = edgeIndex;
  }

  public void setStitchIndex(@Nullable StitchIndex stitchIndex) {
    this.stitchIndex = stitchIndex;
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
      state.roundsCleared = data.getRoundsCleared();
      state.safeRoomsVisited = data.getSafeRoomsVisited();
    });

    int spawnX = generator.getSpawnCenterX();
    int spawnY = generator.getSpawnCenterY();
    int spawnZ = generator.getSpawnCenterZ();

    DungeonTile baseTile = new DungeonTile(0, 0, selector.getBasePrefab(), 0, DungeonTile.TileType.BASE);
    state.rooms.put(new RoomKey(0, 0), baseTile);

    if (!generator.isSkipBaseTile()) {
      // Emit event request instead of direct call - RoomTileSpawnRequestHandler will
      // spawn the tile
      WorldEventQueue.get().dispatch(world,
          new RoomTileSpawnRequestedEvent(world, java.util.Objects.requireNonNull(baseTile, "baseTile"),
              spawnX, spawnY, spawnZ));
      emitRoomGenerated(world, baseTile, new RoomKey(0, 0));
    }

    // Pre-generate 4 adjacent rooms.
    for (CardinalDirection direction : CardinalDirection.all()) {
      RoomKey neighborKey = new RoomKey(direction.getOffsetX(), direction.getOffsetZ());
      if (!state.rooms.containsKey(neighborKey)) {
        DungeonTile room = createRoomTile(direction.getOffsetX(), direction.getOffsetZ(), state, baseTile);
        state.rooms.put(neighborKey, room);
        int[] worldPos = gridToWorld(direction.getOffsetX(), direction.getOffsetZ());
        // Emit event request instead of direct call
        WorldEventQueue.get().dispatch(world,
            new RoomTileSpawnRequestedEvent(world, java.util.Objects.requireNonNull(room, "room"),
                worldPos[0], spawnY, worldPos[1]));
        emitRoomGenerated(world, room, neighborKey);
      }
    }

    log.info("[ROGUELIKE] Initialized world %s with base and 4 adjacent rooms", worldName);
  }

  public void initializePlayer(@Nonnull World world,
      @Nonnull PlayerRef playerRef,
      boolean showWelcome) {
    RoguelikeWorldState state = worldStates.get(world.getName());
    if (state == null) {
      return;
    }
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return;
    }
    Store<EntityStore> store = ref.getStore();
    UUID uuid = playerRef.getUuid();
    state.playerScores.putIfAbsent(uuid, 0);
    state.playerNames.put(uuid, resolveDisplayName(playerRef));
    String partyList = buildPartyList(world);
    if (showWelcome) {
      VexHudSequenceSupport.showWelcomeThenScore(world, playerRef, state.totalScore,
          state.playerScores.getOrDefault(uuid, 0), 0,
          java.util.Objects.requireNonNull(partyList, "partyList"));
    } else {
      VexScoreHud.open(store, ref, playerRef, state.totalScore,
          state.playerScores.getOrDefault(uuid, 0), 0,
          java.util.Objects.requireNonNull(partyList, "partyList"));
    }
  }

  public void pollWorld(@Nonnull World world) {
    String worldName = world.getName();
    RoguelikeWorldState state = worldStates.get(worldName);
    if (state == null) {
      return;
    }

    ParticipantTracker.get().updateFromWorld(world);
    Map<String, String> currentPlayers = new HashMap<>();
    for (PlayerRef playerRef : world.getPlayerRefs()) {
      String uuid = playerRef.getUuid().toString();
      String name = resolveDisplayName(playerRef);
      currentPlayers.put(uuid, name);
    }
    dataStore.updateCurrentPlayers(worldName, currentPlayers);

    int maxPlayers = dataStore.getConfig().getMaxPlayersPerInstance();
    if (maxPlayers > 0 && currentPlayers.size() >= maxPlayers) {
      PortalPlacementRegistry.closePortals("Vex_The_Lich_Dungeon");
      if (!state.capacityReached) {
        state.capacityReached = true;
        dispatchEvent(world, new InstanceCapacityReachedEvent(
            "Vex_The_Lich_Dungeon",
            worldName,
            maxPlayers,
            currentPlayers.size()));
      }
    } else if (state.capacityReached) {
      state.capacityReached = false;
    }

    for (PlayerRef playerRef : world.getPlayerRefs()) {
      if (playerRef == null) {
        continue;
      }
      Player player = resolvePlayer(playerRef);
      if (player == null) {
        continue;
      }
      com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform = player
          .getTransformComponent();
      if (transform == null) {
        continue;
      }
      Vector3d pos = transform.getPosition();
      RoomKey key = toGridKey(pos);
      RoomKey previous = state.playerRooms.put(playerRef.getUuid(), key);
      if (!key.equals(previous)) {
        onEnterRoom(world, state, key, previous);
        emitRoomEntered(world, playerRef, key, previous);
      }
    }
  }

  private void onEnterRoom(@Nonnull World world, @Nonnull RoguelikeWorldState state, @Nonnull RoomKey key,
      RoomKey previous) {
    DungeonTile room = state.rooms.get(key);
    if (room == null) {
      DungeonTile source = previous != null ? state.rooms.get(previous) : null;
      room = createRoomTile(key.x, key.z, state, source);
      state.rooms.put(key, room);
      int[] worldPos = gridToWorld(key.x, key.z);
      // Emit event request instead of direct call
      WorldEventQueue.get().dispatch(world,
          new RoomTileSpawnRequestedEvent(world, java.util.Objects.requireNonNull(room, "room"),
              worldPos[0], generator.getSpawnCenterY(), worldPos[1]));
      emitRoomGenerated(world, room, key);
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
    if (room.isEventRoom() && !roomState.safeCounted) {
      roomState.safeCounted = true;
      state.safeRoomsVisited++;
      state.roundsCleared++;
      RoomCoordinate roomCoordinate = new RoomCoordinate(key.x, key.z);
      dispatchEvent(world, new RoundClearedEvent(world, roomCoordinate));
      dispatchEvent(world, new SafeRoomVisitedEvent(world, roomCoordinate));
      engineAdapter.onRoundCleared(world.getName());
      engineAdapter.onSafeRoomVisited(world.getName());
      spawnReturnPortal(world, state, key);
      state.roomsSinceEvent = 0;
      if (lootService != null) {
        int threat = roomState.enemyPoints;
        lootService.generateLoot(state.totalScore, threat)
            .forEach(roll -> {
              log.info("[LOOT] Event room reward: %s x%d",
                  roll.getItemId(), roll.getCount());
              String itemId = roll.getItemId();
              if (itemId != null) {
                dispatchEvent(world,
                    new LootRolledEvent(world, roomCoordinate, itemId, roll.getCount()));
              }
            });
      }
      if (eventsLogger != null) {
        eventsLogger.info("Event room visited. Total events: " + state.safeRoomsVisited);
      }
    } else if (!room.isEventRoom()) {
      state.roomsSinceEvent++;
      if (state.roomsSinceEvent >= dataStore.getConfig().getEventRoomInterval()) {
        state.eventRoomPending = true;
        dispatchEvent(world, new SafeRoomNeededEvent(world,
            state.roomsSinceEvent,
            dataStore.getConfig().getEventRoomInterval()));
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
    // Emit event request instead of direct call
    WorldEventQueue.get().dispatch(world,
        new RoomEnemiesSpawnRequestedEvent(world, key.x, key.z));
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
        DungeonTile current = state.rooms.get(key);
        DungeonTile room = createRoomTile(neighborKey.x, neighborKey.z, state, current);
        state.rooms.put(neighborKey, room);
        int[] worldPos = gridToWorld(neighborKey.x, neighborKey.z);
        // Emit event request instead of direct call
        WorldEventQueue.get().dispatch(world,
            new RoomTileSpawnRequestedEvent(world, java.util.Objects.requireNonNull(room, "room"),
                worldPos[0], generator.getSpawnCenterY(), worldPos[1]));
        emitRoomGenerated(world, room, neighborKey);
      }
    }
  }

  @Nonnull
  private DungeonTile createRoomTile(int gridX, int gridZ, RoguelikeWorldState state, @Nullable DungeonTile source) {
    String prefabPath;
    if (state.eventRoomPending) {
      prefabPath = selectEventPrefab(source);
      state.eventRoomPending = false;
    } else {
      prefabPath = selectRoomPrefab(source);
    }
    if (prefabPath == null) {
      prefabPath = selector.getBasePrefab();
    }
    DungeonTile tile = new DungeonTile(gridX, gridZ, prefabPath, selector.selectRandomRotation(),
        DungeonTile.TileType.ROOM);
    if (prefabPath.startsWith("Event/")) {
      tile.setEventRoom(true);
    }
    return tile;
  }

  private String selectRoomPrefab(@Nullable DungeonTile source) {
    if (source == null || stitchIndex == null) {
      return selector.selectRandomRoom();
    }

    java.util.Set<String> roomSet = new java.util.HashSet<>(selector.getDiscovery().getAllDungeonPrefabs());
    java.util.List<String> matchingRooms = new java.util.ArrayList<>();
    for (java.util.Map.Entry<String, java.util.List<String>> entry : stitchIndex.getStitchesToPrefabs().entrySet()) {
      if (!entry.getValue().contains(source.getPrefabPath())) {
        continue;
      }
      for (String prefab : entry.getValue()) {
        if (roomSet.contains(prefab)) {
          matchingRooms.add(prefab);
        }
      }
    }

    if (matchingRooms.isEmpty()) {
      return selector.selectRandomRoom();
    }
    return matchingRooms.get(new java.util.Random().nextInt(matchingRooms.size()));
  }

  private String selectEventPrefab(DungeonTile source) {
    List<String> events = selector.getDiscovery().getAllEventPrefabs();
    if (events.isEmpty()) {
      return selector.selectRandomRoom();
    }
    if (source == null || stitchIndex == null) {
      return events.get(new java.util.Random().nextInt(events.size()));
    }

    List<String> matchingEvents = new java.util.ArrayList<>();
    for (java.util.Map.Entry<String, java.util.List<String>> entry : stitchIndex.getStitchesToPrefabs().entrySet()) {
      if (!entry.getValue().contains(source.getPrefabPath())) {
        continue;
      }
      for (String prefab : entry.getValue()) {
        if (prefab.startsWith("Event/")) {
          matchingEvents.add(prefab);
        }
      }
    }

    if (matchingEvents.isEmpty()) {
      return events.get(new java.util.Random().nextInt(events.size()));
    }
    return matchingEvents.get(new java.util.Random().nextInt(matchingEvents.size()));
  }

  /**
   * Public wrapper for spawnEnemiesForRoom - used by event-driven architecture.
   * Spawns enemies for a room at the given grid coordinates (triggered by
   * RoomEnemiesSpawnRequestedEvent).
   */
  public void spawnEnemiesForRoomRequest(@Nonnull World world, int roomX, int roomZ) {
    String worldName = world.getName();
    RoguelikeWorldState state = worldStates.get(worldName);
    if (state == null) {
      return;
    }
    RoomKey key = new RoomKey(roomX, roomZ);
    spawnEnemiesForRoom(world, state, key);
  }

  public void trackEntitySpawned(@Nonnull EntitySpawnedEvent event) {
    World world = event.getWorld();
    if (world == null) {
      return;
    }
    RoguelikeWorldState state = worldStates.get(world.getName());
    if (state == null) {
      return;
    }
    UUID entityId = event.getEntityId();
    RoomCoordinate room = event.getRoom();
    if (entityId == null || room == null) {
      return;
    }
    RoomKey key = new RoomKey(room.getX(), room.getZ());
    state.enemyRooms.putIfAbsent(entityId, key);
    state.enemyPoints.putIfAbsent(entityId, Math.max(0, event.getPoints()));
    if (event.getEntityType() != null) {
      state.enemyTypes.putIfAbsent(entityId, event.getEntityType());
    }
    if (event.getPosition() != null) {
      state.enemyPositions.putIfAbsent(entityId, event.getPosition());
    }
    if (eventsLogger != null) {
      eventsLogger.info("[TRACKED] Enemy " + event.getEntityType() + " (" + entityId
          + ") spawned in room (" + room.getX() + ", " + room.getZ() + ") - now tracking for elimination");
    }
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
    DungeonTile tile = state.rooms.get(key);
    PrefabInspector.PrefabDimensions dims = tile != null
        ? prefabSpawner.getPrefabDimensions(tile.getPrefabPath())
        : null;
    int tileBaseY = generator.getSpawnCenterY() + ROOM_Y_OFFSET;
    int minY = tileBaseY + (dims != null ? dims.minY : 0);
    int maxY = tileBaseY + (dims != null ? dims.maxY : 0);
    int worldMinY = generator.getConfig().getWorldMinY();
    int worldMaxY = generator.getConfig().getWorldMaxY();
    minY = Math.max(worldMinY, minY);
    maxY = Math.min(worldMaxY - 2, maxY);

    int spawned = 0;
    int totalEnemyPoints = 0;
    for (int i = 0; i < enemyPlan.size(); i++) {
      SpawnPoolEntry enemy = enemyPlan.get(i);
      if (enemy == null) {
        continue;
      }
      int offsetX = (i % 3) * 3;
      int offsetZ = (i / 3) * 3;
      int spawnX = baseX + offsetX;
      int spawnZ = baseZ + offsetZ;
      Integer spawnY = findSpawnY(world, spawnX, spawnZ, minY, maxY);
      if (spawnY == null) {
        log.warn("[ROGUELIKE] No safe ground for enemy spawn at (%d,%d) in room (%d,%d)", spawnX, spawnZ, key.x,
            key.z);
        continue;
      }
      Vector3d spawnPos = new Vector3d(spawnX, spawnY, spawnZ);
      if (spawnEnemy(world, state, key, enemy, spawnPos)) {
        spawned++;
        totalEnemyPoints += Math.max(0, enemy.getPoints());
      }
    }

    roomState.enemiesRemaining = spawned;
    roomState.enemyPoints = totalEnemyPoints;
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
      if (roleName == null || modelId == null) {
        log.warn("[ROGUELIKE] Enemy role/model missing for spawn entry");
        return false;
      }
      NPCPlugin npcPlugin = NPCPlugin.get();
      if (!npcPlugin.hasRoleName(roleName)) {
        log.warn("[ROGUELIKE] Unknown NPC role: %s", roleName);
        return false;
      }

      Store<EntityStore> store = world.getEntityStore().getStore();
      if (store == null) {
        log.warn("[ROGUELIKE] Missing entity store for NPC spawn");
        return false;
      }

      Pair<com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>, INonPlayerCharacter> pair = npcPlugin
          .spawnNPC(
              store,
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
          state.enemyTypes.put(uuid, roleName);
          state.enemyPositions.put(uuid, position);
          dispatchEvent(world, new EntitySpawnedEvent(world, uuid, new RoomCoordinate(key.x, key.z), roleName,
              Math.max(1, enemy.getPoints()), position));
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
      int currentScore = state.playerScores.getOrDefault(killerUuid, 0);
      state.playerScores.put(killerUuid, currentScore + Math.max(0, points));
    }
    if (killerUuid != null) {
      engineAdapter.onKill(world.getName(), killerUuid.toString(), "Enemy", points);
    }
    updateScoreHud(world, state, killerUuid, points);

    if (roomState.enemiesRemaining == 0) {
      roomState.cleared = true;
      state.roomsCleared++;
      engineAdapter.onRoomCleared(world.getName());
      dispatchEvent(world, new RoomClearedEvent(world, new RoomCoordinate(key.x, key.z)));
      log.info("[ROGUELIKE] Room (%d, %d) cleared", key.x, key.z);
      if (eventsLogger != null) {
        eventsLogger.info("Room (" + key.x + ", " + key.z + ") cleared");
      }
    }
  }

  public void handleEntityRemoved(@Nonnull com.hypixel.hytale.server.core.entity.Entity entity,
      @Nullable World world) {
    if (eventsLogger != null) {
      eventsLogger.info("[REMOVAL-HANDLER] handleEntityRemoved called for entity");
    }

    // If world wasn't passed, try to get it from entity (fallback)
    if (world == null) {
      world = entity.getWorld();
    }

    if (world == null) {
      if (eventsLogger != null) {
        eventsLogger.warn("[REMOVAL-HANDLER] World is null for entity");
      }
      return;
    }

    RoguelikeWorldState state = worldStates.get(world.getName());
    if (state == null) {
      if (eventsLogger != null) {
        eventsLogger.warn("[REMOVAL-HANDLER] No RoguelikeWorldState for world: " + world.getName());
      }
      return;
    }

    UUID uuid = entity.getUuid();
    if (uuid == null) {
      if (eventsLogger != null) {
        eventsLogger.warn("[REMOVAL-HANDLER] Entity has no UUID");
      }
      return;
    }

    Integer points = state.enemyPoints.get(uuid);
    RoomKey key = state.enemyRooms.get(uuid);
    String entityType = state.enemyTypes.get(uuid);

    if (eventsLogger != null) {
      eventsLogger.info("[REMOVED] Entity " + uuid + " removed. Tracked: points=" + points + ", key="
          + key + ", type=" + entityType);
    }

    if (points != null && key != null && entityType != null) {
      if (eventsLogger != null) {
        eventsLogger.info("[ELIMINATED] Tracked enemy " + entityType + " (" + uuid
            + ") confirmed - emitting elimination event");
      }

      // Get position from tracked data instead of entity (entity is already removed)
      Vector3d position = state.enemyPositions.get(uuid);

      // Try to get transform from entity as fallback if tracked position is null
      if (position == null) {
        try {
          com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform = entity
              .getTransformComponent();
          if (transform != null) {
            position = transform.getPosition();
          }
        } catch (IllegalStateException e) {
          if (eventsLogger != null) {
            eventsLogger.warn("[ELIMINATED] Could not get position from removed entity: " + uuid);
          }
        }
      }

      // Find killer using tracked position instead of entity reference
      UUID killerUuid = (position != null && key != null)
          ? findNearestPlayerInRoom(world, state, key, position)
          : null;

      if (killerUuid == null && world.getPlayerCount() == 1) {
        for (PlayerRef playerRef : world.getPlayerRefs()) {
          if (playerRef != null && playerRef.getUuid() != null) {
            killerUuid = playerRef.getUuid();
            break;
          }
        }
      }

      handleEntityEliminated(world, uuid, killerUuid, position);
    } else {
      if (eventsLogger != null) {
        eventsLogger.warn("[REMOVED] Entity (" + uuid
            + ") removed but not tracked as enemy (not in dungeon scoring system)");
      }
    }
  }

  public void handleEntityEliminated(@Nonnull World world, @Nonnull UUID uuid,
      @Nullable UUID killerUuid, @Nullable Vector3d position) {
    RoguelikeWorldState state = worldStates.get(world.getName());
    if (state == null) {
      if (eventsLogger != null) {
        eventsLogger.warn("[ELIMINATION] Entity " + uuid + " in world " + world.getName()
            + " but no world state - cannot emit elimination event");
      }
      return;
    }

    Integer points = state.enemyPoints.remove(uuid);
    RoomKey key = state.enemyRooms.remove(uuid);
    String entityType = state.enemyTypes.remove(uuid);
    Vector3d trackedPosition = state.enemyPositions.remove(uuid);

    if (eventsLogger != null) {
      eventsLogger.info("[ELIMINATION] Processing entity " + uuid + ": points=" + points + ", key="
          + key + ", type=" + entityType);
    }

    if (points == null || key == null) {
      if (eventsLogger != null) {
        eventsLogger.warn("[ELIMINATION] Entity " + uuid
            + " missing critical data (points=" + points + ", key=" + key
            + ") - cannot emit elimination event");
      }
      return;
    }

    Vector3d finalPosition = position != null ? position : trackedPosition;
    PlayerRef killerRef = killerUuid == null ? null : Universe.get().getPlayer(killerUuid);
    String killerName = killerRef == null ? null : resolveDisplayName(killerRef);

    if (eventsLogger != null) {
      eventsLogger.info("[ELIMINATION] Emitting EntityEliminatedEvent for " + entityType + " ("
          + uuid + ") killed by " + (killerName == null ? "unknown" : killerName) + " at room ("
          + key.x + ", " + key.z + ") for " + points + " points");
    }

    dispatchEvent(world, new EntityEliminatedEvent(world, uuid, killerUuid, killerName, points,
        new RoomCoordinate(key.x, key.z), entityType, finalPosition));
    recordEnemyKilled(world, key, points, killerUuid);
  }

  public void removeWorldState(@Nonnull String worldName) {
    worldStates.remove(worldName);
  }

  private void emitRoomGenerated(@Nonnull World world, @Nullable DungeonTile room, @Nonnull RoomKey key) {
    if (room == null) {
      return;
    }
    String prefabPath = room.getPrefabPath();
    if (prefabPath == null) {
      return;
    }
    dispatchEvent(world, new RoomGeneratedEvent(world, new RoomCoordinate(key.x, key.z), prefabPath));
  }

  private void emitRoomEntered(@Nonnull World world, @Nonnull PlayerRef playerRef, @Nonnull RoomKey key,
      @Nullable RoomKey previous) {
    RoomCoordinate room = new RoomCoordinate(key.x, key.z);
    RoomCoordinate previousRoom = previous == null ? null : new RoomCoordinate(previous.x, previous.z);
    dispatchEvent(world, new RoomEnteredEvent(world, playerRef, room, previousRoom));
  }

  private void dispatchEvent(@Nullable World world, @Nullable IEvent<Void> event) {
    if (event == null) {
      return;
    }
    WorldEventQueue.get().dispatch(world, event);
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

    String safeStatsLine = java.util.Objects.requireNonNullElse(statsLine, "");
    String safeSummaryLine = java.util.Objects.requireNonNullElse(statsLine, "");
    String safeLeaderboard = java.util.Objects.requireNonNullElse(leaderboardText, "");
    VexHudSequenceSupport.showSummarySequence(playerRef,
        java.util.Objects.requireNonNull(safeStatsLine, "statsLine"),
        java.util.Objects.requireNonNull(safeSummaryLine, "summaryLine"),
        java.util.Objects.requireNonNull(safeLeaderboard, "leaderboardText"));
  }

  private UUID findNearestPlayerInRoom(@Nonnull World world, @Nonnull RoguelikeWorldState state,
      @Nonnull RoomKey key, @Nonnull Vector3d entityPosition) {
    double bestDistance = Double.MAX_VALUE;
    UUID bestUuid = null;
    for (PlayerRef playerRef : world.getPlayerRefs()) {
      if (playerRef == null) {
        continue;
      }
      Player player = resolvePlayer(playerRef);
      if (player == null) {
        continue;
      }
      RoomKey playerRoom = state.playerRooms.get(playerRef.getUuid());
      if (playerRoom == null || !playerRoom.equals(key)) {
        continue;
      }
      com.hypixel.hytale.server.core.modules.entity.component.TransformComponent playerTransform = player
          .getTransformComponent();
      if (playerTransform == null) {
        continue;
      }
      Vector3d playerPos = playerTransform.getPosition();
      double dx = playerPos.x - entityPosition.x;
      double dy = playerPos.y - entityPosition.y;
      double dz = playerPos.z - entityPosition.z;
      double dist = dx * dx + dy * dy + dz * dz;
      if (dist < bestDistance) {
        bestDistance = dist;
        bestUuid = playerRef.getUuid();
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
    String worldName = world.getName();
    // Extract instance UUID from world name (e.g.,
    // "instance-Vex_The_Lich_Dungeon-{UUID}")
    int instanceNumber = extractInstanceNumber(worldName);
    String partyList = buildPartyList(world);
    for (PlayerRef playerRef : world.getPlayerRefs()) {
      if (playerRef == null) {
        continue;
      }
      UUID uuid = playerRef.getUuid();
      Ref<EntityStore> ref = playerRef.getReference();
      if (ref == null || !ref.isValid()) {
        continue;
      }
      Store<EntityStore> store = ref.getStore();
      state.playerNames.put(uuid, resolveDisplayName(playerRef));
      int playerScore = state.playerScores.getOrDefault(uuid, 0);
      int playerDelta = (killerUuid != null && killerUuid.equals(uuid)) ? delta : 0;
      VexScoreHud.open(store, ref, playerRef, instanceNumber, playerScore, playerDelta,
          java.util.Objects.requireNonNull(partyList, "partyList"));
    }
  }

  private int extractInstanceNumber(@Nonnull String worldName) {
    // Extract UUID from world name and use hash code as instance number
    // Format: "instance-Vex_The_Lich_Dungeon-{UUID}"
    int lastDash = worldName.lastIndexOf('-');
    if (lastDash > 0 && lastDash < worldName.length() - 1) {
      String uuidPart = worldName.substring(lastDash + 1);
      return Math.abs(uuidPart.hashCode() % 1000); // Use last 3 digits of hash
    }
    return Math.abs(worldName.hashCode() % 1000);
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
    dispatchEvent(world, new ReturnPortalSpawnedEvent(world,
        new RoomCoordinate(key.x, key.z),
        new Vector3i(portalX, portalY, portalZ)));
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
    RoomKey removedRoom = state.returnPortalRoom;
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
    if (removedRoom != null) {
      dispatchEvent(world, new ReturnPortalRemovedEvent(world,
          new RoomCoordinate(removedRoom.x, removedRoom.z),
          pos));
    }
  }

  @Nullable
  private Player resolvePlayer(@Nonnull PlayerRef playerRef) {
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return null;
    }
    Store<EntityStore> store = ref.getStore();
    return store.getComponent(ref, Player.getComponentType());
  }

  @Nonnull
  private String resolveDisplayName(@Nonnull PlayerRef playerRef) {
    String username = playerRef.getUsername();
    Player player = resolvePlayer(playerRef);
    if (player == null) {
      return username == null ? "" : username;
    }
    String displayName = player.getDisplayName();
    return displayName == null || displayName.isBlank()
        ? (username == null ? "" : username)
        : displayName;
  }

  private String buildPartyList(@Nonnull World world) {
    StringBuilder builder = new StringBuilder();
    for (ParticipantSnapshot snapshot : ParticipantTracker.get().getParticipants(world.getName())) {
      String name = snapshot.getName();
      String health = formatStat(snapshot.getHealth(), snapshot.getHealthMax());
      String stamina = formatStat(snapshot.getStamina(), snapshot.getStaminaMax());
      if (builder.length() > 0) {
        builder.append("\n");
      }
      builder.append(name)
          .append(" — HP ")
          .append(health)
          .append(" | ST ")
          .append(stamina);
    }
    return builder.toString();
  }

  private String formatStat(float currentValue, float maxValue) {
    if (currentValue < 0f) {
      return "?";
    }
    int current = Math.max(0, Math.round(currentValue));
    int max = Math.max(0, Math.round(maxValue));
    if (max <= 0 && current > 0) {
      return String.valueOf(current);
    }
    if (max <= 0) {
      return "?";
    }
    return current + "/" + max;
  }

  private Integer findSpawnY(@Nonnull World world, int x, int z, int minY, int maxY) {
    if (maxY < minY) {
      return null;
    }
    long chunkKey = (((long) (x >> 4)) << 32) | (((long) (z >> 4)) & 0xFFFFFFFFL);
    BlockAccessor chunk = world.getChunkIfLoaded(chunkKey);
    if (chunk == null) {
      return null;
    }
    int searchMax = Math.max(minY, maxY - 2);
    for (int y = minY; y <= searchMax; y++) {
      if (!isSolid(chunk, x, y, z)) {
        continue;
      }
      if (isReplaceable(chunk, x, y + 1, z) && isReplaceable(chunk, x, y + 2, z)) {
        return y + 1;
      }
    }
    return null;
  }

  private boolean isSolid(@Nonnull BlockAccessor chunk, int x, int y, int z) {
    int id = chunk.getBlock(x, y, z);
    if (id == 0 || id == BlockType.EMPTY_ID || id == BlockType.UNKNOWN_ID) {
      return false;
    }
    BlockType block = BlockType.getAssetMap().getAsset(id);
    if (block == null) {
      return false;
    }
    return !isReplaceable(block);
  }

  private boolean isReplaceable(@Nonnull BlockAccessor chunk, int x, int y, int z) {
    int id = chunk.getBlock(x, y, z);
    if (id == 0 || id == BlockType.EMPTY_ID || id == BlockType.UNKNOWN_ID) {
      return true;
    }
    BlockType block = BlockType.getAssetMap().getAsset(id);
    if (block == null) {
      return false;
    }
    return isReplaceable(block);
  }

  private boolean isReplaceable(@Nonnull BlockType block) {
    if (block.getMaterial() == com.hypixel.hytale.protocol.BlockMaterial.Empty
        || block.getDrawType() == com.hypixel.hytale.protocol.DrawType.Empty) {
      return true;
    }
    String key = block.getId() != null ? block.getId().toLowerCase() : "";
    return key.contains("grass")
        || key.contains("plant")
        || key.contains("flower")
        || key.contains("mushroom")
        || key.contains("fern")
        || key.contains("bush")
        || key.contains("leaves")
        || key.contains("sapling");
  }

  private static class RoguelikeWorldState {
    private final Map<RoomKey, DungeonTile> rooms = new HashMap<>();
    private final Map<RoomKey, RoomState> roomStates = new HashMap<>();
    private final Map<UUID, RoomKey> playerRooms = new HashMap<>();
    private final Map<UUID, Integer> playerScores = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();
    private int roomsEntered = 0;
    private int totalScore = 0;
    private int totalKills = 0;
    private int roomsCleared = 0;
    private int roundsCleared = 0;
    private int safeRoomsVisited = 0;
    private int roomsSinceEvent = 0;
    private boolean eventRoomPending = false;
    private boolean capacityReached = false;
    private final Map<UUID, Integer> enemyPoints = new HashMap<>();
    private final Map<UUID, RoomKey> enemyRooms = new HashMap<>();
    private final Map<UUID, String> enemyTypes = new HashMap<>();
    private final Map<UUID, Vector3d> enemyPositions = new HashMap<>();
    private RoomKey returnPortalRoom = null;
    private Vector3i returnPortalPos = null;
  }

  private static class RoomState {
    private boolean activated = false;
    private boolean cleared = false;
    private int enemiesRemaining = 0;
    private int enemyPoints = 0;
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
