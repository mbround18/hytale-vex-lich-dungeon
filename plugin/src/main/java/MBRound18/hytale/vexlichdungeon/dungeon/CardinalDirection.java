package MBRound18.hytale.vexlichdungeon.dungeon;

import javax.annotation.Nonnull;

/**
 * Represents the four cardinal directions for dungeon tile connections.
 * Each direction has an associated rotation and offset for navigation.
 */
public enum CardinalDirection {
  NORTH(0, 0, -1, 0), // Rotation 0°, moves -Z
  EAST(90, 1, 0, 90), // Rotation 90°, moves +X
  SOUTH(180, 0, 1, 180), // Rotation 180°, moves +Z
  WEST(270, -1, 0, 270); // Rotation 270°, moves -X

  private final int rotationDegrees;
  private final int offsetX;
  private final int offsetZ;
  private final int prefabRotation;

  CardinalDirection(int rotationDegrees, int offsetX, int offsetZ, int prefabRotation) {
    this.rotationDegrees = rotationDegrees;
    this.offsetX = offsetX;
    this.offsetZ = offsetZ;
    this.prefabRotation = prefabRotation;
  }

  /**
   * Gets the rotation in degrees for this direction.
   * 
   * @return Rotation in degrees (0, 90, 180, 270)
   */
  public int getRotationDegrees() {
    return rotationDegrees;
  }

  /**
   * Gets the X offset for moving one tile in this direction.
   * 
   * @return X offset (-1, 0, or 1)
   */
  public int getOffsetX() {
    return offsetX;
  }

  /**
   * Gets the Z offset for moving one tile in this direction.
   * 
   * @return Z offset (-1, 0, or 1)
   */
  public int getOffsetZ() {
    return offsetZ;
  }

  /**
   * Gets the prefab rotation value for Hytale's rotation system.
   * 
   * @return Rotation value matching PrefabRotation enum
   */
  public int getPrefabRotation() {
    return prefabRotation;
  }

  /**
   * Gets the opposite direction.
   * 
   * @return The opposite cardinal direction
   */
  @Nonnull
  public CardinalDirection getOpposite() {
    return switch (this) {
      case NORTH -> SOUTH;
      case EAST -> WEST;
      case SOUTH -> NORTH;
      case WEST -> EAST;
    };
  }

  /**
   * Rotates this direction clockwise by 90 degrees.
   * 
   * @return The direction after rotating clockwise
   */
  @Nonnull
  public CardinalDirection rotateClockwise() {
    return switch (this) {
      case NORTH -> EAST;
      case EAST -> SOUTH;
      case SOUTH -> WEST;
      case WEST -> NORTH;
    };
  }

  /**
   * Rotates this direction counter-clockwise by 90 degrees.
   * 
   * @return The direction after rotating counter-clockwise
   */
  @Nonnull
  public CardinalDirection rotateCounterClockwise() {
    return switch (this) {
      case NORTH -> WEST;
      case EAST -> NORTH;
      case SOUTH -> EAST;
      case WEST -> SOUTH;
    };
  }

  /**
   * Gets a direction from rotation degrees.
   * 
   * @param degrees Rotation in degrees (0, 90, 180, 270)
   * @return The matching cardinal direction
   * @throws IllegalArgumentException if degrees is not a valid cardinal rotation
   */
  @Nonnull
  public static CardinalDirection fromDegrees(int degrees) {
    // Normalize to 0-360 range
    int normalized = ((degrees % 360) + 360) % 360;

    return switch (normalized) {
      case 0 -> NORTH;
      case 90 -> EAST;
      case 180 -> SOUTH;
      case 270 -> WEST;
      default -> throw new IllegalArgumentException(
          "Invalid cardinal direction degrees: " + degrees +
              ". Must be 0, 90, 180, or 270.");
    };
  }

  /**
   * Gets all cardinal directions in order (N, E, S, W).
   * 
   * @return Array of all cardinal directions
   */
  @Nonnull
  public static CardinalDirection[] all() {
    return values();
  }
}
