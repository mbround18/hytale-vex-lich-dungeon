package MBRound18.PortalEngine.api.prefab;

/**
 * Simple bounding box information extracted from a prefab.
 */
public final class PrefabBounds {
  private final int minX;
  private final int maxX;
  private final int minY;
  private final int maxY;
  private final int minZ;
  private final int maxZ;
  private final int width;
  private final int depth;

  public PrefabBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
    this.minZ = minZ;
    this.maxZ = maxZ;
    this.width = Math.max(1, maxX - minX + 1);
    this.depth = Math.max(1, maxZ - minZ + 1);
  }

  public int getMinX() {
    return minX;
  }

  public int getMaxX() {
    return maxX;
  }

  public int getMinY() {
    return minY;
  }

  public int getMaxY() {
    return maxY;
  }

  public int getMinZ() {
    return minZ;
  }

  public int getMaxZ() {
    return maxZ;
  }

  public int getWidth() {
    return width;
  }

  public int getDepth() {
    return depth;
  }
}
