# Vex Lich Dungeon - Generation System Overview

## What's Been Built

I've created the complete infrastructure for procedural dungeon generation! Here's what's ready:

### ✅ Completed Components

#### 1. **PrefabPathHelper** (`plugin/src/.../prefab/PrefabPathHelper.java`)
Helper utility for converting asset paths to mod-relative paths that Hytale recognizes.

```java
// Convert asset path to mod path
String path = PrefabPathHelper.toModPath("Base/Vex_Courtyard_Base.prefab.json");
// Returns: "Mods/VexLichDungeon/Base/Vex_Courtyard_Base.prefab.json"

// Convenience methods for each category
String basePath = PrefabPathHelper.toBasePath("Vex_Courtyard_Base");
String gatePath = PrefabPathHelper.toGatePath("Vex_Seperator_Gate_Opened");
String roomPath = PrefabPathHelper.toRoomPath("Vex_Room_S_Archers");
String hallwayPath = PrefabPathHelper.toHallwayPath("Vex_Room_S_Hallway_A");
```

#### 2. **CardinalDirection** (`plugin/src/.../dungeon/CardinalDirection.java`)
Enum for the 4 cardinal directions with rotation and navigation logic.

```java
CardinalDirection dir = CardinalDirection.NORTH;
int rotation = dir.getRotationDegrees(); // 0
int offsetX = dir.getOffsetX(); // 0
int offsetZ = dir.getOffsetZ(); // -1
CardinalDirection opposite = dir.getOpposite(); // SOUTH
```

#### 3. **DungeonTile** (`plugin/src/.../dungeon/DungeonTile.java`)
Data model representing a single 19x19 tile in the dungeon grid.

```java
DungeonTile tile = new DungeonTile(
    0, 0,  // Grid position
    "Mods/VexLichDungeon/Rooms/Vex_Room_S_Archers.prefab.json",
    90,    // Rotation
    DungeonTile.TileType.ROOM
);

tile.setGate(CardinalDirection.NORTH, gatePath);
```

#### 4. **GenerationConfig** (`plugin/src/.../dungeon/GenerationConfig.java`)
Configuration for generation parameters.

```java
GenerationConfig config = GenerationConfig.createDefault()
    .setGenerationRadius(5)        // 5 tiles in each direction
    .setRoomProbability(0.7)       // 70% rooms, 30% hallways
    .setTileSize(19)               // 19x19 blocks per tile
    .setAsyncGeneration(true);     // Generate in background
```

#### 5. **PrefabSelector** (`plugin/src/.../dungeon/PrefabSelector.java`)
Handles random selection of prefabs based on configured weights.

```java
PrefabSelector selector = new PrefabSelector(seed);
String randomRoom = selector.selectRandomRoom();
String randomGate = selector.selectRandomGate();
String roomOrHallway = selector.selectRoomOrHallway(0.7);
int rotation = selector.selectRandomRotation();
```

#### 6. **DungeonGenerator** (`plugin/src/.../dungeon/DungeonGenerator.java`)
Core generation algorithm that creates the dungeon layout.

```java
DungeonGenerator generator = new DungeonGenerator(config, log);
CompletableFuture<GenerationResult> future = generator.generateLayout();

future.thenAccept(result -> {
    if (result.isSuccess()) {
        Map<?, DungeonTile> tiles = result.getTiles();
        log.info("Generated %d tiles in %d ms", 
            result.getTileCount(), result.getDurationMs());
    }
});
```

**Algorithm Flow**:
1. Place base tile at origin (0, 0)
2. Generate chains of tiles in each cardinal direction (N, S, E, W)
3. Assign random gates between adjacent tiles
4. Block outer edges with blocked gate prefabs

#### 7. **PrefabSpawner** (`plugin/src/.../prefab/PrefabSpawner.java`)
⚠️ **Skeleton Implementation** - Service for spawning prefabs into the world.

This class is ready but needs Hytale API research to complete the actual world manipulation.

## 🚧 What's Needed Next

### Critical: Hytale API Research

The generation system is complete but **cannot spawn prefabs into the world yet**. We need to research:

1. **World Access**: How to get `World` instance from `Instance`
2. **Prefab Writing**: How `BlockSelection` writes to world at coordinates
3. **Rotation**: How to apply `PrefabRotation` to `BlockSelection`
4. **Entity Spawning**: How entities within prefabs are spawned
5. **Async Safety**: Thread safety for world modification
6. **Chunk Loading**: Ensuring chunks are loaded before placement

**Search Areas**:
- Look in `/data/unpacked/com/hypixel/hytale/builtin/buildertools/` for examples
- Check `PrefabProp.java` world generation usage
- Find `BlockSelection` methods in decompiled code
- Research `SelectionProvider` interface usage

### Integration Tasks

Once API research is complete:

1. **Player Join Handler**
   - Listen for first player joining instance
   - Check if dungeon already generated
   - Freeze player and place blocked gates
   - Run generation
   - Release player and replace blocked gates with random ones

2. **State Persistence**
   - Store generation metadata per instance
   - Prevent re-generation on player rejoin
   - Track which tiles have been generated

3. **Commands**
   - `/vex dungeon generate [radius]` - Manual generation/reset
   - `/vex dungeon config` - View/modify generation settings
   - `/vex dungeon info` - Show current dungeon stats

## Testing the Generation Algorithm

Even without world spawning, you can test the layout generation:

```java
// In your plugin's start() or test command
GenerationConfig config = GenerationConfig.createDefault()
    .setGenerationRadius(3)
    .setSeed(12345);

DungeonGenerator generator = new DungeonGenerator(config, log);
generator.generateLayout().thenAccept(result -> {
    if (result.isSuccess()) {
        // Inspect the generated layout
        for (var entry : result.getTiles().entrySet()) {
            DungeonTile tile = entry.getValue();
            log.info("Tile at (%d, %d): %s [%d°] with %d gates",
                tile.getGridX(), tile.getGridZ(),
                tile.getType(), tile.getRotation(),
                tile.getGates().size());
        }
    }
});
```

## File Structure

```
plugin/src/main/java/com/example/hytale/vexlichdungeon/
├── prefab/
│   ├── PrefabPathHelper.java    ✅ Complete
│   └── PrefabSpawner.java       ⚠️  Needs API research
├── dungeon/
│   ├── CardinalDirection.java   ✅ Complete
│   ├── DungeonTile.java         ✅ Complete
│   ├── GenerationConfig.java    ✅ Complete
│   ├── PrefabSelector.java      ✅ Complete
│   └── DungeonGenerator.java    ✅ Complete (layout only)
└── docs/
    └── generation_implementation.md  📋 Full spec & tracking
```

## Next Steps

1. **Research Hytale APIs** (see blockers in generation_implementation.md)
2. **Implement PrefabSpawner world writing**
3. **Create player join event handler**
4. **Add state persistence**
5. **Create admin commands**
6. **Test with real prefabs**

## Questions?

All design decisions, algorithms, and TODOs are documented in:
- `docs/generation_implementation.md` - Full specification
- Code comments in each class - Implementation details

The foundation is solid and ready for world integration! 🎉
