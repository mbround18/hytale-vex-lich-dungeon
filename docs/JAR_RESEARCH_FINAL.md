# Hytale API Research - JAR Analysis Results (Updated)

## ✅ SOLUTION FOUND: BlockSelection.place() Method

### Core API Methods

The **`BlockSelection`** class from `com.hypixel.hytale.server.core.prefab.selection.standard` has everything we need:

#### Place Methods

```java
BlockSelection place(CommandSender sender, World world)
BlockSelection place(CommandSender sender, World world, BlockMask mask)
BlockSelection place(CommandSender sender, World world, Vector3i position, BlockMask mask)
BlockSelection place(CommandSender sender, World world, Vector3i position,
                     BlockMask mask, Consumer<Ref<EntityStore>> entityConsumer)

void placeNoReturn(World world, Vector3i position, ComponentAccessor<EntityStore> accessor)
```

#### Rotation Methods

```java
BlockSelection rotate(Axis axis, int degrees)
BlockSelection rotate(Axis axis, int degrees, Vector3f origin)
```

---

## ✅ SOLUTION FOUND: World Access Pattern

### Get World from Player Join Event

The **`AddPlayerToWorldEvent`** class provides direct World access:

```java
public class AddPlayerToWorldEvent implements IEvent<String> {
  public World getWorld()
  public Holder<EntityStore> getHolder()
  public void setBroadcastJoinMessage(boolean)
}
```

### Implementation Pattern

```java
// Listen to player join event
EventBus eventBus = HytaleServer.get().getEventBus();
eventBus.register(AddPlayerToWorldEvent.class, event -> {
    World world = event.getWorld();
    String worldName = world.getName();

    if (worldName.contains("VexLichDungeon")) {
        // Generate dungeon for this instance
        startDungeonGeneration(world);
    }
});
```

---

## ✅ Complete Spawning Solution

### Step 1: Load Prefab

```java
BlockSelection prefab = PrefabStore.get()
    .getAssetPrefabFromAnyPack("Base/Vex_Courtyard_Base.prefab.json");
```

### Step 2: Clone and Rotate

```java
BlockSelection clone = prefab.cloneSelection();
if (rotationDegrees != 0) {
    clone = clone.rotate(Axis.Y, rotationDegrees);
}
```

### Step 3: Spawn into World

```java
clone.place(
    commandSender,      // Can be ConsoleSender
    world,              // From AddPlayerToWorldEvent.getWorld()
    new Vector3i(x, y, z),  // World coordinates
    null,               // No block mask
    entityRef -> {      // Entity callback
        log.info("Spawned entity: %s", entityRef);
    }
);
```

---

## Key Classes Found

| Class                 | Location                                                 | Purpose                       |
| --------------------- | -------------------------------------------------------- | ----------------------------- |
| BlockSelection        | com.hypixel.hytale.server.core.prefab.selection.standard | Load, rotate, place prefabs   |
| World                 | com.hypixel.hytale.server.core.universe.world            | Target world for placement    |
| AddPlayerToWorldEvent | com.hypixel.hytale.server.core.event.events.player       | Get World when player joins   |
| PrefabStore           | com.hypixel.hytale.server.core.prefab                    | Load prefabs from assets      |
| Axis                  | com.hypixel.hytale.math                                  | Y axis for cardinal rotations |
| Vector3i              | com.hypixel.hytale.math.vector                           | Block coordinates             |

---

## World Methods

```java
// Core access
String getName()
boolean isAlive()

// Chunk operations
CompletableFuture<WorldChunk> getChunkAsync(long chunkKey)

// Entity handling
<T extends Entity> T spawnEntity(T entity, Vector3d pos, Vector3f rotation)
List<Player> getPlayers()

// Storage
EntityStore getEntityStore()
ChunkStore getChunkStore()

// Configuration
WorldConfig getWorldConfig()
```

---

## Axis Enum Values

For dungeon generation with cardinal directions:

- **Axis.Y** with 0° = NORTH
- **Axis.Y** with 90° = EAST
- **Axis.Y** with 180° = SOUTH
- **Axis.Y** with 270° = WEST

---

## Next Steps

1. **Implement PrefabSpawner** with actual `place()` calls
2. **Create player join handler** listening to AddPlayerToWorldEvent
3. **Test with sample prefab** to verify coordinate system
4. **Handle chunk loading** before prefab placement
5. **Add entity spawning** callbacks from prefabs

---

## Code is Ready!

With this information, we can now implement:

- `PrefabSpawner.spawnTile()` - Real world manipulation
- `PrefabSpawner.spawnGate()` - Gate placement with rotation
- Player join event handler - Trigger generation on first join
- Async prefab loading - Load and spawn in background

All the missing pieces are now found!

---

**Date**: January 27, 2026  
**Source**: HytaleServer.jar (80MB)  
**Research Method**: javap decompilation and strings analysis
