# PrefabStore Quick Reference & Action Items

## What You Need to Know

### The Problem (One Sentence)
You're adding `"Mods/VexLichDungeon/"` prefix to your prefab paths, but the asset pack system doesn't expect it.

### The Fix (One Line)
Change `"Mods/VexLichDungeon/" + modRelativePath` to just `modRelativePath`

---

## How getAssetPrefabFromAnyPack() Actually Works

```
Input Path: "Rooms/Vex_Room_S_Lava_B"
           ↓
For each registered AssetPack:
    fullPath = {assetPack.root}/Server/Prefabs/ + input
    fullPath = /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
           ↓
If file exists → Load and return ✓
If no packs have it → Return null (causes your error)
```

---

## Your Actual Files

```
/assets/manifest.json                              ← Registered as asset pack ✓
/assets/Server/Prefabs/Rooms/                      ← Base prefabs directory ✓
/assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json  ← YOUR FILE ✓
```

---

## Current Wrong Code vs. Fixed Code

### WRONG ❌
```java
String namespacedPath = "Mods/VexLichDungeon/" + modRelativePath;
// Constructs: /assets/Server/Prefabs/Mods/VexLichDungeon/Rooms/Vex_Room_S_Lava_B
// FILE DOESN'T EXIST
```

### CORRECT ✓
```java
String prefabPath = modRelativePath;
// Constructs: /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B
// FILE EXISTS
```

---

## The 5-Minute Fix

Edit [plugin/src/main/java/com/example/hytale/vexlichdungeon/prefab/PrefabSpawner.java](plugin/src/main/java/com/example/hytale/vexlichdungeon/prefab/PrefabSpawner.java#L51-L65)

Replace this:
```java
@Nonnull
public CompletableFuture<BlockSelection> loadPrefab(@Nonnull String modRelativePath) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            // Asset packs are namespaced - prefabs show in-game as "Mods/VexLichDungeon/Category/Name"
            // So we need to prepend "Mods/VexLichDungeon/" to the path
            String namespacedPath = "Mods/VexLichDungeon/" + modRelativePath;
            log.info("Loading prefab: [%s]", namespacedPath);

            // PrefabStore.getAssetPrefabFromAnyPack() resolves paths relative to {assetPackRoot}/Server/Prefabs/
            BlockSelection prefab = prefabStore.getAssetPrefabFromAnyPack(namespacedPath);

            if (prefab == null) {
                throw new PrefabLoadException("Prefab not found: " + namespacedPath);
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

With this:
```java
@Nonnull
public CompletableFuture<BlockSelection> loadPrefab(@Nonnull String modRelativePath) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            log.info("Loading prefab: [%s]", modRelativePath);

            // PrefabStore.getAssetPrefabFromAnyPack() expects paths relative to {assetPackRoot}/Server/Prefabs/
            // Asset pack registration handles the namespacing automatically
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

---

## Why This Works

| Aspect | How It Works |
|--------|-------------|
| **Asset Pack Registration** | Your manifest.json has `"IncludesAssetPack": true` → Server registers `/assets/` as an AssetPack |
| **Prefab Discovery** | PrefabStore.getAssetPrefabFromAnyPack() iterates registered packs and looks in `{pack}/Server/Prefabs/` |
| **Path Resolution** | Your input path + base directory = full file path |
| **Your Files** | Located at `/assets/Server/Prefabs/Rooms/` → matches expected structure |
| **Caching** | Once loaded, cached in memory for performance |

---

## Verification Steps

After fixing, verify with these debug methods:

```java
// 1. List all registered asset packs
public void debugListLoadedPacks() {
    List<AssetPackPrefabPath> paths = prefabStore.getAllAssetPrefabPaths();
    log.info("Found {} asset packs", paths.size());
    for (AssetPackPrefabPath p : paths) {
        log.info("  - {}", p.getAssetPack().getName());
    }
}

// 2. Check if specific prefab can be found
public void debugFindPrefab(String path) {
    Path found = prefabStore.findAssetPrefabPath(path);
    if (found != null) {
        log.info("Found: {}", found);
    } else {
        log.warning("Not found: {}", path);
    }
}

// 3. Try loading directly
public void debugLoadPrefab(String path) {
    BlockSelection prefab = prefabStore.getAssetPrefabFromAnyPack(path);
    log.info("Load result for {}: {}", path, (prefab != null ? "SUCCESS" : "NULL"));
}
```

---

## What getAssetPrefabFromAnyPack() Does NOT Do

- ❌ Does NOT add "Mods/" prefix automatically
- ❌ Does NOT search your mod's directory structure
- ❌ Does NOT read a separate prefab registry file
- ❌ Does NOT resolve multiple levels of nesting
- ❌ Does NOT handle "in-game names" like "Mods/VexLichDungeon/Rooms/Name"

---

## What getAssetPrefabFromAnyPack() DOES Do

- ✓ Gets list of all registered AssetPacks
- ✓ For each pack: checks {pack.root}/Server/Prefabs/{yourInput}
- ✓ Returns first match found
- ✓ Caches the result for future calls
- ✓ Returns null if not found in any pack

---

## File Existence Verification

Your prefab file definitely exists:

```bash
# Confirm the file exists
ls -lh /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json

# Check it's readable
file /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json

# View first few bytes
head -c 100 /assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json
```

---

## Common Mistakes to Avoid

| Mistake | Why It Fails | Fix |
|---------|-------------|-----|
| Using `"Mods/VexLichDungeon/Rooms/..."` | Extra nesting not in file system | Remove prefix |
| Using absolute paths | System expects relative paths | Use relative to `Server/Prefabs/` |
| Forgetting `.prefab.json` | System adds extension | Don't include `.prefab.json` in path |
| Including `Server/Prefabs/` | Base path already includes this | Just put `Rooms/Name` |
| Different asset pack not registered | File exists but pack not loaded | Ensure manifest.json in pack root |

---

## The Asset Pack Architecture

```
Server Boot Sequence:
1. Load command-line options (--asset-directory, --mods-directories)
2. AssetModule.setup() initializes
3. Load manifest.json from each pack location
4. For enabled packs: call registerPack(name, path, manifest)
5. Store AssetPack objects in AssetModule.assetPacks list
6. When prefab is requested: iterate assetPacks and search

Your Setup:
├── manifest.json (has "IncludesAssetPack": true)
├── Common/
├── Cosmetics/
└── Server/
    └── Prefabs/
        ├── Rooms/
        │   └── Vex_Room_S_Lava_B.prefab.json  ← YOUR FILE HERE
        ├── Hallways/
        ├── Gates/
        └── Decoration/
```

---

## Method Signatures for Reference

```java
// What you're calling
public BlockSelection getAssetPrefabFromAnyPack(String inputPath)

// It internally uses
public Path getAssetPrefabsPathForPack(AssetPack pack)
// Returns: pack.getRoot() + "Server" + "Prefabs"

// And Path operations
public BlockSelection getPrefab(Path fullPath)
// Loads from: fullPath

// For debugging
public List<AssetPackPrefabPath> getAllAssetPrefabPaths()
public Path findAssetPrefabPath(String inputPath)
```

---

## Summary

| Item | Status | Notes |
|------|--------|-------|
| Your prefab file exists | ✓ YES | `/assets/Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json` |
| Asset pack registered | ✓ YES | manifest.json with `IncludesAssetPack: true` |
| Path construction | ❌ WRONG | You're adding unnecessary `Mods/VexLichDungeon/` prefix |
| API being used correctly | ❌ NO | Decompilation shows it expects just relative path |
| Cache system | ✓ YES | Works transparently once path is correct |

**The fix is simple: remove the `"Mods/VexLichDungeon/"` prefix from your path strings.**
