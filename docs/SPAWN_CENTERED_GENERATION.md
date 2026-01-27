# Spawn-Centered Dungeon Generation

## Overview

The dungeon generation system now centers the dungeon around the **actual player spawn location** rather than using hardcoded coordinates. This ensures the player spawns inside the base courtyard and the dungeon extends properly in all directions.

## Key Concepts

### 1. Spawn Location Capture

**Problem**: The spawn point defined in `instance.bson` / `config.json` doesn't always match where players actually spawn due to the game's spawn logic.

**Solution**: The `PlayerSpawnTracker` captures the **first player's actual position** when they are added to the world via `AddPlayerToWorldEvent`. This is retrieved from the player's `TransformComponent`, giving us their true in-game location.

### 2. Grid Coordinate System

The dungeon uses a **grid-based coordinate system**:

- **Grid (0, 0)** = Base courtyard (centered on player spawn)
- **Grid (1, 0)** = One tile east
- **Grid (-1, 0)** = One tile west
- **Grid (0, 1)** = One tile south
- **Grid (0, -1)** = One tile north

### 3. Tile Sizing

Each dungeon tile consists of:

- **19 blocks** for the room/hallway
- **1 block** for the connecting gate
- **Total: 20 blocks** per tile

### 4. World Coordinate Conversion

The `DungeonGenerator.gridToWorld()` method converts grid coordinates to world coordinates:

```java
worldX = spawnCenterX + (gridX * 20)
worldZ = spawnCenterZ + (gridZ * 20)
```

Example (spawn at 100, 64, 200):

- Grid (0, 0) → World (100, 64, 200) - Base courtyard at spawn
- Grid (1, 0) → World (120, 64, 200) - First room east (+20 blocks)
- Grid (-1, 0) → World (80, 64, 200) - First room west (-20 blocks)

## Generation Flow

### Step 1: Player Joins World

1. Player joins "Vex_The_Lich_Dungeon" instance
2. `AddPlayerToWorldEvent` fires
3. System retrieves player from `world.getPlayers()` and gets their actual position via `TransformComponent`
4. `PlayerSpawnTracker` captures actual spawn: `(x, y, z)` - where the game placed them, not configured spawn

### Step 2: Set Spawn Center

```java
dungeonGenerator.setSpawnCenter(
    (int) Math.floor(playerPos.x()),
    (int) Math.floor(playerPos.y()),
    (int) Math.floor(playerPos.z())
);
```

### Step 3: Generate Layout

The generator creates tiles in the grid:

- **Base tile** at grid (0, 0) - centered on spawn
- **Directional chains** extend in each cardinal direction
- **Gates** connect adjacent tiles
- **Blocked gates** seal outer edges

### Step 4: Spawn Prefabs

For each tile:

1. Convert grid coords to world coords using `gridToWorld()`
2. Load prefab from asset pack
3. Apply rotation
4. Place prefab at calculated world position

## Room Offset Calculation

Since rooms are **19 blocks** and the player spawns in the **center** of the base courtyard:

- **Room center** = 19 / 2 = 9.5 blocks from edge
- **First adjacent room starts** at spawn ± 10 blocks (9.5 + 0.5 for gate)

This is handled automatically by the 20-block tile size (19 + 1 gate).

## Prefab Anchor Points

Human-designed prefabs may have **non-zero anchor points** which affect positioning.

### Reading Anchor Data

Use `PrefabMetadata.fromPrefabFile()` to read anchor points:

```java
PrefabMetadata meta = PrefabMetadata.fromPrefabFile(prefabJsonPath, log);
int[] adjusted = meta.calculatePlacementPosition(targetX, targetY, targetZ);
// Use adjusted[0], adjusted[1], adjusted[2] for placement
```

### Anchor Interpretation

From the example `Vex_Courtyard_Base.prefab.json`:

```json
{
  "anchorX": 0,
  "anchorY": 0,
  "anchorZ": 0,
  ...
}
```

If anchors are (0, 0, 0), no adjustment needed. If non-zero:

- **Positive anchor** = blocks extend in negative direction
- Place prefab at `targetPos - anchor` to align correctly

## Instance Metadata

`InstanceMetadata` can read spawn points from config files (though actual spawn is preferred):

```java
Path configPath = Paths.get("assets/Server/Instances/Vex_The_Lich_Dungeon/config.json");
InstanceMetadata meta = InstanceMetadata.fromConfigFile(configPath, log);
double configuredSpawnX = meta.getSpawnX(); // May not match actual spawn!
```

## Debugging Tips

### Check Spawn Capture

Look for this log message:

```log
[SPAWN] Captured first player actual position at (x, y, z) for world: Vex_The_Lich_Dungeon
```

This is the **actual location** where the game placed the player, which becomes the center point for dungeon generation.

### Check Grid-to-World Conversion

Look for tile spawn messages:

```log
Spawned tile at grid(0, 0) -> world(100, 64, 200): Base/Vex_Courtyard_Base
Spawned tile at grid(1, 0) -> world(120, 64, 200): Rooms/Vex_Room_S_Archers
```

### Verify Tile Alignment

- Base courtyard should be **centered on player**
- Adjacent rooms should be **20 blocks apart** (19 + 1 gate)
- No gaps or overlaps between tiles
- Player should spawn **inside** the base courtyard, not in a wall

## Troubleshooting

### Player Spawns Outside Dungeon

**Cause**: Generation triggered before spawn capture, or spawn not captured.

**Fix**: Ensure `AddPlayerToWorldEvent` fires before generation. Check logs for spawn capture message.

### Rooms Misaligned

**Cause**: Prefab anchor points not accounted for.

**Fix**: Use `PrefabMetadata` to read anchors and adjust placement positions.

### Dungeon Not Centered

**Cause**: AddPlayerToWorldEvent fired before player was positioned in world.

**Fix**: If spawn capture shows "No players found in world yet", the system will retry on the next event. The PlayerReadyEvent will also trigger generation. Ensure at least one player is in the world when the event fires.

### Generation Happens at (0, 0, 0)

**Cause**: Generator's spawn center not set before generation.

**Fix**: Call `dungeonGenerator.setSpawnCenter()` before `generateLayout()`.

## File References

- [DungeonGenerator.java](../plugin/src/main/java/com/example/hytale/vexlichdungeon/dungeon/DungeonGenerator.java) - Grid-to-world conversion
- [PlayerSpawnTracker.java](../plugin/src/main/java/com/example/hytale/vexlichdungeon/data/PlayerSpawnTracker.java) - Spawn capture
- [DungeonGenerationEventHandler.java](../plugin/src/main/java/com/example/hytale/vexlichdungeon/events/DungeonGenerationEventHandler.java) - Event handling
- [PrefabMetadata.java](../plugin/src/main/java/com/example/hytale/vexlichdungeon/prefab/PrefabMetadata.java) - Anchor reading
- [InstanceMetadata.java](../plugin/src/main/java/com/example/hytale/vexlichdungeon/data/InstanceMetadata.java) - Config reading
