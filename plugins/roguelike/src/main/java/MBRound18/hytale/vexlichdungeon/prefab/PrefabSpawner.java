package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.vexlichdungeon.dungeon.CardinalDirection;
import MBRound18.hytale.vexlichdungeon.dungeon.DungeonTile;
import MBRound18.hytale.vexlichdungeon.dungeon.GenerationConfig;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.events.WorldEventQueue;
import MBRound18.hytale.vexlichdungeon.events.EntitySpawnedEvent;
import MBRound18.hytale.vexlichdungeon.events.RoomCoordinate;
import MBRound18.hytale.vexlichdungeon.events.ChestSpawnedEvent;
import MBRound18.hytale.vexlichdungeon.events.LootableEntitySpawnedEvent;
import MBRound18.hytale.vexlichdungeon.events.EntityReplacedEvent;
import MBRound18.hytale.vexlichdungeon.events.NpcSpawnRequestedEvent;
import MBRound18.hytale.vexlichdungeon.events.NpcSpawnResult;
import MBRound18.hytale.vexlichdungeon.events.PrefabEntitySpawnedEvent;
import MBRound18.ImmortalEngine.api.prefab.PrefabInspector;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import MBRound18.ImmortalEngine.api.prefab.PrefabEntityUtils;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.npc.NPCPlugin;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Service for spawning prefabs into the world.
 * This class interfaces with Hytale's prefab system to load and place prefabs.
 * 
 * Uses BlockSelection.place() to write prefabs to the world with proper
 * rotation and entity spawning support.
 * 
 * NOTE: Prefab metadata (anchor points) should be read from JSON if positioning
 * needs adjustment. Human-designed prefabs may have non-zero anchors that
 * affect
 * block placement relative to the specified coordinates.
 */
public class PrefabSpawner {

  private static final int ROOM_Y_OFFSET = 14;
  private static final int GATE_Y_OFFSET = 15;
  private static final int MAX_PREFAB_CACHE = 64;

  private final LoggingHelper log;
  private final PrefabInspector inspector;
  private final GenerationConfig config;
  private final @Nullable Path unpackedRoot;
  private final Map<String, SoftReference<BlockSelection>> prefabCache;
  private final Map<String, List<PrefabEntityDefinition>> prefabEntityCache = new ConcurrentHashMap<>();
  private final Map<String, String> prefabJsonCache = new ConcurrentHashMap<>();
  private final Map<String, Path> sanitizedPrefabCache = new ConcurrentHashMap<>();

  /**
   * Creates a new prefab spawner.
   * 
   * @param log Logger for spawning events
   */
  public PrefabSpawner(@Nonnull LoggingHelper log, @Nonnull GenerationConfig config) {
    this(log, config, null);
  }

  public PrefabSpawner(@Nonnull LoggingHelper log, @Nonnull GenerationConfig config,
      @Nullable Path unpackedRoot) {
    this.log = log;
    this.config = config;
    this.unpackedRoot = unpackedRoot;
    this.inspector = new PrefabInspector(log, unpackedRoot);
    this.prefabCache = Collections.synchronizedMap(new LinkedHashMap<>(MAX_PREFAB_CACHE, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, SoftReference<BlockSelection>> eldest) {
        return size() > MAX_PREFAB_CACHE;
      }
    });
  }

  /**
   * Loads a prefab from the server asset store.
   * 
   * @param modRelativePath Path relative to Server/Prefabs/ (e.g.,
   *                        "Rooms/Vex_Room_S_Lava_B")
   * @return CompletableFuture with the loaded BlockSelection
   */
  @Nonnull
  public CompletableFuture<BlockSelection> loadPrefab(@Nonnull String modRelativePath) {
    return Objects.requireNonNull(CompletableFuture.supplyAsync(() -> {
      StringBuilder jsonBuilder = new StringBuilder();
      try {
        SoftReference<BlockSelection> cachedRef = prefabCache.get(modRelativePath);
        if (cachedRef != null) {
          BlockSelection cached = cachedRef.get();
          if (cached != null) {
            return cached;
          }
        }

        log.info("Loading prefab: [%s]", modRelativePath);

        String prefabEntryPath = "Server/Prefabs/" + modRelativePath + ".prefab.json";
        Path prefabPath = resolveAssetPrefab(prefabEntryPath, modRelativePath);
        if (prefabPath == null || !Files.exists(prefabPath)) {
          throw new PrefabLoadException("Prefab file not found in assets at: " + prefabEntryPath);
        }

        try (BufferedReader reader = Files.newBufferedReader(prefabPath, StandardCharsets.UTF_8)) {
          String line;
          while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line).append('\n');
          }
        }

        String jsonContent = Objects.requireNonNull(jsonBuilder.toString(), "jsonContent");
        JsonObject root = Objects.requireNonNull(JsonParser.parseString(jsonContent).getAsJsonObject(), "root");

        // Extract entities first so they're removed from the JSON
        List<PrefabEntityDefinition> entities = extractPrefabEntities(root, modRelativePath);
        prefabEntityCache.put(modRelativePath, entities);
        log.fine("[CACHE] Cached %d entities for prefab %s", entities.size(), modRelativePath);

        // Now sanitize items from the entity-extracted root
        int sanitizedItems = sanitizeItemContainers(root);

        // Write sanitized JSON (with entities removed and items sanitized) if needed
        Path resolvedPrefabPath = prefabPath;
        if (entities.size() > 0 || sanitizedItems > 0) {
          resolvedPrefabPath = writeSanitizedPrefab(modRelativePath, prefabPath, root,
              entities.size() + sanitizedItems);
          log.fine("[SANITIZE] Wrote entity-extracted prefab to %s (removed %d entities, %d item stacks)",
              resolvedPrefabPath.getFileName(), entities.size(), sanitizedItems);
        }

        prefabJsonCache.put(modRelativePath, jsonContent);

        // Use PrefabStore to deserialize the BlockSelection from the JSON file
        BlockSelection prefab = PrefabStore.get().getPrefab(Objects.requireNonNull(resolvedPrefabPath, "prefabPath"));

        hydrateFluidsFromJson(prefab, jsonContent,
            modRelativePath);

        if (prefab.getFluidCount() > 0) {
          log.info("Prefab %s contains %d fluids", modRelativePath, prefab.getFluidCount());
        }

        log.info("Successfully loaded and deserialized prefab: %s", modRelativePath);
        for (PrefabHook hook : PrefabHookRegistry.getHooks()) {
          hook.onPrefabLoaded(modRelativePath, prefab);
        }
        prefabCache.put(modRelativePath, new SoftReference<>(prefab));
        return prefab;

      } catch (Exception e) {
        log.error("Failed to load prefab %s: %s", modRelativePath, e.getMessage());
        throw new RuntimeException("Failed to load prefab: " + modRelativePath, e);
      } finally {
      }
    }), "prefabFuture");
  }

  @Nullable
  private Path resolveAssetPrefab(@Nonnull String entryPath, @Nonnull String modRelativePath) {
    try {
      String trimmed = entryPath.startsWith("Server/Prefabs/")
          ? entryPath.substring("Server/Prefabs/".length())
          : entryPath;
      java.util.List<Path> roots = new java.util.ArrayList<>();
      PrefabStore store = PrefabStore.get();
      Path baseRoot = store.getAssetPrefabsPath();
      if (baseRoot != null) {
        roots.add(baseRoot);
      }
      for (PrefabStore.AssetPackPrefabPath packPath : store.getAllAssetPrefabPaths()) {
        if (packPath == null) {
          continue;
        }
        Path prefabsPath = packPath.prefabsPath();
        if (prefabsPath != null) {
          roots.add(prefabsPath);
        }
      }
      for (Path root : roots) {
        if (root == null) {
          continue;
        }
        Path candidate = root.resolve(trimmed);
        if (Files.exists(candidate)) {
          return candidate;
        }
      }
    } catch (Exception e) {
      log.fine("PrefabStore lookup failed for %s: %s", modRelativePath, e.getMessage());
    }
    Path root = unpackedRoot;
    if (root == null) {
      return null;
    }
    String trimmed = entryPath.startsWith("Server/") ? entryPath.substring("Server/".length()) : entryPath;
    return root.resolve(trimmed);
  }

  private List<PrefabEntityDefinition> extractPrefabEntities(@Nonnull JsonObject root, @Nonnull String prefabPath) {
    try {
      JsonArray entities = root.getAsJsonArray("entities");
      if (entities == null) {
        log.fine("[EXTRACT] Prefab %s has no entities array", prefabPath);
        return List.of();
      }
      if (entities.isEmpty()) {
        log.fine("[EXTRACT] Prefab %s entities array is empty", prefabPath);
        return List.of();
      }

      List<PrefabEntityDefinition> definitions = new ArrayList<>();
      JsonArray keptEntities = new JsonArray();
      int extracted = 0;
      int kept = 0;

      for (JsonElement element : entities) {
        if (!element.isJsonObject()) {
          keptEntities.add(element);
          kept++;
          continue;
        }
        JsonObject entity = element.getAsJsonObject();
        JsonObject components = entity.getAsJsonObject("Components");
        if (components == null) {
          keptEntities.add(entity);
          kept++;
          continue;
        }
        if (components.has("BlockEntity")) {
          keptEntities.add(entity);
          kept++;
          continue;
        }

        JsonObject modelWrapper = components.getAsJsonObject("Model");
        if (modelWrapper == null) {
          keptEntities.add(entity);
          kept++;
          continue;
        }
        JsonObject model = modelWrapper.getAsJsonObject("Model");
        if (model == null || !model.has("Id")) {
          keptEntities.add(entity);
          kept++;
          continue;
        }
        String modelId = Objects.requireNonNull(model.get("Id").getAsString(), "modelId");

        JsonObject transform = components.getAsJsonObject("Transform");
        if (transform == null) {
          keptEntities.add(entity);
          kept++;
          continue;
        }
        JsonObject position = transform.getAsJsonObject("Position");
        JsonObject rotation = transform.getAsJsonObject("Rotation");
        if (position == null || rotation == null) {
          keptEntities.add(entity);
          kept++;
          continue;
        }

        Vector3d pos = new Vector3d(
            position.get("X").getAsDouble(),
            position.get("Y").getAsDouble(),
            position.get("Z").getAsDouble());
        Vector3f rot = new Vector3f(
            (float) rotation.get("Pitch").getAsDouble(),
            (float) rotation.get("Yaw").getAsDouble(),
            (float) rotation.get("Roll").getAsDouble());

        definitions.add(new PrefabEntityDefinition(modelId, pos, rot));
        extracted++;
      }

      if (keptEntities.size() > 0) {
        root.add("entities", keptEntities);
      } else {
        root.remove("entities");
      }

      log.info("[EXTRACT] Prefab %s: %d frozen entities removed, %d other entities kept", prefabPath, extracted, kept);
      if (!definitions.isEmpty()) {
        log.info("[EXTRACT] Extracted enemy entities: %s",
            definitions.stream().map(PrefabEntityDefinition::getModelId).toList());
      }
      return definitions;
    } catch (Exception e) {
      log.warn("Failed to parse prefab entities for %s: %s", prefabPath, e.getMessage());
      return List.of();
    }
  }

  private void hydrateFluidsFromJson(
      @Nonnull BlockSelection prefab,
      @Nonnull String jsonContent,
      @Nonnull String modRelativePath) {
    try {
      if (prefab.getFluidCount() > 0) {
        return;
      }

      JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
      JsonArray fluids = root.getAsJsonArray("fluids");
      if (fluids == null || fluids.isEmpty()) {
        return;
      }

      int added = 0;
      for (JsonElement element : fluids) {
        if (!element.isJsonObject()) {
          continue;
        }
        JsonObject fluid = element.getAsJsonObject();
        String name = fluid.has("name") ? fluid.get("name").getAsString() : "Empty";
        if ("Empty".equalsIgnoreCase(name)) {
          continue;
        }

        int level = fluid.has("level") ? fluid.get("level").getAsInt() : 0;
        int fluidId = Fluid.getFluidIdOrUnknown(name, "Prefab fluid %s", name);
        if (fluidId == Fluid.EMPTY_ID) {
          continue;
        }

        int x = fluid.has("x") ? fluid.get("x").getAsInt() : 0;
        int y = fluid.has("y") ? fluid.get("y").getAsInt() : 0;
        int z = fluid.has("z") ? fluid.get("z").getAsInt() : 0;

        prefab.addFluidAtLocalPos(x, y, z, fluidId, (byte) level);
        added++;
      }

      if (added > 0) {
        log.info("Injected %d fluids into prefab %s", added, modRelativePath);
      }
    } catch (Exception e) {
      log.warn("Failed to inject fluids for prefab %s: %s", modRelativePath, e.getMessage());
    }
  }

  private void hydrateFluidsFromJson(
      @Nonnull BlockSelection prefab,
      @Nonnull String jsonContent,
      @Nonnull String modRelativePath,
      int rotationDegrees) {
    int normalized = ((rotationDegrees % 360) + 360) % 360;
    if (normalized == 0) {
      hydrateFluidsFromJson(prefab, jsonContent, modRelativePath);
      return;
    }
    try {
      if (prefab.getFluidCount() > 0) {
        return;
      }

      JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
      JsonArray fluids = root.getAsJsonArray("fluids");
      if (fluids == null || fluids.isEmpty()) {
        return;
      }

      int added = 0;
      for (JsonElement element : fluids) {
        if (!element.isJsonObject()) {
          continue;
        }
        JsonObject fluid = element.getAsJsonObject();
        String name = fluid.has("name") ? fluid.get("name").getAsString() : "Empty";
        if ("Empty".equalsIgnoreCase(name)) {
          continue;
        }

        int level = fluid.has("level") ? fluid.get("level").getAsInt() : 0;
        int fluidId = Fluid.getFluidIdOrUnknown(name, "Prefab fluid %s", name);
        if (fluidId == Fluid.EMPTY_ID) {
          continue;
        }

        int x = fluid.has("x") ? fluid.get("x").getAsInt() : 0;
        int y = fluid.has("y") ? fluid.get("y").getAsInt() : 0;
        int z = fluid.has("z") ? fluid.get("z").getAsInt() : 0;

        int[] rotated = rotateLocalXZ(x, z, normalized);
        prefab.addFluidAtLocalPos(rotated[0], y, rotated[1], fluidId, (byte) level);
        added++;
      }

      if (added > 0) {
        log.info("[FLUIDS] Rehydrated %d fluids into %s after rotation %d",
            added, modRelativePath, normalized);
      }
    } catch (Exception e) {
      log.warn("Failed to inject rotated fluids for prefab %s: %s", modRelativePath, e.getMessage());
    }
  }

  private int[] rotateLocalXZ(int x, int z, int rotationDegrees) {
    int normalized = ((rotationDegrees % 360) + 360) % 360;
    return switch (normalized) {
      case 90 -> new int[] { -z, x };
      case 180 -> new int[] { -x, -z };
      case 270 -> new int[] { z, -x };
      default -> new int[] { x, z };
    };
  }

  private int sanitizeItemContainers(@Nonnull JsonObject root) {
    return sanitizeItemContainers((JsonElement) root);
  }

  private int sanitizeItemContainers(@Nonnull JsonElement element) {
    if (element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();
      int updated = 0;
      if (obj.has("ItemContainer") && obj.get("ItemContainer").isJsonObject()) {
        JsonObject container = obj.getAsJsonObject("ItemContainer");
        if (container != null) {
          updated += sanitizeItemContainer(container);
        }
      }
      for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
        JsonElement value = entry.getValue();
        if (value != null) {
          updated += sanitizeItemContainers(value);
        }
      }
      return updated;
    }
    if (element.isJsonArray()) {
      int updated = 0;
      JsonArray array = element.getAsJsonArray();
      for (JsonElement child : array) {
        if (child != null) {
          updated += sanitizeItemContainers(child);
        }
      }
      return updated;
    }
    return 0;
  }

  private int sanitizeItemContainer(@Nonnull JsonObject container) {
    JsonObject items = container.getAsJsonObject("Items");
    if (items == null) {
      return 0;
    }
    int updated = 0;
    for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
      if (!entry.getValue().isJsonObject()) {
        continue;
      }
      JsonObject item = entry.getValue().getAsJsonObject();
      if (!item.has("MaxDurability") || !item.get("MaxDurability").isJsonPrimitive()) {
        continue;
      }
      double max = item.get("MaxDurability").getAsDouble();
      if (max <= 0.0) {
        item.addProperty("MaxDurability", 1.0);
        max = 1.0;
        updated++;
      }
      if (item.has("Durability") && item.get("Durability").isJsonPrimitive()) {
        double durability = item.get("Durability").getAsDouble();
        double clamped = Math.min(Math.max(0.0, durability), max);
        if (clamped != durability) {
          item.addProperty("Durability", clamped);
          updated++;
        }
      }
    }
    return updated;
  }

  private Path writeSanitizedPrefab(
      @Nonnull String modRelativePath,
      @Nonnull Path originalPath,
      @Nonnull JsonObject root,
      int sanitizedItems) {
    try {
      Path cached = sanitizedPrefabCache.get(modRelativePath);
      if (cached != null && Files.exists(cached)) {
        return cached;
      }
      Path sanitizedRoot = Path.of("build", "prefab-sanitized");
      Path sanitizedPath = sanitizedRoot.resolve(modRelativePath + ".prefab.json");
      Path parent = sanitizedPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(sanitizedPath, root.toString(), StandardCharsets.UTF_8);
      sanitizedPrefabCache.put(modRelativePath, sanitizedPath);
      log.warn("[PREFAB] Sanitized %d item(s) with zero MaxDurability in %s", sanitizedItems, modRelativePath);
      return sanitizedPath;
    } catch (IOException e) {
      log.warn("[PREFAB] Failed to write sanitized prefab for %s: %s", modRelativePath, e.getMessage());
      return originalPath;
    }
  }

  /**
   * Spawns a dungeon tile into the world.
   * This includes the main tile prefab only (gates are off by default).
   * MUST be called from the world thread.
   * 
   * NOTE: If prefabs aren't aligning correctly, use PrefabMetadata to read
   * anchor points from the .prefab.json files and adjust placement accordingly.
   * 
   * @param tile   The tile to spawn
   * @param world  The world to spawn into
   * @param worldX World X coordinate for tile origin (grid position)
   * @param worldY World Y coordinate for tile origin
   * @param worldZ World Z coordinate for tile origin (grid position)
   */
  public void spawnTile(
      @Nonnull DungeonTile tile,
      @Nonnull World world,
      int worldX,
      int worldY,
      int worldZ) {
    spawnTile(tile, world, worldX, worldY, worldZ, false);
  }

  /**
   * Spawns a dungeon tile into the world.
   * This includes the main tile prefab and optionally gates.
   * MUST be called from the world thread.
   * 
   * @param tile       The tile to spawn
   * @param world      The world to spawn into
   * @param worldX     World X coordinate for tile origin (grid position)
   * @param worldY     World Y coordinate for tile origin
   * @param worldZ     World Z coordinate for tile origin (grid position)
   * @param spawnGates Whether to spawn gates for this tile
   */
  public void spawnTile(
      @Nonnull DungeonTile tile,
      @Nonnull World world,
      int worldX,
      int worldY,
      int worldZ,
      boolean spawnGates) {
    try {
      log.info("Spawning tile at world coords (%d, %d, %d): %s",
          worldX, worldY, worldZ, tile.getPrefabPath());

      PrefabInspector.PrefabDimensions tileDims = inspector.getPrefabDimensions(tile.getPrefabPath());
      int tileBaseY = worldY + ROOM_Y_OFFSET;
      int tileMinY = tileBaseY + tileDims.minY;
      int tileMaxY = tileBaseY + tileDims.maxY;
      if (!isWithinWorldBounds(tileMinY, tileMaxY)) {
        log.warn("Skipping tile %s at (%d, %d, %d) - Y bounds [%d,%d] exceed world limits [%d,%d]",
            tile.getPrefabPath(), worldX, worldY, worldZ, tileMinY, tileMaxY,
            config.getWorldMinY(), config.getWorldMaxY());
        return;
      }

      // Load the tile's prefab
      BlockSelection tilePrefab = loadPrefab(tile.getPrefabPath()).join();

      // Apply rotation based on tile rotation (Y-axis)
      BlockSelection rotatedPrefab = tilePrefab.cloneSelection()
          .rotate(Axis.Y, tile.getRotation());
      if (rotatedPrefab.getFluidCount() == 0 && tilePrefab.getFluidCount() > 0) {
        String jsonContent = prefabJsonCache.get(tile.getPrefabPath());
        if (jsonContent != null) {
          hydrateFluidsFromJson(rotatedPrefab, jsonContent, tile.getPrefabPath(), tile.getRotation());
        }
      }

      // Write prefab to world at the specified coordinates
      Vector3i tileOrigin = new Vector3i(worldX, tileBaseY, worldZ);
      PrefabPlaceContext placeContext = new PrefabPlaceContext(world, tile.getPrefabPath(), tileOrigin,
          tile.getRotation(), false, rotatedPrefab);
      for (PrefabHook hook : PrefabHookRegistry.getHooks()) {
        hook.beforePlace(placeContext);
      }

      rotatedPrefab.place(
          ConsoleSender.INSTANCE,
          world,
          tileOrigin,
          null,
          entityRef -> {
            if (entityRef != null) {
              for (PrefabHook hook : PrefabHookRegistry.getHooks()) {
                hook.onSpawnEntity(world, tile.getPrefabPath(), entityRef);
              }
            }
            unfreezeSpawnedEntity(world, entityRef);
          });

      for (PrefabHook hook : PrefabHookRegistry.getHooks()) {
        hook.afterPlace(placeContext);
      }
      spawnPrefabEntities(world, tile.getPrefabPath(), tileOrigin, tile.getRotation(),
          new RoomCoordinate(tile.getGridX(), tile.getGridZ()));

      if (spawnGates) {
        // Spawn gates: blocked gates always; interior gates only once per edge
        for (CardinalDirection direction : CardinalDirection.all()) {
          if (direction == null) {
            continue;
          }
          String gatePath = tile.getGate(direction);
          if (gatePath == null) {
            continue;
          }

          if (!shouldSpawnGate(tile, direction, gatePath)) {
            continue;
          }

          spawnGate(gatePath, direction, world, worldX, worldY, worldZ);
        }
      }

      log.info("Successfully spawned tile at (%d, %d, %d)", worldX, worldY, worldZ);

    } catch (Exception e) {
      log.error("Failed to spawn tile at (%d, %d, %d): %s",
          worldX, worldY, worldZ, e.getMessage());
      throw new RuntimeException("Failed to spawn tile", e);
    }
  }

  /**
   * Spawns a gate prefab at the specified location and direction.
   * MUST be called from the world thread.
   * 
   * @param gatePath   Full mod path to gate prefab
   * @param direction  Direction the gate faces (the wall it's on)
   * @param world      The world to spawn into
   * @param tileWorldX World X of the parent tile
   * @param tileWorldY World Y of the parent tile
   * @param tileWorldZ World Z of the parent tile
   */
  public void spawnGate(
      @Nonnull String gatePath,
      @Nonnull CardinalDirection direction,
      @Nonnull World world,
      int tileWorldX,
      int tileWorldY,
      int tileWorldZ) {
    try {
      log.info("Spawning gate %s facing %s", gatePath, direction);

      // Inspect the gate prefab to determine its dimensions
      PrefabInspector.PrefabDimensions gateDims = inspector.getPrefabDimensions(gatePath);

      // Calculate optimal placement based on gate dimensions
      int[] placement = calculateGatePlacement(direction, gateDims);
      int rotationDegrees = placement[0];
      int offsetX = placement[1];
      int offsetZ = placement[2];

      // Calculate gate position based on direction and inspected dimensions
      int gateX = tileWorldX + offsetX;
      int gateZ = tileWorldZ + offsetZ;
      int gateY = tileWorldY + GATE_Y_OFFSET - gateDims.minY;
      int gateMinY = gateY + gateDims.minY;
      int gateMaxY = gateY + gateDims.maxY;
      if (!isWithinWorldBounds(gateMinY, gateMaxY)) {
        log.warn("Skipping gate %s at (%d, %d, %d) - Y bounds [%d,%d] exceed world limits [%d,%d]",
            gatePath, gateX, gateY, gateZ, gateMinY, gateMaxY,
            config.getWorldMinY(), config.getWorldMaxY());
        return;
      }

      // Load and rotate gate prefab
      BlockSelection gatePrefab = loadPrefab(gatePath).join();

      BlockSelection rotatedGate = gatePrefab.cloneSelection()
          .rotate(Axis.Y, rotationDegrees);

      // Write gate to world
      Vector3i gateOrigin = new Vector3i(gateX, gateY, gateZ);
      PrefabPlaceContext gateContext = new PrefabPlaceContext(world, gatePath, gateOrigin, rotationDegrees, true,
          rotatedGate);
      for (PrefabHook hook : PrefabHookRegistry.getHooks()) {
        hook.beforePlace(gateContext);
      }

      rotatedGate.place(
          ConsoleSender.INSTANCE,
          world,
          gateOrigin,
          null,
          entityRef -> {
            log.info("Spawned entity in gate: %s", entityRef);
            if (entityRef != null) {
              for (PrefabHook hook : PrefabHookRegistry.getHooks()) {
                hook.onSpawnEntity(world, gatePath, entityRef);
              }
              unfreezeSpawnedEntity(world, entityRef);
            }
          });

      for (PrefabHook hook : PrefabHookRegistry.getHooks()) {
        hook.afterPlace(gateContext);
      }
      spawnPrefabEntities(world, gatePath, gateOrigin, rotationDegrees, null);

      log.info("Successfully spawned gate at (%d, %d, %d) facing %s with %d degree rotation (dims: %s)",
          gateX, gateY, gateZ, direction, rotationDegrees, gateDims);

    } catch (Exception e) {
      log.error("Failed to spawn gate %s: %s", gatePath, e.getMessage());
      throw new RuntimeException("Failed to spawn gate", e);
    }
  }

  private boolean isWithinWorldBounds(int minY, int maxY) {
    return minY >= config.getWorldMinY() && maxY <= config.getWorldMaxY();
  }

  @Nonnull
  private int[] calculateGatePlacement(@Nonnull CardinalDirection direction,
      @Nonnull PrefabInspector.PrefabDimensions dims) {
    int rotationDegrees = 0;
    int offsetX = 0;
    int offsetZ = 0;
    final int halfTile = config.getTileSize() / 2;
    final int edgeOffset = halfTile + config.getGateGap();

    boolean doorWiderInX = dims.isWiderInX();

    switch (direction) {
      case EAST -> {
        if (doorWiderInX) {
          rotationDegrees = 90;
        } else {
          rotationDegrees = 0;
        }
        offsetX = edgeOffset;
        offsetZ = 0;
      }
      case WEST -> {
        if (doorWiderInX) {
          rotationDegrees = 270;
        } else {
          rotationDegrees = 180;
        }
        offsetX = -edgeOffset;
        offsetZ = 0;
      }
      case SOUTH -> {
        if (doorWiderInX) {
          rotationDegrees = 0;
        } else {
          rotationDegrees = 90;
        }
        offsetX = 0;
        offsetZ = edgeOffset;
      }
      case NORTH -> {
        if (doorWiderInX) {
          rotationDegrees = 180;
        } else {
          rotationDegrees = 270;
        }
        offsetX = 0;
        offsetZ = -edgeOffset;
      }
    }

    log.info("Gate placement for %s: rotation=%d°, offset=(%d,%d), doorWiderInX=%s",
        direction, rotationDegrees, offsetX, offsetZ, doorWiderInX);

    return new int[] { rotationDegrees, offsetX, offsetZ };
  }

  public void clearCaches() {
    prefabCache.clear();
    inspector.clearCache();
  }

  @Nonnull
  public PrefabInspector.PrefabDimensions getPrefabDimensions(@Nonnull String prefabPath) {
    return inspector.getPrefabDimensions(prefabPath);
  }

  @Nullable
  public String getPrefabJson(@Nonnull String prefabPath) {
    String cached = prefabJsonCache.get(prefabPath);
    if (cached != null) {
      return cached;
    }
    String prefabEntryPath = "Server/Prefabs/" + prefabPath + ".prefab.json";
    Path prefabPathResolved = resolveAssetPrefab(prefabEntryPath, prefabPath);
    if (prefabPathResolved == null || !Files.exists(prefabPathResolved)) {
      return null;
    }
    StringBuilder jsonBuilder = new StringBuilder();
    try (BufferedReader reader = Files.newBufferedReader(prefabPathResolved, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        jsonBuilder.append(line).append('\n');
      }
    } catch (IOException e) {
      log.warn("Failed to read prefab JSON for %s: %s", prefabPath, e.getMessage());
      return null;
    }
    String json = jsonBuilder.toString();
    if (!json.isBlank()) {
      prefabJsonCache.put(prefabPath, json);
    }
    return json;
  }

  private void unfreezeSpawnedEntity(@Nonnull World world, @Nullable Ref<EntityStore> entityRef) {
    if (entityRef == null) {
      return;
    }
    attemptUnfreeze(world, entityRef, 3);
  }

  private void attemptUnfreeze(@Nonnull World world, @Nonnull Ref<EntityStore> entityRef, int remainingAttempts) {
    boolean success = PrefabEntityUtils.tryUnfreezePrefabEntity(entityRef, log.getLogger());
    if (success || remainingAttempts <= 0) {
      return;
    }
    world.execute(() -> attemptUnfreeze(world, entityRef, remainingAttempts - 1));
  }

  private void spawnPrefabEntities(@Nonnull World world, @Nonnull String prefabPath, @Nonnull Vector3i origin,
      int rotationDegrees, @Nullable RoomCoordinate room) {
    List<PrefabEntityDefinition> entities = prefabEntityCache.get(prefabPath);
    if (entities == null) {
      log.fine("No prefab entities cached for %s", prefabPath);
      return;
    }
    if (entities.isEmpty()) {
      log.fine("Prefab %s has no entities to spawn", prefabPath);
      return;
    }
    log.info("[PREFAB-ENTITIES] Spawning %d entities from %s at %s", entities.size(), prefabPath, origin);
      NPCPlugin npcPlugin = NPCPlugin.get();
      if (npcPlugin == null) {
        log.warn("[PREFAB-ENTITIES] NPCPlugin not available - cannot spawn entities");
        return;
      }
      for (PrefabEntityDefinition def : entities) {
        if (def == null) {
          continue;
        }
        String modelId = def.getModelId();
      if (!npcPlugin.hasRoleName(modelId)) {
        log.warn("[PREFAB-ENTITIES] Skipping NPC %s (no role registered)", modelId);
        continue;
      }
      Vector3d rotated = rotatePosition(def.getPosition(), rotationDegrees);
      Vector3d worldPos = new Vector3d(origin.x + rotated.x, origin.y + rotated.y, origin.z + rotated.z);
      Vector3f rotation = rotateRotation(def.getRotation(), rotationDegrees);
      log.info("[PREFAB-ENTITIES] Spawning %s at (%.1f, %.1f, %.1f)", modelId, worldPos.x, worldPos.y, worldPos.z);
      maybeEmitChestSpawned(world, prefabPath, modelId, worldPos);
      NpcSpawnRequestedEvent request = new NpcSpawnRequestedEvent(
          world,
          room,
          modelId,
          modelId,
          worldPos,
          Objects.requireNonNull(rotation, "rotation"),
          prefabPath);
      WorldEventQueue.get().dispatch(world, request);
      NpcSpawnResult result = request.getResult().getNow(null);
      if (result != null && result.isSuccess() && result.getEntityId() != null) {
        UUID uuid = result.getEntityId();
        log.info("[PREFAB-ENTITIES] Successfully spawned %s", modelId);
        WorldEventQueue.get().dispatch(world,
            new PrefabEntitySpawnedEvent(world, modelId, worldPos, prefabPath));

        if (room != null) {
          int defaultPoints = getDefaultPointsForEntity(modelId);
          log.info("[PREFAB-ENTITIES] Dispatching EntitySpawnedEvent for %s (%s) in room (%d, %d) with %d points",
              modelId, uuid, room.getX(), room.getZ(), defaultPoints);
          WorldEventQueue.get().dispatch(world,
              new EntitySpawnedEvent(world, uuid, room, modelId, defaultPoints, worldPos));
        } else {
          log.fine("[PREFAB-ENTITIES] Spawned %s but room is null - no EntitySpawnedEvent", modelId);
        }
      } else {
        log.warn("[PREFAB-ENTITIES] Failed to spawn %s at (%.1f, %.1f, %.1f)", modelId, worldPos.x, worldPos.y,
            worldPos.z);
      }
    }
  }

  private void maybeEmitChestSpawned(@Nonnull World world, @Nonnull String prefabPath, @Nonnull String modelId,
      @Nonnull Vector3d worldPos) {
    String lower = modelId.toLowerCase();
    if (!lower.contains("furniture_")) {
      return;
    }
    // Emit LootableEntitySpawned only for chests
    if (lower.contains("chest")) {
      WorldEventQueue.get().dispatch(world,
          new LootableEntitySpawnedEvent(world, worldPos, modelId, prefabPath));
      // Also emit legacy ChestSpawned for backward compatibility
      WorldEventQueue.get().dispatch(world,
          new ChestSpawnedEvent(world, worldPos, modelId, prefabPath));
    } else {
      // Emit EntityReplaced for other furniture (torches, doors, etc.)
      WorldEventQueue.get().dispatch(world,
          new EntityReplacedEvent(world, worldPos, modelId, prefabPath));
    }
  }

  private Vector3d rotatePosition(@Nonnull Vector3d position, int rotationDegrees) {
    int normalized = ((rotationDegrees % 360) + 360) % 360;
    double x = position.x;
    double z = position.z;
    return switch (normalized) {
      case 90 -> new Vector3d(-z, position.y, x);
      case 180 -> new Vector3d(-x, position.y, -z);
      case 270 -> new Vector3d(z, position.y, -x);
      default -> new Vector3d(x, position.y, z);
    };
  }

  private Vector3f rotateRotation(@Nonnull Vector3f rotation, int rotationDegrees) {
    int normalized = ((rotationDegrees % 360) + 360) % 360;
    float yaw = rotation.y + (float) Math.toRadians(normalized);
    return new Vector3f(rotation.x, yaw, rotation.z);
  }

  private boolean shouldSpawnGate(
      @Nonnull DungeonTile tile,
      @Nonnull CardinalDirection direction,
      @Nonnull String gatePath) {
    if (gatePath.contains("Blocked")) {
      return true;
    }

    if (tile.getType() == DungeonTile.TileType.BASE || tile.getType() == DungeonTile.TileType.BASE_CORNER) {
      return true;
    }

    // Spawn interior gates only once to avoid duplicates
    return direction == CardinalDirection.EAST || direction == CardinalDirection.SOUTH;
  }

  /**
   * Calculates the X offset for a gate based on direction.
   * Gates connect tiles in a 20-block grid (19 blocks + 1 gate).
   * 
   * @param direction Gate direction
   * @param tileSize  Size of the tile (19)
   * @return X offset from tile origin
   */
  @SuppressWarnings("unused")
  private int calculateGateOffsetX(@Nonnull CardinalDirection direction, int tileSize) {
    // Gates are placed at the edges to connect 20-block spaced tiles
    // Gates fill the 1-block gap between tiles (after the 19-block tile)
    return switch (direction) {
      case EAST -> tileSize; // Right edge (at 19, the 1-block gap)
      case WEST -> -1; // Left edge (extends into the tile to the west)
      case NORTH, SOUTH -> tileSize / 2; // Center for N/S walls
    };
  }

  /**
   * Calculates the Z offset for a gate based on direction.
   * Gates connect tiles in a 20-block grid (19 blocks + 1 gate).
   * 
   * @param direction Gate direction
   * @param tileSize  Size of the tile (19)
   * @return Z offset from tile origin
   */
  @SuppressWarnings("unused")
  private int calculateGateOffsetZ(@Nonnull CardinalDirection direction, int tileSize) {
    // Gates are placed at the edges to connect 20-block spaced tiles
    // Gates fill the 1-block gap between tiles (after the 19-block tile)
    return switch (direction) {
      case NORTH -> -1; // Front edge (extends into the tile to the north)
      case SOUTH -> tileSize; // Back edge (at 19, the 1-block gap)
      case EAST, WEST -> tileSize / 2; // Center for E/W walls
    };
  }

  /**
   * Get default points for entity type spawned from prefabs.
   * TODO: Look up actual points from spawn pool configuration
   */
  private int getDefaultPointsForEntity(String entityType) {
    if (entityType == null) {
      return 5; // Unknown entity
    }
    // Default point values for common enemy types
    if (entityType.contains("Archer")) {
      return 8;
    } else if (entityType.contains("Alchemist")) {
      return 10;
    } else if (entityType.contains("Skeleton")) {
      return 7;
    } else if (entityType.contains("Ghoul")) {
      return 12;
    } else if (entityType.contains("Bunny")) {
      return 3;
    } else {
      return 5; // Default for unrecognized types
    }
  }

  /**
   * Exception thrown when prefab loading fails.
   */
  public static class PrefabLoadException extends RuntimeException {
    public PrefabLoadException(String message) {
      super(message);
    }

    public PrefabLoadException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
