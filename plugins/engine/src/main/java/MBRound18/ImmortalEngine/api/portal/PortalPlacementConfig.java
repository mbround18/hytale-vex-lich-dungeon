package MBRound18.ImmortalEngine.api.portal;

/**
 * Configuration for scanning a safe portal placement near a player.
 *
 * <p>The scan starts in front of the player and searches outward in a grid:
 * forward distance, lateral offset, then vertical positions top-down.</p>
 */
public final class PortalPlacementConfig {
  private final int minForward;
  private final int maxForward;
  private final int lateralRange;
  private final int verticalAbove;
  private final int verticalBelow;

  public PortalPlacementConfig(int minForward, int maxForward, int lateralRange,
      int verticalAbove, int verticalBelow) {
    this.minForward = minForward;
    this.maxForward = maxForward;
    this.lateralRange = lateralRange;
    this.verticalAbove = verticalAbove;
    this.verticalBelow = verticalBelow;
  }

  public static PortalPlacementConfig defaults() {
    return new PortalPlacementConfig(2, 8, 2, 3, 6);
  }

  public int getMinForward() {
    return minForward;
  }

  public int getMaxForward() {
    return maxForward;
  }

  public int getLateralRange() {
    return lateralRange;
  }

  public int getVerticalAbove() {
    return verticalAbove;
  }

  public int getVerticalBelow() {
    return verticalBelow;
  }

  public boolean isValid() {
    return minForward > 0
        && maxForward >= minForward
        && lateralRange >= 0
        && verticalAbove >= 0
        && verticalBelow >= 0;
  }
}
