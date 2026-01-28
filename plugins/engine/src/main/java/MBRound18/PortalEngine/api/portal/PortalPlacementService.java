package MBRound18.PortalEngine.api.portal;

import MBRound18.PortalEngine.api.logging.EngineLog;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Handles locating and placing a portal block within a world.
 *
 * <p>This class centralizes the "find a safe spot" logic and the actual
 * block placement so that game logic can remain focused on flow control.</p>
 */
public final class PortalPlacementService {
  private final PortalPlacementPlanner planner = new PortalPlacementPlanner();
  private final PortalPlacementConfig config;
  private final PortalBlockResolver blockResolver;

  public PortalPlacementService(PortalPlacementConfig config, PortalBlockResolver blockResolver) {
    this.config = Objects.requireNonNull(config, "config");
    this.blockResolver = Objects.requireNonNull(blockResolver, "blockResolver");
  }

  /**
   * Attempts to locate and place a portal near the provided position.
   *
   * <p>Failures are reported via {@link PortalPlacementResult} to allow
   * command/UI callers to choose appropriate messages.</p>
   */
  public PortalPlacementResult placePortal(@Nullable World world, @Nullable Vector3d position,
      @Nullable Vector3f rotation, @Nullable EngineLog log) {
    if (world == null || position == null || rotation == null) {
      return PortalPlacementResult.failure(PortalPlacementFailure.INVALID_INPUT, null,
          "world/position/rotation missing");
    }
    if (!config.isValid()) {
      return PortalPlacementResult.failure(PortalPlacementFailure.INVALID_INPUT, null,
          "invalid placement config");
    }

    Optional<Vector3i> portalPos = planner.findPlacement(position, rotation, config,
        (x, y, z) -> isSafePortalSpot(world, x, y, z));
    if (portalPos.isEmpty()) {
      return PortalPlacementResult.failure(PortalPlacementFailure.NO_SAFE_SPOT, null, null);
    }

    Vector3i pos = portalPos.get();
    BlockType block = blockResolver.resolve(log);
    PlacementAttempt attempt = placePortalBlock(world, pos, block, log);
    if (!attempt.placed) {
      PortalPlacementFailure failure = block == null
          ? PortalPlacementFailure.BLOCK_NOT_FOUND
          : PortalPlacementFailure.PLACE_FAILED;
      return PortalPlacementResult.failure(failure, pos, attempt.detail);
    }
    int placedId = resolveBlockId(world, pos);
    BlockType placedType = placedId != 0 ? BlockType.getAssetMap().getAsset(placedId) : null;
    String placedName = placedType != null ? placedType.getId() : (block != null ? block.getId() : null);
    return PortalPlacementResult.success(pos, placedName, placedId);
  }

  private boolean isSafePortalSpot(World world, int x, int y, int z) {
    BlockAccessor chunk = getChunkIfLoaded(world, x, z);
    if (chunk == null) {
      return false;
    }
    int below = chunk.getBlock(x, y - 1, z);
    if (below == 0 || isReplaceable(chunk, x, y - 1, z)) {
      return false;
    }
    return isReplaceable(chunk, x, y, z)
        && isReplaceable(chunk, x, y + 1, z)
        && isReplaceable(chunk, x, y + 2, z);
  }

  private boolean isReplaceable(BlockAccessor chunk, int x, int y, int z) {
    int id = chunk.getBlock(x, y, z);
    if (id == 0 || id == BlockType.EMPTY_ID || id == BlockType.UNKNOWN_ID) {
      return true;
    }
    BlockType block = BlockType.getAssetMap().getAsset(id);
    if (block == null) {
      return false;
    }
    if (block.getMaterial() == BlockMaterial.Empty || block.getDrawType() == DrawType.Empty) {
      return true;
    }
    String key = block.getId() != null ? block.getId().toLowerCase() : "";
    return key.contains("grass")
        || key.contains("plant")
        || key.contains("flower")
        || key.contains("mushroom")
        || key.contains("fern")
        || key.contains("bush")
        || key.contains("leaves")
        || key.contains("sapling");
  }

  private PlacementAttempt placePortalBlock(World world, Vector3i pos, @Nullable BlockType block,
      @Nullable EngineLog log) {
    BlockAccessor chunk = getChunkIfLoaded(world, pos.x, pos.z);
    if (chunk == null) {
      if (log != null) {
        log.warn("[PORTAL] Chunk not loaded for portal placement at (%d,%d,%d)", pos.x, pos.y, pos.z);
      }
      return new PlacementAttempt(false, "chunk not loaded");
    }

    BlockType placedBlock = block;
    boolean placed = false;
    if (block != null) {
      String defaultState = block.getDefaultStateKey();
      if (defaultState != null && !defaultState.isBlank()) {
        BlockType stateBlock = block.getBlockForState(defaultState);
        if (stateBlock != null) {
          placedBlock = stateBlock;
        }
      }
      placed = chunk.setBlock(pos.x, pos.y, pos.z, placedBlock);
    }

    if (!placed) {
      for (String candidate : blockResolver.getCandidates()) {
        if (chunk.setBlock(pos.x, pos.y, pos.z, candidate)) {
          placed = true;
          break;
        }
      }
    }

    int placedId = chunk.getBlock(pos.x, pos.y, pos.z);
    if (!placed || placedId == BlockType.EMPTY_ID || placedId == BlockType.UNKNOWN_ID) {
      if (log != null) {
        log.warn("[PORTAL] Portal placement failed at (%d,%d,%d) using block %s (id=%d, placed=%s)",
            pos.x, pos.y, pos.z, placedBlock != null ? placedBlock.getId() : "null", placedId, placed);
      }
      return new PlacementAttempt(false, "block placement rejected");
    }

    if (log != null) {
      BlockType placedType = BlockType.getAssetMap().getAsset(placedId);
      log.info("[PORTAL] Portal device placed at (%d,%d,%d) (id=%d, type=%s)",
          pos.x, pos.y, pos.z, placedId, placedType != null ? placedType.getId() : "unknown");
    }
    return new PlacementAttempt(true, null);
  }

  @Nullable
  private BlockAccessor getChunkIfLoaded(World world, int x, int z) {
    long chunkKey = (((long) (x >> 4)) << 32) | (((long) (z >> 4)) & 0xFFFFFFFFL);
    return world.getChunkIfLoaded(chunkKey);
  }

  private int resolveBlockId(World world, Vector3i pos) {
    BlockAccessor chunk = getChunkIfLoaded(world, pos.x, pos.z);
    if (chunk == null) {
      return 0;
    }
    return chunk.getBlock(pos.x, pos.y, pos.z);
  }

  private static final class PlacementAttempt {
    private final boolean placed;
    private final String detail;

    private PlacementAttempt(boolean placed, @Nullable String detail) {
      this.placed = placed;
      this.detail = detail;
    }
  }
}
