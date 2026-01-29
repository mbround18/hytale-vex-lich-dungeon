package MBRound18.ImmortalEngine.api.portal;

import com.hypixel.hytale.math.vector.Vector3i;
import javax.annotation.Nullable;

/**
 * Result of attempting to find and place a portal block.
 */
public final class PortalPlacementResult {
  private final boolean placed;
  private final PortalPlacementFailure failure;
  private final Vector3i position;
  private final String blockId;
  private final int blockNumericId;
  private final String detail;

  private PortalPlacementResult(boolean placed, PortalPlacementFailure failure, @Nullable Vector3i position,
      @Nullable String blockId, int blockNumericId, @Nullable String detail) {
    this.placed = placed;
    this.failure = failure;
    this.position = position;
    this.blockId = blockId;
    this.blockNumericId = blockNumericId;
    this.detail = detail;
  }

  public static PortalPlacementResult success(Vector3i position, @Nullable String blockId, int blockNumericId) {
    return new PortalPlacementResult(true, PortalPlacementFailure.NONE, position, blockId, blockNumericId, null);
  }

  public static PortalPlacementResult failure(PortalPlacementFailure failure, @Nullable Vector3i position,
      @Nullable String detail) {
    return new PortalPlacementResult(false, failure, position, null, 0, detail);
  }

  public boolean isPlaced() {
    return placed;
  }

  public PortalPlacementFailure getFailure() {
    return failure;
  }

  @Nullable
  public Vector3i getPosition() {
    return position;
  }

  @Nullable
  public String getBlockId() {
    return blockId;
  }

  public int getBlockNumericId() {
    return blockNumericId;
  }

  @Nullable
  public String getDetail() {
    return detail;
  }
}
