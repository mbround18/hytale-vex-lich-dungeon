package MBRound18.hytale.vexlichdungeon.portal;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.data.PortalPlacementRecord;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import javax.annotation.Nonnull;

public final class PortalSnapshotUtil {
  private static final LoggingHelper LOG = new LoggingHelper("PortalSnapshotUtil");

  private PortalSnapshotUtil() {
  }

  public static void restore(@Nonnull World world, @Nonnull PortalPlacementRecord record) {
    int sizeX = record.getSizeX();
    int sizeY = record.getSizeY();
    int sizeZ = record.getSizeZ();
    int[] snapshotBlocks = record.getSnapshotBlocks();
    clearBounds(world, record.getMinX(), record.getMaxX(), record.getMinY(), record.getMaxY(),
        record.getMinZ(), record.getMaxZ());
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
        long chunkKey = (((long) (x >> 4)) << 32) | (((long) (z >> 4)) & 0xFFFFFFFFL);
        BlockAccessor chunk = world.getChunkIfLoaded(chunkKey);
        if (chunk != null) {
          for (int y = minY; y <= maxY; y++) {
            int idx = index(x - minX, y - minY, z - minZ, sizeX, sizeY, sizeZ);
            int blockId = snapshotBlocks[idx];
            if (blockId == 0) {
              restoreSelection.addEmptyAtWorldPos(x, y, z);
            } else {
              restoreSelection.addBlockAtWorldPos(x, y, z, blockId, 0, 0, 0);
            }
          }
          continue;
        }
        final int blockX = x;
        final int blockZ = z;
        world.getChunkAsync(chunkKey).thenAccept(loaded -> {
          if (loaded == null) {
            return;
          }
          world.execute(() -> {
            for (int y = minY; y <= maxY; y++) {
              int idx = index(blockX - minX, y - minY, blockZ - minZ, sizeX, sizeY, sizeZ);
              int blockId = snapshotBlocks[idx];
              if (blockId == 0) {
                restoreSelection.addEmptyAtWorldPos(blockX, y, blockZ);
              } else {
                restoreSelection.addBlockAtWorldPos(blockX, y, blockZ, blockId, 0, 0, 0);
              }
            }
          });
        });
      }
    }

    try {
      restoreSelection.placeNoReturn("VexPortalRestore", ConsoleSender.INSTANCE, world, null);
    } catch (Exception e) {
      LOG.warn("[PORTAL] Failed to place restore selection: %s", e.getMessage());
    }
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

  private static int index(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
    return (x * sizeZ * sizeY) + (z * sizeY) + y;
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
