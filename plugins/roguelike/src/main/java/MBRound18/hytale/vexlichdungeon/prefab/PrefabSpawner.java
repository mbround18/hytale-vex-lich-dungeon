package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.vexlichdungeon.dungeon.CardinalDirection;
import MBRound18.hytale.vexlichdungeon.dungeon.DungeonTile;
import MBRound18.hytale.vexlichdungeon.dungeon.GenerationConfig;
import MBRound18.hytale.shared.utilities.LoggingHelper;
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
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
  private final ZipFile zipFile;
  private final PrefabInspector inspector;
  private final GenerationConfig config;
  private final Map<String, SoftReference<BlockSelection>> prefabCache;
  private final Map<String, List<PrefabEntityDefinition>> prefabEntityCache = new ConcurrentHashMap<>();
  private final Map<String, String> prefabJsonCache = new ConcurrentHashMap<>();

  /**
   * Creates a new prefab spawner.
   * 
   * @param log     Logger for spawning events
   * @param zipFile The asset ZIP file containing prefabs
   */
  public PrefabSpawner(@Nonnull LoggingHelper log, @Nonnull ZipFile zipFile, @Nonnull GenerationConfig config) {
    this(log, zipFile, config, null);
  }

  public PrefabSpawner(@Nonnull LoggingHelper log, @Nonnull ZipFile zipFile, @Nonnull GenerationConfig config,
      @Nullable Path unpackedRoot) {
    this.log = log;
    this.zipFile = zipFile;
    this.config = config;
    this.inspector = new PrefabInspector(log, zipFile, config.getTileSize(), config.getGateGap(), unpackedRoot);
    this.prefabCache = Collections.synchronizedMap(new LinkedHashMap<>(MAX_PREFAB_CACHE, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, SoftReference<BlockSelection>> eldest) {
        return size() > MAX_PREFAB_CACHE;
      }
    });
  }

  /**
   * Loads a prefab from the mod's asset pack ZIP.
   * 
   * @param modRelativePath Path relative to Server/Prefabs/ (e.g.,
   *                        "Rooms/Vex_Room_S_Lava_B")
   * @return CompletableFuture with the loaded BlockSelection
   */
  @Nonnull
  public CompletableFuture<BlockSelection> loadPrefab(@Nonnull String modRelativePath) {
    return Objects.requireNonNull(CompletableFuture.supplyAsync(() -> {
      Path tempFile = null;
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

        // Load prefab JSON from ZIP file
        // Path within ZIP: Server/Prefabs/Rooms/Vex_Room_S_Lava_B.prefab.json
        String zipEntryPath = "Server/Prefabs/" + modRelativePath + ".prefab.json";
        ZipEntry entry = zipFile.getEntry(zipEntryPath);

        if (entry == null) {
          throw new PrefabLoadException("Prefab file not found in ZIP at: " + zipEntryPath);
        }

        // Extract ZIP entry into memory
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
          String line;
          while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line).append('\n');
          }
        }

        String jsonContent = Objects.requireNonNull(jsonBuilder.toString(), "jsonContent");
        JsonObject root = Objects.requireNonNull(JsonParser.parseString(jsonContent).getAsJsonObject(), "root");
        List<PrefabEntityDefinition> entities = extractPrefabEntities(root, modRelativePath);
        prefabEntityCache.put(modRelativePath, entities);
        prefabJsonCache.put(modRelativePath, jsonContent);

        // Create a temporary file to store the sanitized JSON
        tempFile = Files.createTempFile("prefab_", ".json");
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(Files.newOutputStream(tempFile), StandardCharsets.UTF_8))) {
          writer.write(root.toString());
        }

        log.info("Extracted prefab JSON to temporary file: %s", tempFile);

        // Use PrefabStore to deserialize the BlockSelection from the JSON file
        BlockSelection prefab = PrefabStore.get().getPrefab(Objects.requireNonNull(tempFile, "tempFile"));

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
        // Clean up temporary file
        if (tempFile != null) {
          try {
            Files.deleteIfExists(tempFile);
          } catch (Exception e) {
            log.warn("Failed to delete temporary prefab file: %s", e.getMessage());
          }
        }
      }
    }), "prefabFuture");
  }

  private List<PrefabEntityDefinition> extractPrefabEntities(@Nonnull JsonObject root, @Nonnull String prefabPath) {
    try {
      JsonArray entities = root.getAsJsonArray("entities");
      if (entities == null || entities.isEmpty()) {
        return List.of();
      }
      List<PrefabEntityDefinition> definitions = new ArrayList<>();
      JsonArray keptEntities = new JsonArray();

      for (JsonElement element : entities) {
        if (!element.isJsonObject()) {
          keptEntities.add(element);
          continue;
        }
        JsonObject entity = element.getAsJsonObject();
        JsonObject components = entity.getAsJsonObject("Components");
        if (components == null) {
          keptEntities.add(entity);
          continue;
        }
        if (components.has("BlockEntity")) {
          keptEntities.add(entity);
          continue;
        }

        JsonObject modelWrapper = components.getAsJsonObject("Model");
        if (modelWrapper == null) {
          keptEntities.add(entity);
          continue;
        }
        JsonObject model = modelWrapper.getAsJsonObject("Model");
        if (model == null || !model.has("Id")) {
          keptEntities.add(entity);
          continue;
        }
        String modelId = Objects.requireNonNull(model.get("Id").getAsString(), "modelId");

        JsonObject transform = components.getAsJsonObject("Transform");
        if (transform == null) {
          keptEntities.add(entity);
          continue;
        }
        JsonObject position = transform.getAsJsonObject("Position");
        JsonObject rotation = transform.getAsJsonObject("Rotation");
        if (position == null || rotation == null) {
          keptEntities.add(entity);
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
      }

      if (keptEntities.size() > 0) {
        root.add("entities", keptEntities);
      } else {
        root.remove("entities");
      }

      if (!definitions.isEmpty()) {
        log.info("Extracted %d prefab entities from %s", definitions.size(), prefabPath);
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
      log.info("[FLUIDS] %s base fluids=%d", tile.getPrefabPath(), tilePrefab.getFluidCount());

      // Apply rotation based on tile rotation (Y-axis)
      BlockSelection rotatedPrefab = tilePrefab.cloneSelection()
          .rotate(Axis.Y, tile.getRotation());
      log.info("[FLUIDS] %s rotated(%d) fluids=%d", tile.getPrefabPath(), tile.getRotation(),
          rotatedPrefab.getFluidCount());
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

      log.info("[FLUIDS] %s placed fluids=%d", tile.getPrefabPath(), rotatedPrefab.getFluidCount());

      for (PrefabHook hook : PrefabHookRegistry.getHooks()) {
        hook.afterPlace(placeContext);
      }
      spawnPrefabEntities(world, tile.getPrefabPath(), tileOrigin, tile.getRotation());

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
      int[] placement = inspector.calculateGatePlacement(direction, gateDims);
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
      spawnPrefabEntities(world, gatePath, gateOrigin, rotationDegrees);

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

  public void clearCaches() {
    prefabCache.clear();
    inspector.clearCache();
  }

  @Nonnull
  public PrefabInspector.PrefabDimensions getPrefabDimensions(@Nonnull String prefabPath) {
    return inspector.getPrefabDimensions(prefabPath);
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
      int rotationDegrees) {
    List<PrefabEntityDefinition> entities = prefabEntityCache.get(prefabPath);
    if (entities == null || entities.isEmpty()) {
      return;
    }
    NPCPlugin npcPlugin = NPCPlugin.get();
    if (npcPlugin == null) {
      return;
    }
    for (PrefabEntityDefinition def : entities) {
      if (def == null) {
        continue;
      }
      String modelId = def.getModelId();
      if (!npcPlugin.hasRoleName(modelId)) {
        log.warn("Skipping prefab NPC %s (no role registered)", modelId);
        continue;
      }
      Vector3d rotated = rotatePosition(def.getPosition(), rotationDegrees);
      Vector3d worldPos = new Vector3d(origin.x + rotated.x, origin.y + rotated.y, origin.z + rotated.z);
      Vector3f rotation = rotateRotation(def.getRotation(), rotationDegrees);
      npcPlugin.spawnNPC(
          Objects.requireNonNull(world.getEntityStore().getStore(), "store"),
          modelId,
          modelId,
          worldPos,
          Objects.requireNonNull(rotation, "rotation"));
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
