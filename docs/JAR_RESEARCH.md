# Hytale API Research - JAR Analysis Results

## BlockSelection Class - FOUND ✅

**Location**: `com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection`

### Key Methods for Spawning Prefabs

#### Place Methods (PERFECT!)

```java
// Core placement methods:
public BlockSelection place(CommandSender, World)
public BlockSelection place(CommandSender, World, BlockMask)
public BlockSelection place(CommandSender, World, Vector3i position, BlockMask)
public BlockSelection place(CommandSender, World, Vector3i position, BlockMask,
                           Consumer<Ref<EntityStore>> entityConsumer)

// No-return variants (fire and forget):
public void placeNoReturn(World, Vector3i position,
                         ComponentAccessor<EntityStore>)
public void placeNoReturn(String prefabId, CommandSender, World,
                         ComponentAccessor<EntityStore>)
public void placeNoReturn(String prefabId, CommandSender, FeedbackConsumer,
                         World, ComponentAccessor<EntityStore>)
public void placeNoReturn(String prefabId, CommandSender, FeedbackConsumer,
                         World, Vector3i position, BlockMask,
                         ComponentAccessor<EntityStore>)
```

**RECOMMENDED FOR DUNGEON GENERATION**:

```java
BlockSelection prefab = /* loaded prefab */;
prefab.place(commandSender, world, new Vector3i(worldX, worldY, worldZ), null, entityConsumer);
```

#### Rotation Methods (FOUND!)

```java
public BlockSelection rotate(Axis axis, int degrees)
public BlockSelection rotate(Axis axis, int degrees, Vector3f origin)
public BlockSelection rotateArbitrary(float x, float y, float z)
public BlockSelection flip(Axis axis)
```

**For our use case:**

```java
// Rotate around Y axis (vertical) for cardinal directions
BlockSelection rotated = prefab.rotate(Axis.Y, rotationDegrees);
// rotationDegrees: 0, 90, 180, or 270
```

#### Other Useful Methods

```java
public void setPosition(int x, int y, int z)
public void setAnchor(int x, int y, int z)
public BlockSelection cloneSelection()
public int getBlockCount()
public int getEntityCount()
public boolean canPlace(World, Vector3i, IntList)
```

---

## World Class - FOUND ✅

**Location**: `com.hypixel.hytale.server.core.universe.world.World`

### Key Methods

```java
// Getting the world:
public String getName()
public boolean isAlive()

// Entity operations:
public <T extends Entity> T spawnEntity(T entity, Vector3d position, Vector3f rotation)
public <T extends Entity> T addEntity(T entity, Vector3d position, Vector3f rotation, AddReason)
public Entity getEntity(UUID uuid)
public List<Player> getPlayers()

// Storage access:
public ChunkStore getChunkStore()
public EntityStore getEntityStore()

// Chunk operations:
public CompletableFuture<WorldChunk> getChunkAsync(long chunkKey)
public WorldChunk getChunkIfLoaded(long chunkKey)
public WorldChunk loadChunkIfInMemory(long chunkKey)

// Executor (can run tasks):
public void execute(Runnable)
public void consumeTaskQueue()

// Configuration:
public WorldConfig getWorldConfig()
public GameplayConfig getGameplayConfig()
```

---

## How to Get World from Instance - FOUND! ✅

**Best Method: Listen to AddPlayerToWorldEvent**

```java
public class com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent
    implements IEvent<String> {

  public Holder<EntityStore> getHolder();
  public World getWorld();
  public boolean shouldBroadcastJoinMessage();
  public void setBroadcastJoinMessage(boolean);
}
```

### Recommended Implementation

```java
// In your plugin's setup():
EventBus eventBus = HytaleServer.get().getEventBus();
eventBus.register(AddPlayerToWorldEvent.class, event -> {
    World world = event.getWorld();
    EntityStore player = event.getHolder().getComponent(Player.getComponentType());

    // Check if this is a Vex Lich Dungeon instance
    String worldName = world.getName();
    if (worldName.contains("VexLichDungeon")) {
        // Start generation!
        startDungeonGeneration(world, player);
    }
});
```

### World Access Pattern

```java
// From any event that provides World:
World world = event.getWorld();

// Or directly from player ref:
PlayerRef playerRef = /* from somewhere */;
World world = getWorldFromPlayer(playerRef); // need to find this

// From event context:
Holder<EntityStore> entityHolder = event.getHolder();
ComponentAccessor<EntityStore> accessor = /* derive from event */;
```

---

## ComponentAccessor - FOUND ✅

**Location**: `com.hypixel.hytale.component.ComponentAccessor`

Used for accessing entity components. For prefab spawning:

```java
// Getting entity store accessor:
ComponentAccessor<EntityStore> accessor = world.getEntityStore().something();
// EXACT METHOD NEEDS VERIFICATION
```

---

## FeedbackConsumer - FOUND ✅

**Location**: `com.hypixel.hytale.server.core.prefab.selection.standard.FeedbackConsumer`

Optional feedback during placement. Can be null for silent operation.

```java
FeedbackConsumer feedback = new FeedbackConsumer() {
    @Override
    public void accept(String message) {
        log.info(message);
    }
};
```

---

## Axis Enum - FOUND ✅

**Location**: `com.hypixel.hytale.math.Axis`

Values (likely):

- `Axis.X` - Rotate around X axis
- `Axis.Y` - Rotate around Y axis (cardinal directions)
- `Axis.Z` - Rotate around Z axis

---

## Implementation Plan

### Phase 1: Basic Spawning

```java
public void spawnPrefab(BlockSelection prefab, World world, Vector3i position,
                       int rotationDegrees, CommandSender sender) {
    // Clone to avoid modifying original
    BlockSelection clone = prefab.cloneSelection();

    // Apply rotation (Y axis for cardinal directions)
    if (rotationDegrees != 0) {
        clone = clone.rotate(Axis.Y, rotationDegrees);
    }

    // Set position
    clone.setPosition(position.x, position.y, position.z);

    // Spawn into world
    clone.place(sender, world, position, null, null);
}
```

### Phase 2: With Feedback

```java
clone.placeNoReturn(
    "vex_dungeon_prefab",
    sender,
    message -> log.info("Placement: %s", message),
    world,
    position,
    null, // no block mask
    componentAccessor
);
```

### Phase 3: Full Gate + Entity Spawning

```java
clone.place(
    sender,
    world,
    position,
    null,
    entityRef -> {
        // Handle entity spawning from prefab
        log.info("Spawned entity: %s", entityRef);
    }
);
```

---

## Remaining Unknowns

1. ✅ How to spawn prefabs into world - **SOLVED: `place()` method**
2. ✅ How to rotate prefabs - **SOLVED: `rotate()` method**
3. ✅ How to get World - **PARTIAL: Found World class, need Instance.getWorld()**
4. ✅ Entity handling - **FOUND: entities passed to consumer**
5. ❓ Getting ComponentAccessor<EntityStore> - Need to check `world.getEntityStore()`
6. ❓ Loading prefabs by path from mod assets - Verify `PrefabStore.getAssetPrefabFromAnyPack()`
7. ❓ Instance structure - Still searching for Instance class definition

---

## Next Steps

1. Search decompiled code for `Instance.getWorld()` or similar
2. Find how to get `ComponentAccessor<EntityStore>` from World
3. Test loading prefabs from mod asset pack
4. Implement PrefabSpawner.spawnTile() with actual code
5. Create player join event handler

---

**Date**: January 27, 2026
**Source**: `/home/mbruno/development/hytale-mods/hytale-vex-lich-dungeon/data/server/Server/HytaleServer.jar`
**Tool**: `javap` decompilation
