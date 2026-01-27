# Vex Lich Dungeon - Procedural Generation Implementation

## Overview
Procedural dungeon generation system that builds a unique dungeon layout around the player when they first join a Vex Lich Dungeon instance.

## Dungeon Structure Specifications

### Tile Grid System
- **Tile Size**: 19x19 blocks (any height, no depth variation)
- **Doorway Marker**: Bedrock block signifies doorway locations
- **Grid Layout**: Each tile can connect to 4 adjacent tiles (North, South, East, West)

### Prefab Categories

#### Base (Starting Point)
- **Path**: `Mods/VexLichDungeon/Base/`
- **Start Prefab**: `Vex_Courtyard_Base.prefab.json`
- **Behavior**: Always pre-placed at instance spawn

#### Gates (Connectors)
- **Path**: `Mods/VexLichDungeon/Gates/`
- **Width**: 1 block wide
- **Orientation**: Must rotate based on cardinal direction (N/S/E/W)
- **Types**:
  - `Vex_Seperator_Gate_Blocked.prefab.json` - Outer edge barriers
  - `Vex_Seperator_Gate_Closed.prefab.json`
  - `Vex_Seperator_Gate_Opened.prefab.json`
  - `Vex_Seperator_Gate_Crawl.prefab.json`
  - `Vex_Seperator_Gate_Jail.prefab.json`
  - `Vex_Seperator_Gate_Lava.prefab.json`
  - `Vex_Seperator_Gate_Lighted_Door.prefab.json`
  - `Vex_Seperator_Gate_Peep.prefab.json`
  - `Vex_Seperator_Gate_Spiked.prefab.json`
  - `Vex_Seperator_Gate_Water.prefab.json`

#### Rooms (Combat/Puzzle Spaces)
- **Path**: `Mods/VexLichDungeon/Rooms/`
- **Rotation**: Random orientation per placement
- **Must Align**: Doorways must align with gates
- **Varieties**:
  - `Vex_Room_S_Archers.prefab.json`
  - `Vex_Room_S_Bats.prefab.json`
  - `Vex_Room_S_Duck.prefab.json`
  - `Vex_Room_S_Empty.prefab.json`
  - `Vex_Room_S_Lava_A.prefab.json`
  - `Vex_Room_S_Lava_B.prefab.json`
  - `Vex_Room_S_Lava_C_Hostile.prefab.json`
  - `Vex_Room_S_Mages.prefab.json`

#### Hallways (Connecting Passages)
- **Path**: `Mods/VexLichDungeon/Hallways/`
- **Purpose**: Connect rooms with variety
- **Count**: 22 variations (A-V)
- **Pattern**: `Vex_Room_S_Hallway_[A-V].prefab.json`

## Generation Algorithm

### Phase 1: Initial Spawn Protection
**Trigger**: First player joins instance
**Actions**:
1. Check if dungeon already generated (persistent flag)
2. If not generated:
   - Freeze player movement/interaction
   - Place `Vex_Seperator_Gate_Blocked.prefab.json` at all 4 base doorways
   - Display loading message to player

### Phase 2: Procedural Generation
**Parameters**:
- `generation_radius`: Number of tiles to generate in each direction (default: 5)
- `seed`: Instance-specific seed for reproducibility

**Algorithm**:
```
1. Start at base tile (0, 0)
2. For each cardinal direction (N, S, E, W):
   a. Generate chain of tiles up to generation_radius
   b. For each position:
      - Choose: Room (70%) or Hallway (30%)
      - Select random prefab from chosen category
      - Apply random rotation (0°, 90°, 180°, 270°)
      - Place gates between tiles with appropriate rotation
3. For all outer edge tiles:
   - Replace exposed gates with Blocked variant
4. Mark instance as generated (persistent storage)
```

### Phase 3: Finalization
**Actions**:
1. Replace blocked gates at base with random non-blocked gate types
2. Release player controls
3. Teleport player to precise spawn point if needed
4. Log generation completion

## Technical Implementation

### Classes to Create

#### `PrefabPathHelper.java`
**Purpose**: Convert asset paths to mod-relative paths
```java
// Mods/VexLichDungeon/Base/Vex_Courtyard_Base.prefab.json
String modPath = PrefabPathHelper.toModPath("Base/Vex_Courtyard_Base.prefab.json");
```

#### `DungeonGenerator.java`
**Purpose**: Core generation logic
- Tile grid management
- Prefab selection and placement
- Rotation handling
- Gate management

#### `DungeonTile.java`
**Purpose**: Represents single tile in grid
- Position (x, z)
- Prefab reference
- Rotation
- Gate states (N/S/E/W)

#### `PrefabSpawner.java` (or service)
**Purpose**: Interface with Hytale's prefab system
- Load prefab from PrefabStore
- Apply rotation (PrefabRotation)
- Write to world at coordinates
- Handle entity spawning within prefabs

#### `GenerationConfig.java`
**Purpose**: Configuration management
- Generation radius
- Prefab weights (room vs hallway)
- Gate selection probabilities
- Performance settings (batch size, async)

### Event Handlers

#### Player Join Event
```java
@EventHandler
public void onPlayerJoin(PlayerJoinInstanceEvent event) {
    if (isVexLichDungeonInstance(event.getInstance())) {
        if (!isDungeonGenerated(event.getInstance())) {
            initiateGeneration(event.getPlayer(), event.getInstance());
        }
    }
}
```

### Commands

#### `/vex dungeon generate [radius]`
**Permission**: `vexlich.admin.generate`
**Purpose**: Manually trigger/reset generation
**Args**: Optional radius override

#### `/vex dungeon config`
**Permission**: `vexlich.admin.config`
**Purpose**: View/modify generation parameters

## Data Storage

### Instance Metadata
**Location**: Instance-specific storage
**Data**:
```json
{
  "generated": true,
  "generation_timestamp": 1706313600000,
  "seed": 12345678,
  "radius": 5,
  "tile_count": 41,
  "generation_time_ms": 2847
}
```

### Tile Registry
**Purpose**: Track all generated tiles for future expansion
```json
{
  "tiles": [
    {
      "x": 0,
      "z": 0,
      "prefab": "Base/Vex_Courtyard_Base",
      "rotation": 0,
      "gates": {
        "north": "Vex_Seperator_Gate_Opened",
        "south": "Vex_Seperator_Gate_Lava",
        "east": "Vex_Seperator_Gate_Closed",
        "west": "Vex_Seperator_Gate_Peep"
      }
    }
  ]
}
```

## Performance Considerations

### Async Generation
- Generate in background thread
- Chunk-load areas before placement
- Batch prefab placements
- Progress updates every N tiles

### Memory Management
- Don't load all prefabs at once
- Cache frequently used prefabs
- Unload prefab data after placement

### Player Experience
- Visual feedback during generation
- Estimated time remaining
- Prevent player damage during freeze
- Smooth transition to gameplay

## Testing Checklist

- [ ] Single player spawn - generation triggers
- [ ] Multiple players join - only one generation
- [ ] Respawn after death - no regeneration
- [ ] Leave and rejoin - dungeon persists
- [ ] All gate types appear
- [ ] All room types appear
- [ ] Hallways properly placed
- [ ] Outer edges fully blocked
- [ ] No gaps between tiles
- [ ] Doorways align correctly
- [ ] Rotations work for all prefabs
- [ ] Entities spawn within prefabs
- [ ] Performance acceptable (< 5 seconds for radius 5)
- [ ] Manual regeneration command works
- [ ] Configuration changes apply

## Future Enhancements

### Dynamic Expansion
- Generate additional radius when player approaches edge
- Procedural "endless dungeon" mode

### Difficulty Scaling
- Progressive room difficulty based on distance from spawn
- Boss rooms at specific distances
- Loot quality increases with distance

### Special Rooms
- Treasure rooms (rare spawn chance)
- Puzzle rooms (require mechanics to proceed)
- Safe rooms (no enemies, healing)
- Shop rooms (NPC vendors)

### Biome Variants
- Ice dungeon theme
- Fire dungeon theme
- Poison/decay theme
- Mix themes within single dungeon

### Multiplayer Features
- Race mode (separate wings per player)
- Cooperative mode (shared progression)
- PvP arenas within dungeon

## Status Tracking

### Phase 1: Infrastructure (✅ Complete)
- [x] PrefabPathHelper utility class
- [x] CardinalDirection enum with rotation logic
- [x] DungeonTile data model
- [x] GenerationConfig configuration class
- [x] PrefabSelector for random selection
- [x] Coordinate calculation for tile grid
- [x] Rotation helper methods

### Phase 2: Core Generation (✅ Complete - Layout Only)
- [x] DungeonGenerator class
- [x] Tile placement algorithm
- [x] Gate management system
- [x] Outer edge blocking
- [ ] PrefabSpawner implementation (needs Hytale API research)

### Phase 3: Integration (Not Started)
- [ ] Player join event handler
- [ ] Generation state persistence
- [ ] Player freeze/unfreeze
- [ ] Error handling and recovery

### Phase 4: Polish (Not Started)
- [ ] Admin commands (`/vex dungeon generate`, `/vex dungeon config`)
- [ ] Progress feedback
- [ ] Performance optimization

### Phase 5: Testing (Not Started)
- [ ] Unit tests for tile logic
- [ ] Integration tests for generation
- [ ] Load testing with multiple players
- [ ] Edge case handling

## Current Blockers

### Hytale API Research Needed
The following Hytale API details need investigation:

1. **World Access**: How to get World instance from Instance
2. **Prefab Writing**: How to write BlockSelection to world at coordinates
3. **Rotation Application**: How to apply PrefabRotation to BlockSelection
4. **Entity Spawning**: How entities within prefabs are handled
5. **Async World Modification**: Thread safety for world manipulation
6. **Chunk Loading**: Ensuring chunks are loaded before prefab placement

**Action Items**:
- [ ] Search decompiled code for world manipulation examples
- [ ] Find BlockSelection write/paste methods
- [ ] Research PrefabRotation application
- [ ] Test prefab loading from mod assets

---

**Last Updated**: January 27, 2026
**Status**: Phase 1-2 Complete - Awaiting API Research for World Integration

