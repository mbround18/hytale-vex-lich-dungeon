package MBRound18.hytale.vexlichdungeon.portal;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.data.PortalPlacementRecord;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PortalSnapshotUtil {
  private static final LoggingHelper LOG = new LoggingHelper("PortalSnapshotUtil");
  private static final int PORTAL_MARKER_OFFSET_X = 0;
  private static final int PORTAL_MARKER_OFFSET_Y = 1;
  private static final int PORTAL_MARKER_OFFSET_Z = 0;

  private PortalSnapshotUtil() {
  }

  public static void restore(@Nonnull World world, @Nonnull PortalPlacementRecord record) {
    int sizeX = record.getSizeX();
    int sizeY = record.getSizeY();
    int sizeZ = record.getSizeZ();
    int[] snapshotBlocks = record.getSnapshotBlocks();
    clearBounds(world, record.getMinX(), record.getMaxX(), record.getMinY(), record.getMaxY(),
        record.getMinZ(), record.getMaxZ());
    clearPortalMapMarker(world, record, snapshotBlocks, sizeX, sizeY, sizeZ);
    if (snapshotBlocks == null || snapshotBlocks.length == 0 || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
      LOG.warn("[PORTAL] Missing snapshot for portal %s in world %s; clearing bounds only",
          record.getPortalId(), record.getWorldName());
      return;
    }

    int minX = record.getMinX();
    int minY = record.getMinY();
    int minZ = record.getMinZ();
    int maxX = record.getMaxX();
    int maxY = record.getMaxY();
    int maxZ = record.getMaxZ();
    int expected = sizeX * sizeY * sizeZ;
    if (snapshotBlocks.length != expected) {
      LOG.warn("Snapshot size mismatch (expected %d, got %d). Clearing instead.", expected,
          snapshotBlocks.length);
      return;
    }

    BlockSelection restoreSelection = new BlockSelection();

    LOG.info("[PORTAL] Restoring snapshot for portal %s in world %s", record.getPortalId(),
        record.getWorldName());

    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        for (int y = minY; y <= maxY; y++) {
          int idx = index(x - minX, y - minY, z - minZ, sizeX, sizeY, sizeZ);
          int blockId = snapshotBlocks[idx];
          if (blockId == 0) {
            restoreSelection.addEmptyAtWorldPos(x, y, z);
          } else {
            restoreSelection.addBlockAtWorldPos(x, y, z, blockId, 0, 0, 0);
          }
        }
      }
    }

    try {
      restoreSelection.placeNoReturn("VexPortalRestore", ConsoleSender.INSTANCE, world, null);
    } catch (Exception e) {
      LOG.warn("[PORTAL] Failed to place restore selection: %s", e.getMessage());
    }

    clearPortalMapMarker(world, record, snapshotBlocks, sizeX, sizeY, sizeZ);
  }

  public static void clearBounds(@Nonnull World world, int minX, int maxX, int minY, int maxY,
      int minZ, int maxZ) {
    BlockSelection clearSelection = new BlockSelection();
    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        for (int y = minY; y <= maxY; y++) {
          clearSelection.addEmptyAtWorldPos(x, y, z);
        }
      }
    }

    try {
      clearSelection.placeNoReturn("VexPortalClear", ConsoleSender.INSTANCE, world, null);
    } catch (Exception e) {
      LOG.warn("[PORTAL] Failed to place clear selection: %s", e.getMessage());
    }
  }

  @Nonnull
  public static Snapshot captureSnapshot(@Nonnull World world, int minX, int maxX, int minY, int maxY,
      int minZ, int maxZ) {
    int sizeX = maxX - minX + 1;
    int sizeY = maxY - minY + 1;
    int sizeZ = maxZ - minZ + 1;
    int volume = sizeX * sizeY * sizeZ;
    int[] snapshotBlocks = new int[volume];

    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        long chunkKey = (((long) (x >> 4)) << 32) | (((long) (z >> 4)) & 0xFFFFFFFFL);
        BlockAccessor chunk = world.getChunkIfLoaded(chunkKey);
        if (chunk == null) {
          continue;
        }
        for (int y = minY; y <= maxY; y++) {
          int idx = index(x - minX, y - minY, z - minZ, sizeX, sizeY, sizeZ);
          snapshotBlocks[idx] = chunk.getBlock(x, y, z);
        }
      }
    }
    return new Snapshot(sizeX, sizeY, sizeZ, snapshotBlocks);
  }

  public static CompletableFuture<Snapshot> captureSnapshotAsync(@Nonnull World world, int minX, int maxX,
      int minY, int maxY, int minZ, int maxZ) {
    int sizeX = maxX - minX + 1;
    int sizeY = maxY - minY + 1;
    int sizeZ = maxZ - minZ + 1;
    int volume = sizeX * sizeY * sizeZ;
    int[] snapshotBlocks = new int[volume];

    List<CompletableFuture<Void>> pending = new ArrayList<>();
    int chunkMinX = minX >> 4;
    int chunkMaxX = maxX >> 4;
    int chunkMinZ = minZ >> 4;
    int chunkMaxZ = maxZ >> 4;

    for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
      for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
        final int chunkX = cx;
        final int chunkZ = cz;
        long chunkKey = (((long) chunkX) << 32) | (((long) chunkZ) & 0xFFFFFFFFL);
        BlockAccessor chunk = world.getChunkIfLoaded(chunkKey);
        if (chunk != null) {
          fillChunk(snapshotBlocks, chunk, chunkX, chunkZ, minX, maxX, minY, maxY, minZ, maxZ, sizeX, sizeY,
              sizeZ);
          continue;
        }

        CompletableFuture<Void> loadFuture = world.getChunkAsync(chunkKey)
            .thenCompose(loaded -> {
              if (loaded == null) {
                return CompletableFuture.completedFuture(null);
              }
              CompletableFuture<Void> exec = new CompletableFuture<>();
              world.execute(() -> {
                fillChunk(snapshotBlocks, loaded, chunkX, chunkZ, minX, maxX, minY, maxY, minZ, maxZ, sizeX,
                    sizeY,
                    sizeZ);
                exec.complete(null);
              });
              return exec;
            });
        pending.add(loadFuture);
      }
    }

    if (pending.isEmpty()) {
      return CompletableFuture.completedFuture(new Snapshot(sizeX, sizeY, sizeZ, snapshotBlocks));
    }

    return CompletableFuture.allOf(pending.toArray(new CompletableFuture[0]))
        .thenApply(ignored -> new Snapshot(sizeX, sizeY, sizeZ, snapshotBlocks));
  }

  private static void fillChunk(int[] snapshotBlocks, BlockAccessor chunk, int chunkX, int chunkZ,
      int minX, int maxX, int minY, int maxY, int minZ, int maxZ, int sizeX, int sizeY, int sizeZ) {
    int startX = Math.max(minX, chunkX << 4);
    int endX = Math.min(maxX, (chunkX << 4) + 15);
    int startZ = Math.max(minZ, chunkZ << 4);
    int endZ = Math.min(maxZ, (chunkZ << 4) + 15);
    for (int x = startX; x <= endX; x++) {
      for (int z = startZ; z <= endZ; z++) {
        for (int y = minY; y <= maxY; y++) {
          int idx = index(x - minX, y - minY, z - minZ, sizeX, sizeY, sizeZ);
          snapshotBlocks[idx] = chunk.getBlock(x, y, z);
        }
      }
    }
  }

  private static int index(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
    return (x * sizeZ * sizeY) + (z * sizeY) + y;
  }

  private static void clearPortalMapMarker(@Nonnull World world, @Nonnull PortalPlacementRecord record,
      @Nullable int[] snapshotBlocks, int sizeX, int sizeY, int sizeZ) {
    int markerX = record.getX() + PORTAL_MARKER_OFFSET_X;
    int markerY = record.getY() + PORTAL_MARKER_OFFSET_Y;
    int markerZ = record.getZ() + PORTAL_MARKER_OFFSET_Z;

    if (markerX < record.getMinX() || markerX > record.getMaxX()
        || markerY < record.getMinY() || markerY > record.getMaxY()
        || markerZ < record.getMinZ() || markerZ > record.getMaxZ()) {
      return;
    }

    int blockId = 0;
    if (snapshotBlocks != null && snapshotBlocks.length > 0 && sizeX > 0 && sizeY > 0 && sizeZ > 0) {
      int expected = sizeX * sizeY * sizeZ;
      if (snapshotBlocks.length == expected) {
        int idx = index(markerX - record.getMinX(), markerY - record.getMinY(),
            markerZ - record.getMinZ(), sizeX, sizeY, sizeZ);
        if (idx >= 0 && idx < snapshotBlocks.length) {
          blockId = snapshotBlocks[idx];
        }
      }
    }

    resetBlock(world, markerX, markerY, markerZ, blockId);
  }

  private static void resetBlock(@Nonnull World world, int x, int y, int z, int blockId) {
    long chunkKey = (((long) (x >> 4)) << 32) | (((long) (z >> 4)) & 0xFFFFFFFFL);
    BlockAccessor chunk = world.getChunkIfLoaded(chunkKey);
    if (chunk != null) {
      chunk.setBlock(x, y, z, 0);
      if (blockId != 0) {
        chunk.setBlock(x, y, z, blockId);
      }
      return;
    }

    world.getChunkAsync(chunkKey).thenAccept(loaded -> {
      if (loaded == null) {
        return;
      }
      world.execute(() -> {
        loaded.setBlock(x, y, z, 0);
        if (blockId != 0) {
          loaded.setBlock(x, y, z, blockId);
        }
      });
    });
  }

  public static final class Snapshot {
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    @Nonnull
    private final int[] blocks;

    private Snapshot(int sizeX, int sizeY, int sizeZ, @Nonnull int[] blocks) {
      this.sizeX = sizeX;
      this.sizeY = sizeY;
      this.sizeZ = sizeZ;
      this.blocks = blocks;
    }

    public int getSizeX() {
      return sizeX;
    }

    public int getSizeY() {
      return sizeY;
    }

    public int getSizeZ() {
      return sizeZ;
    }

    @Nonnull
    public int[] getBlocks() {
      return blocks;
    }
  }
}
