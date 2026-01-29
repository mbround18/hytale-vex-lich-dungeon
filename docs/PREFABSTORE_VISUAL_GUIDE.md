# PrefabStore Visual Explanation & Diagrams

## The Problem in One Diagram

```
                    YOUR CODE                          ACTUAL API
                    =========                          ===========

Input:          "Rooms/Vex_Room_S_Lava_B"
                        ↓
Add prefix:   "Mods/VexLichDungeon/" + input
                        ↓
Call API:   getAssetPrefabFromAnyPack(...)
                        ↓
Inside API: basePath.resolve(input)
                        ↓
Result:     /assets/Server/Prefabs/Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B
                        ↓
            ❌ FILE DOESN'T EXIST


vs.

Input:          "Rooms/Vex_Room_S_Lava_B"
                        ↓
Call API:   getAssetPrefabFromAnyPack(...)
                        ↓
Inside API: basePath.resolve(input)
                        ↓
Result:     /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
                        ↓
            ✓ FILE EXISTS
            ✓ LOADS SUCCESSFULLY
```

---

## Complete Asset Pack Resolution Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                      SERVER STARTUP                               │
└──────────────────────────────────────────────────────────────────┘
                              ↓
                   ┌──────────────────────┐
                   │  AssetModule.setup() │
                   └──────────────────────┘
                              ↓
        ┌─────────────────────────────────────────────────┐
        │  Load all packs from:                           │
        │  • CLI: --asset-directory                       │
        │  • CLI: --mods-directories                      │
        │  • Default: PluginManager.MODS_PATH             │
        └─────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────────────────────────────────┐
        │  For each directory, load manifest.json         │
        │  • Check "IncludesAssetPack": true              │
        │  • Check "DisabledByDefault": false             │
        │  • Check HytaleServerConfig mod enabled status  │
        └─────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────────────────────────────────┐
        │  registerPack(name, path, manifest)             │
        │  • Create AssetPack object                      │
        │  • Add to AssetModule.assetPacks list           │
        │  • Log: "Loaded pack: ..."                      │
        └─────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│  RUNTIME - Prefab Loading                                        │
└──────────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────────────────────────────────┐
        │  prefabStore.getAssetPrefabFromAnyPack(input)   │
        └─────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────────────────────────────────┐
        │  For each AssetPack in AssetModule.assetPacks:  │
        │                                                 │
        │  1. Get base path:                              │
        │     pack.getRoot() + "Server" + "Prefabs"      │
        │     = /assets/Server/Prefabs                   │
        │                                                 │
        │  2. Resolve input path:                         │
        │     basePath.resolve(input)                     │
        │     = /assets/Server/Prefabs/Rooms/Vex_...     │
        │                                                 │
        │  3. Check if exists:                            │
        │     Files.exists(fullPath)                      │
        │                                                 │
        │  4. If yes:                                     │
        │     return getPrefab(fullPath)                  │
        │     ├─ Check cache                              │
        │     ├─ Load from disk                           │
        │     ├─ Deserialize BSON                         │
        │     └─ Cache and return                         │
        │                                                 │
        │  5. If no match found:                          │
        │     return null                                 │
        └─────────────────────────────────────────────────┘
                              ↓
                       Result returned
```

---

## File System Structure vs. What API Expects

```
ACTUAL FILE SYSTEM                    API EXPECTATION
==================                    ================

/assets/                              AssetPack.root = /assets/
├── manifest.json
├── Common/                           Input to getAssetPrefabFromAnyPack:
├── Cosmetics/                        "Rooms/Vex_Room_S_Lava_B"
└── Server/
    └── Prefabs/                      ↓ resolve to ↓
        ├── Rooms/
        │   ├── Vex_Room_S_Lava_A.prefab.json
        │   └── Vex_Room_S_Lava_B.prefab.json ← FILE
        ├── Hallways/
        ├── Gates/
        └── Decoration/


NOT EXPECTED:                         WRONG INPUT (Your Current Code):
=============                         =================================

/assets/
└── Server/
    └── Prefabs/
        └── Mods/
            └── VexLichDungeon/
                └── Rooms/
                    └── Vex_Room_S_Lava_B.prefab.json

This structure doesn't exist!         Input: "Mods/VexLichDungeon/Rooms/..."
                                     Resolves to: /assets/Server/Prefabs/Mods/...
                                     ❌ FILE NOT FOUND
```

---

## Path Resolution Step-by-Step

```
SCENARIO: Loading "Rooms/Vex_Room_S_Lava_B"

Step 1: User Code
        ┌─────────────────────────────────────────┐
        │ loadPrefab("Rooms/Vex_Room_S_Lava_B")  │
        └─────────────────────────────────────────┘
                        ↓

Step 2: Call PrefabStore Method
        ┌──────────────────────────────────────────────┐
        │ prefabStore.getAssetPrefabFromAnyPack(       │
        │     "Rooms/Vex_Room_S_Lava_B"               │
        │ )                                            │
        └──────────────────────────────────────────────┘
                        ↓

Step 3: Get All Registered Packs
        ┌──────────────────────────────────────────────┐
        │ AssetModule.get().getAssetPacks()           │
        │ Returns:                                     │
        │ [                                            │
        │   AssetPack(                                 │
        │     name: "MBRound18.VexLichDungeon",      │
        │     root: /assets/                          │
        │   ),                                         │
        │   AssetPack(                                 │
        │     name: "Hytale",                         │
        │     root: /hytale/assets/                   │
        │   ),                                         │
        │   ...more packs...                          │
        │ ]                                            │
        └──────────────────────────────────────────────┘
                        ↓

Step 4: Process First Pack (VexLichDungeon)
        ┌──────────────────────────────────────────────┐
        │ For pack = VexLichDungeon:                   │
        │                                              │
        │ a) Get base path for this pack:              │
        │    pack.getRoot() = /assets/                │
        │    .resolve("Server") = /assets/Server/     │
        │    .resolve("Prefabs") = /assets/Server/Prefabs/
        │                                              │
        │ b) Resolve input path:                       │
        │    basePath.resolve("Rooms/Vex_Room_S_Lava_B")
        │    = /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
        │                                              │
        │ c) Check if file exists:                     │
        │    Files.exists(/assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B)
        │    Result: YES ✓                            │
        │                                              │
        │ d) File found! Load and return:              │
        │    getPrefab(/assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B)
        │    → Load from disk                          │
        │    → Deserialize BSON                       │
        │    → Cache in PREFAB_CACHE                  │
        │    → Return BlockSelection                  │
        └──────────────────────────────────────────────┘
                        ↓

Step 5: Return to Caller
        ┌──────────────────────────────────────────────┐
        │ BlockSelection prefab = [loaded prefab]      │
        │ Log: "Successfully loaded prefab: ..."       │
        │ Return CompletableFuture with result         │
        └──────────────────────────────────────────────┘
```

---

## Cache Behavior Diagram

```
FIRST LOAD
==========

getPrefab(Path filePath)
         ↓
Check PREFAB_CACHE
  key = filePath.toAbsolutePath().normalize()
  key = /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
         ↓
Cache MISS (first time)
         ↓
Load from disk:
  ├─ Open file
  ├─ Read BSON data
  ├─ Deserialize to BlockSelection
  └─ Store in cache
         ↓
PREFAB_CACHE now contains:
  {
    /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B : BlockSelection(...),
    /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_A : BlockSelection(...),
    ...
  }
         ↓
Return BlockSelection


SECOND LOAD (SAME PREFAB)
=========================

getPrefab(Path filePath)
         ↓
Check PREFAB_CACHE
  key = /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
         ↓
Cache HIT ✓
         ↓
Return cached BlockSelection INSTANTLY
(No disk I/O, no deserialization)
         ↓
Result: ~1000x faster
```

---

## Asset Pack Registration Order

```
AssetModule Setup Sequence:

┌─────────────────────────────────────────────────────────────┐
│ 1. Parse Command-Line Arguments                             │
│    • --asset-directory /path/to/packs                       │
│    • --mods-directories /path/to/mods                       │
│    • Default MODS_PATH from PluginManager                   │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Load Packs from CLI Options                              │
│                                                             │
│    for each --asset-directory:                              │
│        loadAndRegisterPack(path)                            │
│                                                             │
│    ✓ VexLichDungeon loaded here                            │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Load Packs from MODS_PATH                                │
│                                                             │
│    for each subdirectory in PluginManager.MODS_PATH:        │
│        if has manifest.json and "IncludesAssetPack":        │
│            loadAndRegisterPack(subdirectory)                │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Load Packs from --mods-directories                       │
│                                                             │
│    for each --mods-directories:                             │
│        for each subdirectory:                               │
│            if has manifest.json and "IncludesAssetPack":    │
│                loadAndRegisterPack(subdirectory)            │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Verify At Least One Pack Loaded                          │
│                                                             │
│    if (assetPacks.isEmpty()) {                              │
│        log.error("Failed to load any asset packs")          │
│        server.shutdown()                                    │
│    }                                                        │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Ready to Serve Prefab Requests                           │
│                                                             │
│    assetPacks = [                                           │
│        AssetPack(VexLichDungeon, /assets/),                │
│        AssetPack(Hytale, /hytale/assets/),                 │
│        ...                                                  │
│    ]                                                        │
│                                                             │
│    ✓ Ready for getAssetPrefabFromAnyPack() calls           │
└─────────────────────────────────────────────────────────────┘
```

---

## Error Scenario vs. Success Scenario

```
ERROR SCENARIO (CURRENT CODE)
==============================

loadPrefab("Rooms/Vex_Room_S_Lava_B")
         ↓
prefabStore.getAssetPrefabFromAnyPack(
    "Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B"  ← WRONG
)
         ↓
For pack VexLichDungeon:
    basePath = /assets/Server/Prefabs/
    fullPath = /assets/Server/Prefabs/Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B
    Files.exists(fullPath) = NO ❌
         ↓
For pack Hytale:
    basePath = /hytale/assets/Server/Prefabs/
    fullPath = /hytale/assets/Server/Prefabs/Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B
    Files.exists(fullPath) = NO ❌
         ↓
No more packs
         ↓
return null
         ↓
if (prefab == null) {
    throw new PrefabLoadException(
        "Prefab not found: Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B"
    )
}
         ↓
CompletableFuture completes with exception
         ↓
Error logged: "Failed to load prefab Rooms/Vex_Room_S_Lava_B"


SUCCESS SCENARIO (FIXED CODE)
==============================

loadPrefab("Rooms/Vex_Room_S_Lava_B")
         ↓
prefabStore.getAssetPrefabFromAnyPack(
    "Rooms/Vex_Room_S_Lava_B"  ← CORRECT
)
         ↓
For pack VexLichDungeon:
    basePath = /assets/Server/Prefabs/
    fullPath = /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
    Files.exists(fullPath) = YES ✓
         ↓
    getPrefab(fullPath):
        ├─ Load from disk
        ├─ Parse BSON
        ├─ Create BlockSelection
        └─ Cache result
         ↓
    return BlockSelection
         ↓
if (prefab == null) {
    // False - we have a prefab
}
         ↓
log.info("Successfully loaded prefab: Rooms/Vex_Room_S_Lava_B")
         ↓
return prefab
         ↓
CompletableFuture completes with BlockSelection
         ↓
Success! Prefab ready for spawning
```

---

## Method Call Hierarchy

```
User Code
    └── PrefabSpawner.loadPrefab(String)
            └── PrefabStore.getAssetPrefabFromAnyPack(String)  ← Main method
                    └── AssetModule.get()
                            └── Returns singleton instance
                    └── .getAssetPacks()  ← Gets registered packs
                            └── Returns List<AssetPack>
                    ├── For each pack:
                    │       └── PrefabStore.getAssetPrefabsPathForPack(AssetPack)
                    │               └── Returns pack.getRoot() + "Server" + "Prefabs"
                    │
                    ├── Path.resolve(inputPath)
                    │       └── Appends input to base path
                    │
                    ├── Files.exists(Path)
                    │       └── Checks file system
                    │
                    └── PrefabStore.getPrefab(Path)
                            ├── Check PREFAB_CACHE
                            │       └── Returns cached BlockSelection if available
                            │
                            └── If not cached:
                                    ├── Read file
                                    ├── SelectionPrefabSerializer.deserialize()
                                    │   └── Parses BSON format
                                    ├── Store in PREFAB_CACHE
                                    └── Return BlockSelection
```

---

## Namespace vs. File Path Distinction

```
IN-GAME DISPLAY NAME (Namespace)
=================================

What players/admins see:
    "Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B"

Used in:
    • GUI prefab selection
    • In-game command names
    • Player-facing documentation

⚠️  NOT used in getAssetPrefabFromAnyPack()


FILE SYSTEM PATH (What API Expects)
===================================

What the file system actually contains:
    /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json

Used in:
    • PrefabStore methods
    • Path resolution
    • File I/O operations

✓ This is what getAssetPrefabFromAnyPack() expects


Why the Confusion?
==================

The namespace and path are DIFFERENT because:

1. In-game, everything is prefixed with pack namespace
   to avoid conflicts

2. On disk, files are organized under the pack's directory
   (the AssetPack.root determines the base)

3. The API (getAssetPrefabFromAnyPack) handles the
   pack selection automatically by iterating packs

4. So you only need to provide the PATH RELATIVE
   to the pack's {root}/Server/Prefabs/ directory
```

---

## Complete Example: Loading Multiple Prefabs

```
Code:
    loadPrefab("Rooms/Vex_Room_S_Lava_B")
    loadPrefab("Hallways/Vex_Room_S_Hallway_N")
    loadPrefab("Gates/Vex_Seperator_Gate_Lava")

Execution:

Load 1: "Rooms/Vex_Room_S_Lava_B"
    ├─ Path: /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
    ├─ Status: ✓ FILE FOUND
    ├─ Action: Load, parse, cache
    └─ Cache now contains:
        {
            /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B : BlockSelection(...),
            ...
        }

Load 2: "Hallways/Vex_Room_S_Hallway_N"
    ├─ Path: /assets/Server/Prefabs/Hallways/Vex_Room_S_Hallway_N
    ├─ Status: ✓ FILE FOUND
    ├─ Action: Load, parse, cache
    └─ Cache now contains:
        {
            /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B : BlockSelection(...),
            /assets/Server/Prefabs/Hallways/Vex_Room_S_Hallway_N : BlockSelection(...),
            ...
        }

Load 3: "Gates/Vex_Seperator_Gate_Lava"
    ├─ Path: /assets/Server/Prefabs/Gates/Vex_Seperator_Gate_Lava
    ├─ Status: ✓ FILE FOUND
    ├─ Action: Load, parse, cache
    └─ Cache now contains:
        {
            /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B : BlockSelection(...),
            /assets/Server/Prefabs/Hallways/Vex_Room_S_Hallway_N : BlockSelection(...),
            /assets/Server/Prefabs/Gates/Vex_Seperator_Gate_Lava : BlockSelection(...),
            ...
        }

Reload Load 1: "Rooms/Vex_Room_S_Lava_B"
    ├─ Path: /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
    ├─ Status: ✓ CACHE HIT
    ├─ Action: Return from cache (instant)
    └─ Time: ~0.1ms vs. ~10ms for disk load
```

---

## Summary Diagram

```
┌───────────────────────────────────────────────────────────┐
│              HYTALE PREFAB LOADING SYSTEM                 │
└───────────────────────────────────────────────────────────┘

┌─────────────────────┐
│  manifest.json      │
│  "IncludesAsset     │
│   Pack": true       │
└─────────────────────┘
         ↓
┌─────────────────────────────────────┐
│  AssetModule.registerPack()         │
│  ├─ name: "VexLichDungeon"          │
│  ├─ root: /assets/                  │
│  └─ Add to assetPacks list          │
└─────────────────────────────────────┘
         ↓
┌─────────────────────────────────────┐
│  User Code                          │
│  loadPrefab("Rooms/Vex_...")        │
└─────────────────────────────────────┘
         ↓
┌──────────────────────────────────────────────────────────┐
│  PrefabStore.getAssetPrefabFromAnyPack("Rooms/Vex_...")│
├──────────────────────────────────────────────────────────┤
│ For each AssetPack:                                     │
│   basePath = pack.root + "Server" + "Prefabs"          │
│   fullPath = basePath + "Rooms/Vex_..."                │
│   if (Files.exists(fullPath)):                          │
│     return getPrefab(fullPath)                          │
│   else:                                                 │
│     check next pack                                     │
└──────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────┐
│  getPrefab(Path)                    │
├─────────────────────────────────────┤
│ Check PREFAB_CACHE                  │
│  ├─ MISS: Load from disk            │
│  │        Cache result              │
│  │        Return BlockSelection      │
│  │                                  │
│  └─ HIT: Return from cache          │
└─────────────────────────────────────┘
         ↓
    BlockSelection ready for use
```

Perfect! These diagrams visualize the complete system flow.
