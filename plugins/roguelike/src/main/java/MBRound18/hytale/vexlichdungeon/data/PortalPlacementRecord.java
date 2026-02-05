package MBRound18.hytale.vexlichdungeon.data;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Persistent record of a placed portal instance.
 */
public class PortalPlacementRecord implements Serializable {
  private static final long serialVersionUID = 1L;
  private UUID portalId;
  private UUID worldUuid;
  private String worldName;
  private int x;
  private int y;
  private int z;
  private int minX;
  private int maxX;
  private int minY;
  private int maxY;
  private int minZ;
  private int maxZ;
  private int sizeX;
  private int sizeY;
  private int sizeZ;
  private int[] snapshotBlocks;
  private int[] snapshotRotation;
  private int[] snapshotSupport;
  private int[] snapshotFiller;
  private int[] snapshotFluids;
  private byte[] snapshotFluidLevels;
  private long createdAt;
  private long expiresAt;

  public PortalPlacementRecord() {
  }

  public PortalPlacementRecord(@Nonnull UUID portalId, @Nonnull UUID worldUuid,
      @Nonnull String worldName, int x, int y, int z, int minX, int maxX, int minY, int maxY,
      int minZ, int maxZ, long createdAt, long expiresAt) {
    this.portalId = Objects.requireNonNull(portalId, "portalId");
    this.worldUuid = Objects.requireNonNull(worldUuid, "worldUuid");
    this.worldName = Objects.requireNonNull(worldName, "worldName");
    this.x = x;
    this.y = y;
    this.z = z;
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
    this.minZ = minZ;
    this.maxZ = maxZ;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  @Nullable
  public UUID getPortalId() {
    return portalId;
  }

  public void setPortalId(@Nullable UUID portalId) {
    this.portalId = portalId;
  }

  @Nullable
  public UUID getWorldUuid() {
    return worldUuid;
  }

  public void setWorldUuid(@Nullable UUID worldUuid) {
    this.worldUuid = worldUuid;
  }

  @Nullable
  public String getWorldName() {
    return worldName;
  }

  public void setWorldName(@Nullable String worldName) {
    this.worldName = worldName;
  }

  public int getX() {
    return x;
  }

  public void setX(int x) {
    this.x = x;
  }

  public int getY() {
    return y;
  }

  public void setY(int y) {
    this.y = y;
  }

  public int getZ() {
    return z;
  }

  public void setZ(int z) {
    this.z = z;
  }

  public int getMinX() {
    return minX;
  }

  public void setMinX(int minX) {
    this.minX = minX;
  }

  public int getMaxX() {
    return maxX;
  }

  public void setMaxX(int maxX) {
    this.maxX = maxX;
  }

  public int getMinY() {
    return minY;
  }

  public void setMinY(int minY) {
    this.minY = minY;
  }

  public int getMaxY() {
    return maxY;
  }

  public void setMaxY(int maxY) {
    this.maxY = maxY;
  }

  public int getMinZ() {
    return minZ;
  }

  public void setMinZ(int minZ) {
    this.minZ = minZ;
  }

  public int getMaxZ() {
    return maxZ;
  }

  public void setMaxZ(int maxZ) {
    this.maxZ = maxZ;
  }

  public int getSizeX() {
    return sizeX;
  }

  public void setSizeX(int sizeX) {
    this.sizeX = sizeX;
  }

  public int getSizeY() {
    return sizeY;
  }

  public void setSizeY(int sizeY) {
    this.sizeY = sizeY;
  }

  public int getSizeZ() {
    return sizeZ;
  }

  public void setSizeZ(int sizeZ) {
    this.sizeZ = sizeZ;
  }

  @Nullable
  public int[] getSnapshotBlocks() {
    return snapshotBlocks;
  }

  public void setSnapshotBlocks(@Nullable int[] snapshotBlocks) {
    this.snapshotBlocks = snapshotBlocks;
  }

  @Nullable
  public int[] getSnapshotRotation() {
    return snapshotRotation;
  }

  public void setSnapshotRotation(@Nullable int[] snapshotRotation) {
    this.snapshotRotation = snapshotRotation;
  }

  @Nullable
  public int[] getSnapshotSupport() {
    return snapshotSupport;
  }

  public void setSnapshotSupport(@Nullable int[] snapshotSupport) {
    this.snapshotSupport = snapshotSupport;
  }

  @Nullable
  public int[] getSnapshotFiller() {
    return snapshotFiller;
  }

  public void setSnapshotFiller(@Nullable int[] snapshotFiller) {
    this.snapshotFiller = snapshotFiller;
  }

  @Nullable
  public int[] getSnapshotFluids() {
    return snapshotFluids;
  }

  public void setSnapshotFluids(@Nullable int[] snapshotFluids) {
    this.snapshotFluids = snapshotFluids;
  }

  @Nullable
  public byte[] getSnapshotFluidLevels() {
    return snapshotFluidLevels;
  }

  public void setSnapshotFluidLevels(@Nullable byte[] snapshotFluidLevels) {
    this.snapshotFluidLevels = snapshotFluidLevels;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(long expiresAt) {
    this.expiresAt = expiresAt;
  }
}
