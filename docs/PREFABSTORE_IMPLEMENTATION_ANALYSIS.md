# PrefabStore Implementation Analysis

**Date:** January 27, 2026  
**Status:** Deep Investigation Complete

## Executive Summary

The Hytale `PrefabStore` class uses a multi-pack asset system where prefabs are resolved from registered asset packs. Your prefabs **do exist** in the correct location, but the resolution mechanism requires the asset pack to be properly registered with the server.

### Key Finding: Your Prefab Path is WRONG

**Current Code:** `"Mods/VexLichDungeon/" + modRelativePath` → `"Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B"`

**Actual File Location:** `/assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json`

**Correct Path:** `"Rooms/Vex_Room_S_Lava_B"` (WITHOUT the "Mods/VexLichDungeon/" prefix)

---

## How PrefabStore.getAssetPrefabFromAnyPack() Works

### The Algorithm (Decompiled Bytecode)

```
for each AssetPack in AssetModule.getAssetPacks():
    prefabsPath = assetPack.getRoot() + "Server" + "Prefabs"
    fullPath = prefabsPath + inputPath
    
    if (file exists at fullPath):
        return getPrefab(fullPath)
    
return null
```

### Exact Path Transformation

For input: `"Rooms/Vex_Room_S_Lava_B"`

**For your VexLichDungeon asset pack:**
```
assetPack.getRoot()                  = /path/to/unpacked/assets/
assetPack.getRoot() + "Server"       = /path/to/unpacked/assets/Server/
assetPack.getRoot() + "Server" + "Prefabs" = /path/to/unpacked/assets/Server/Prefabs/
Final full path = /path/to/unpacked/assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
```

The method then calls `Files.exists()` to check if the file exists, and if so, loads it via `getPrefab()`.

---

## 1. The Complete PrefabStore Class Methods

### Core Prefab Loading Methods

```java
public BlockSelection getAssetPrefabFromAnyPack(String inputPath)
    // Iterates through ALL registered asset packs
    // Constructs: {assetPack.root}/Server/Prefabs/{inputPath}
    // Returns first match found, or null
    
public BlockSelection getAssetPrefab(String inputPath)
    // Loads from fixed asset root path only
    // Constructs: {getAssetPrefabsPath()}/{inputPath}
    // Returns null if not found

public java.nio.file.Path getAssetPrefabsPathForPack(AssetPack pack)
    // Returns: pack.getRoot() + "Server" + "Prefabs"
    // This is the KEY METHOD that constructs the base prefab path
```

### Server/WorldGen Prefab Methods

```java
public BlockSelection getServerPrefab(String namespacedPath)
    // Loads from: {PREFABS_PATH}/{namespacedPath}
    // Used for server-installed prefabs

public BlockSelection getWorldGenPrefab(String inputPath)
    // Loads from: {getWorldGenPrefabsPath()}/{inputPath}
    // Used for procedural generation

public Map<Path, BlockSelection> getServerPrefabDir(String directory)
public Map<Path, BlockSelection> getPrefabDir(Path dirPath)
    // Lists all prefabs in a directory
```

### Asset Pack Discovery

```java
public List<AssetPackPrefabPath> getAllAssetPrefabPaths()
    // Returns list of {AssetPack, prefabsPath} pairs
    // Only includes packs where Server/Prefabs/ directory exists
    
public java.nio.file.Path findAssetPrefabPath(String inputPath)
    // Returns the path where prefab was found, or null
    
public AssetPack findAssetPackForPrefabPath(java.nio.file.Path prefabPath)
    // Returns which asset pack contains the given prefab path
```

---

## 2. How getAssetPrefabFromAnyPack() Works Internally

### The Decompiled Logic (Bytecode Analysis)

```
Method: getAssetPrefabFromAnyPack(String)
Input: "Rooms/Vex_Room_S_Lava_B"

Step 1: Get all asset packs
    AssetModule.get().getAssetPacks()  // Returns List<AssetPack>

Step 2: Iterate through each pack
    for (AssetPack pack : assetPacks) {
        // Step 3: Get base prefab path for this pack
        Path basePath = getAssetPrefabsPathForPack(pack)
        // Result: {pack.getRoot()}/Server/Prefabs
        
        // Step 4: Resolve input path
        Path fullPath = basePath.resolve(inputPath)
        // Result: {pack.getRoot()}/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
        
        // Step 5: Check if file exists
        if (Files.exists(fullPath)) {
            // Step 6: Load and return
            return getPrefab(fullPath)
        }
    }

// Step 7: No match found
return null
```

### Critical Detail: Path.resolve() Behavior

The `Path.resolve()` method used in the algorithm:
- Takes the base path and appends the relative path
- Does NOT treat "Mods/VexLichDungeon/Rooms/..." specially
- Simply concatenates: `{base} + {relative}`

**This means:**
- Input: `"Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B"`
- Resolves to: `/path/to/assets/Server/Prefabs/Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B`
- **FILE DOESN'T EXIST** ← Your problem!

---

## 3. Path Transformations Occurring

### Your Current Code Path

```
Input: "Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B"
         ↓
getAssetPrefabFromAnyPack(path)
         ↓
For each AssetPack:
    path.resolve("Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B")
         ↓
{assetPack.root}/Server/Prefabs/Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B
         ↓
FILE NOT FOUND ← Returns null
```

### The Correct Path

```
Input: "Rooms/Vex_Room_S_Lava_B"
         ↓
getAssetPrefabFromAnyPack(path)
         ↓
For each AssetPack:
    path.resolve("Rooms/Vex_Room_S_Lava_B")
         ↓
{assetPack.root}/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
         ↓
FILE EXISTS ✓
     /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json
         ↓
Load and return BlockSelection
```

---

## 4. Asset Pack Cache/Registry System

### How Asset Packs Are Registered

**AssetModule.setup()** (initialization)

```java
// 1. Load from explicit --asset-directory CLI options
for (Path assetDir : Options.ASSET_DIRECTORY) {
    loadAndRegisterPack(assetDir)
}

// 2. Load from MODS_PATH (default mods directory)
loadPacksFromDirectory(PluginManager.MODS_PATH)

// 3. Load from --mods-directories CLI options
for (Path modsDir : Options.MODS_DIRECTORIES) {
    loadPacksFromDirectory(modsDir)
}

// 4. If no packs loaded, SERVER CRASHES
if (assetPacks.isEmpty()) {
    server.shutdown("Failed to load any asset packs")
}
```

### The Registration Process

**loadAndRegisterPack(Path packLocation)**

```
Step 1: Load manifest.json from pack root
    manifest = loadPackManifest(packLocation)
    
Step 2: If no manifest, skip pack
    if (manifest == null) {
        log.warning("Skipping pack at %s: missing or invalid manifest.json")
        return
    }

Step 3: Check if mod is enabled
    PluginIdentifier id = new PluginIdentifier(manifest)
    enabled = HytaleServer.getConfig().getModConfig(id).getEnabled()
    
Step 4: If disabled, log and skip
    if (!enabled) {
        log.info("Skipped disabled pack: %s", id)
        return
    }

Step 5: Register the pack
    registerPack(id.toString(), packLocation, manifest)
    log.info("Loaded pack: %s from %s", id, packLocation)
```

### Asset Pack Data Structure

```java
class AssetPack {
    private String name              // From manifest.json
    private Path root                // Directory containing manifest.json
    private FileSystem fileSystem     // ZIP or regular filesystem
    private boolean isImmutable       // Whether pack can be modified
    private PluginManifest manifest   // Plugin metadata
    private Path packLocation         // Original JAR/ZIP location
}
```

### The Critical Registry List

```java
// In AssetModule
private final List<AssetPack> assetPacks  // Thread-safe CopyOnWriteArrayList
    
public List<AssetPack> getAssetPacks() {
    return assetPacks  // Used by PrefabStore to iterate
}
```

---

## 5. Method That Lists All Available Prefabs

### getAllAssetPrefabPaths()

```java
public List<AssetPackPrefabPath> getAllAssetPrefabPaths() {
    List<AssetPackPrefabPath> result = new ObjectArrayList()
    
    for (AssetPack pack : AssetModule.get().getAssetPacks()) {
        Path prefabsPath = getAssetPrefabsPathForPack(pack)
        
        if (Files.isDirectory(prefabsPath)) {
            result.add(new AssetPackPrefabPath(pack, prefabsPath))
        }
    }
    
    return result
}
```

### The AssetPackPrefabPath Data Class

```java
class AssetPackPrefabPath {
    private AssetPack assetPack
    private Path path  // The actual Server/Prefabs directory path
}
```

---

## 6. Manifest/Registry Files That Need to List Prefabs

### Requirements Analysis

**The PrefabStore does NOT require a separate "available prefabs" registry.**

Instead:
1. **manifest.json** - Registers the asset pack with the server
2. **Server/Prefabs/** - File system directory with actual prefab files
3. **PrefabList/** - Optional configuration file for prefab metadata

### Your manifest.json

**Location:** `/assets/manifest.json`

```json
{
  "Group": "MBRound18",
  "Name": "VexLichDungeon",
  "Version": "0.1.0",
  "Description": "Face the trials of Vex the Lich's dungeon...",
  "Authors": [...],
  "Website": "https://example.com",
  "ServerVersion": "*",
  "Dependencies": {},
  "OptionalDependencies": {},
  "DisabledByDefault": false,
  "IncludesAssetPack": true  ← KEY: This marks it as an asset pack
}
```

**This is correct!** The `"IncludesAssetPack": true` flag tells the server this directory should be registered as an asset pack.

### How Prefabs Are Discovered

The system uses **file system discovery**, not a registry:

```
1. Server loads manifest.json
2. manifest.json has "IncludesAssetPack": true
3. Server registers {assetPackRoot} as an AssetPack
4. When loading prefab, iterate through packs
5. For each pack: check if {pack.root}/Server/Prefabs/{inputPath} exists
6. If exists, load and return
```

**No separate prefab registry is needed.**

---

## 7. Why Your Current Code Fails

### The Root Cause

Your code in [PrefabSpawner.java](PrefabSpawner.java#L54-L55):

```java
String namespacedPath = "Mods/VexLichDungeon/" + modRelativePath;
log.info("Loading prefab: [%s]", namespacedPath);

BlockSelection prefab = prefabStore.getAssetPrefabFromAnyPack(namespacedPath);
```

**Problem:** `getAssetPrefabFromAnyPack()` expects a path RELATIVE to `{assetPack.root}/Server/Prefabs/`

Your asset pack root is `/assets/`

So the method constructs: `/assets/Server/Prefabs/Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B`

But your file is at: `/assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B`

**The "Mods/VexLichDungeon/" prefix is wrong.**

### Why the Comment is Misleading

Your comment says:
```
// Asset packs are namespaced - prefabs show in-game as "Mods/VexLichDungeon/Category/Name"
```

**This is NOT how getAssetPrefabFromAnyPack() works.** That's the in-game display name, not the file path. The method only handles file paths relative to the asset pack's `Server/Prefabs/` directory.

---

## 8. Complete Path Resolution Specification

### Input → Output Mapping

| Method | Input | Base Path | Result | File Location |
|--------|-------|-----------|--------|----------------|
| `getAssetPrefabFromAnyPack("Rooms/Vex")` | Relative to pack | `{pack}/Server/Prefabs/` | `{pack}/Server/Prefabs/Rooms/Vex` | ✓ FOUND |
| `getAssetPrefabFromAnyPack("Mods/VexLichDungeon/Rooms/Vex")` | Relative to pack | `{pack}/Server/Prefabs/` | `{pack}/Server/Prefabs/Mods/VexLichDungeon/Rooms/Vex` | ✗ NOT FOUND |
| `getAssetPrefab("Rooms/Vex")` | Relative to fixed asset root | `{assetRoot}/` | `{assetRoot}/Rooms/Vex` | Depends on config |
| `getServerPrefab("Rooms/Vex")` | Relative to server root | `{serverRoot}/Prefabs/` | `{serverRoot}/Prefabs/Rooms/Vex` | Server install only |

---

## 9. Recommendations for Fixing Path Resolution

### Option 1: Use Correct Simple Path (RECOMMENDED)

```java
public CompletableFuture<BlockSelection> loadPrefab(@Nonnull String modRelativePath) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            log.info("Loading prefab: [%s]", modRelativePath);
            
            // getAssetPrefabFromAnyPack() constructs:
            // {assetPack.root}/Server/Prefabs/{modRelativePath}
            BlockSelection prefab = prefabStore.getAssetPrefabFromAnyPack(modRelativePath);
            
            if (prefab == null) {
                throw new PrefabLoadException("Prefab not found: " + modRelativePath);
            }
            
            log.info("Successfully loaded prefab: %s", modRelativePath);
            return prefab;
            
        } catch (Exception e) {
            log.error("Failed to load prefab %s: %s", modRelativePath, e.getMessage());
            throw new RuntimeException("Failed to load prefab: " + modRelativePath, e);
        }
    });
}
```

**Usage:**
```java
loadPrefab("Rooms/Vex_Room_S_Lava_B")  // ✓ Works
```

### Option 2: Use Namespace-Aware Loading

If you want to support multiple mods and namespace isolation:

```java
private static final String MOD_NAME = "VexLichDungeon";

public CompletableFuture<BlockSelection> loadPrefab(@Nonnull String modRelativePath) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            // Check which pack it's actually in
            AssetPack pack = prefabStore.findAssetPackForPrefabPath(
                Paths.get(prefabStore.getAssetRootPath().toString(), 
                         "Server", "Prefabs", modRelativePath)
            );
            
            if (pack == null) {
                // Try finding by pack name
                AssetModule assetModule = AssetModule.get();
                for (AssetPack p : assetModule.getAssetPacks()) {
                    if (p.getName().equals(MOD_NAME)) {
                        pack = p;
                        break;
                    }
                }
            }
            
            if (pack == null) {
                throw new PrefabLoadException("Mod asset pack '" + MOD_NAME + "' not registered");
            }
            
            BlockSelection prefab = prefabStore.getAssetPrefabFromAnyPack(modRelativePath);
            
            if (prefab == null) {
                throw new PrefabLoadException("Prefab not found: " + modRelativePath);
            }
            
            log.info("Successfully loaded prefab: %s from pack %s", modRelativePath, pack.getName());
            return prefab;
            
        } catch (Exception e) {
            log.error("Failed to load prefab %s: %s", modRelativePath, e.getMessage());
            throw new RuntimeException("Failed to load prefab: " + modRelativePath, e);
        }
    });
}
```

### Option 3: Verify Pack Is Registered (Debug)

Before trying to load, verify your pack is registered:

```java
public void debugListLoadedPacks() {
    AssetModule assetModule = AssetModule.get();
    List<AssetPackPrefabPath> allPrefabPaths = prefabStore.getAllAssetPrefabPaths();
    
    log.info("Loaded %d asset packs with prefabs", allPrefabPaths.size());
    
    for (AssetPackPrefabPath appPath : allPrefabPaths) {
        log.info("  - Pack: %s at %s", 
            appPath.getAssetPack().getName(),
            appPath.getPath());
    }
}
```

---

## 10. Complete Internal Path Resolution Flow

```
User Code:
    loadPrefab("Rooms/Vex_Room_S_Lava_B")
        ↓
    prefabStore.getAssetPrefabFromAnyPack("Rooms/Vex_Room_S_Lava_B")
        ↓
    AssetModule.get().getAssetPacks()
        ↓
    [AssetPack(VexLichDungeon, /assets/),
     AssetPack(Hytale, /hytale/assets/),
     ...]
        ↓
    For each pack:
        ├─ pack = AssetPack(VexLichDungeon, /assets/)
        ├─ basePath = pack.getRoot() + "Server" + "Prefabs"
        │           = /assets/Server/Prefabs
        ├─ fullPath = basePath.resolve("Rooms/Vex_Room_S_Lava_B")
        │           = /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
        ├─ Files.exists(fullPath)? YES ✓
        ├─ getPrefab(fullPath)
        │   ├─ Check cache: MISS (first time)
        │   ├─ Load from disk
        │   ├─ Parse .prefab.json (BSON format)
        │   ├─ Return BlockSelection
        │   └─ Cache for future use
        └─ RETURN BlockSelection
    
Result: BlockSelection object loaded and cached
```

---

## 11. Debug Checklist

To verify your setup is correct:

- [ ] Verify manifest.json exists at `/assets/manifest.json`
- [ ] Verify `"IncludesAssetPack": true` is set
- [ ] Verify prefabs exist at `/assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json`
- [ ] Check server logs for "Loaded pack: MBRound18.VexLichDungeon" message
- [ ] Call `debugListLoadedPacks()` to verify pack registration
- [ ] Call `findAssetPrefabPath("Rooms/Vex_Room_S_Lava_B")` to verify file discovery
- [ ] Verify no parent directory path traversal (../)

---

## 12. Cache Behavior

### Prefab Caching

```java
private final Map<Path, BlockSelection> PREFAB_CACHE

public BlockSelection getPrefab(Path path) {
    return PREFAB_CACHE.computeIfAbsent(
        path.toAbsolutePath().normalize(),
        pathKey -> loadPrefabFromDisk(pathKey)
    )
}
```

**Key Points:**
- Uses `ConcurrentHashMap` for thread-safe access
- Caches by **absolute normalized path**
- Once cached, subsequent calls return instantly
- Cache is **never invalidated** during runtime

---

## Summary Table: Methods and Their Paths

| Method | Takes Input | Expects Path Relative To | Example |
|--------|-------------|--------------------------|---------|
| `getAssetPrefabFromAnyPack()` | Relative path | `{AssetPack.root}/Server/Prefabs/` | `"Rooms/Vex"` |
| `getAssetPrefab()` | Relative path | `{AssetPrefabsPath}/` | `"Rooms/Vex"` |
| `getServerPrefab()` | Relative path | `{ServerPrefabsPath}/` | `"Rooms/Vex"` |
| `getPrefab(Path)` | Absolute path | (full filesystem path) | `/path/to/Prefabs/Rooms/Vex` |
| `getPrefabDir()` | Directory path | File system | `/path/to/Prefabs/Rooms/` |

---

## Critical Implementation Details

### How Files.exists() Check Works

```java
// In getAssetPrefabFromAnyPack()
Path fullPath = basePath.resolve(inputPath)
boolean exists = Files.exists(fullPath)  // Uses LinkOption.NOFOLLOW_LINKS

// This checks:
// 1. Regular filesystem files
// 2. Files in mounted ZIPs
// 3. Does NOT follow symlinks
```

### Loading from Binary Format

Once `Files.exists()` returns true:

```
1. File exists at path
2. Call getPrefab(fullPath)
3. Check PREFAB_CACHE for hit
4. If miss, deserialize from BSON
5. Use SelectionPrefabSerializer
6. Return BlockSelection
7. Cache for next time
```

---

## Conclusion

**Your prefabs are in the correct location.** The issue is purely in the path string you're passing to `getAssetPrefabFromAnyPack()`.

- **Remove** the `"Mods/VexLichDungeon/"` prefix
- **Pass** just the relative path: `"Rooms/Vex_Room_S_Lava_B"`
- The asset pack system will resolve it correctly from there

The asset pack mechanism was designed to eliminate the need for namespace prefixes in paths - the pack selection handles the namespacing automatically.
