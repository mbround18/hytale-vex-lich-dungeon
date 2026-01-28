package MBRound18.hytale.vexlichdungeon.dungeon;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single tile in the dungeon grid.
 * Each tile is 19x19 blocks and can have gates on each cardinal direction.
 */
public class DungeonTile {

  private final int gridX;
  private final int gridZ;
  private final String prefabPath;
  private final int rotation; // 0, 90, 180, 270 degrees
  private final Map<CardinalDirection, String> gates;
  private final TileType type;

  /**
   * Creates a new dungeon tile.
   * 
   * @param gridX      Grid X coordinate
   * @param gridZ      Grid Z coordinate
   * @param prefabPath Full mod path to the prefab
   * @param rotation   Rotation in degrees (0, 90, 180, 270)
   * @param type       Type of tile (BASE, ROOM, HALLWAY)
   */
  public DungeonTile(int gridX, int gridZ, @Nonnull String prefabPath, int rotation, @Nonnull TileType type) {
    this.gridX = gridX;
    this.gridZ = gridZ;
    this.prefabPath = Objects.requireNonNull(prefabPath, "prefabPath cannot be null");
    this.rotation = rotation;
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.gates = new EnumMap<>(CardinalDirection.class);
  }

  /**
   * Gets the grid X coordinate.
   * 
   * @return Grid X position
   */
  public int getGridX() {
    return gridX;
  }

  /**
   * Gets the grid Z coordinate.
   * 
   * @return Grid Z position
   */
  public int getGridZ() {
    return gridZ;
  }

  /**
   * Gets the full mod path to this tile's prefab.
   * 
   * @return Prefab path (e.g.,
   *         "Mods/VexLichDungeon/Rooms/Vex_Room_S_Archers.prefab.json")
   */
  @Nonnull
  public String getPrefabPath() {
    return prefabPath;
  }

  /**
   * Gets the rotation of this tile in degrees.
   * 
   * @return Rotation (0, 90, 180, 270)
   */
  public int getRotation() {
    return rotation;
  }

  /**
   * Gets the type of this tile.
   * 
   * @return Tile type
   */
  @Nonnull
  public TileType getType() {
    return type;
  }

  /**
   * Sets the gate prefab for a specific direction.
   * 
   * @param direction      Cardinal direction
   * @param gatePrefabPath Full mod path to the gate prefab
   */
  public void setGate(@Nonnull CardinalDirection direction, @Nonnull String gatePrefabPath) {
    gates.put(direction, gatePrefabPath);
  }

  /**
   * Gets the gate prefab for a specific direction.
   * 
   * @param direction Cardinal direction
   * @return Gate prefab path, or null if no gate set
   */
  @Nullable
  public String getGate(@Nonnull CardinalDirection direction) {
    return gates.get(direction);
  }

  /**
   * Checks if this tile has a gate in the specified direction.
   * 
   * @param direction Cardinal direction
   * @return True if gate exists
   */
  public boolean hasGate(@Nonnull CardinalDirection direction) {
    return gates.containsKey(direction);
  }

  /**
   * Gets all gates for this tile.
   * 
   * @return Immutable map of direction to gate prefab path
   */
  @Nonnull
  public Map<CardinalDirection, String> getGates() {
    return Map.copyOf(gates);
  }

  /**
   * Calculates the world X coordinate for this tile's origin.
   * 
   * @param tileSize Size of each tile in blocks (default 19)
   * @return World X coordinate
   */
  public int getWorldX(int tileSize) {
    return gridX * tileSize;
  }

  /**
   * Calculates the world Z coordinate for this tile's origin.
   * 
   * @param tileSize Size of each tile in blocks (default 19)
   * @return World Z coordinate
   */
  public int getWorldZ(int tileSize) {
    return gridZ * tileSize;
  }

  @Override
  public String toString() {
    return String.format("DungeonTile[grid=(%d,%d), prefab=%s, rotation=%d°, type=%s, gates=%d]",
        gridX, gridZ, prefabPath, rotation, type, gates.size());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    DungeonTile that = (DungeonTile) o;
    return gridX == that.gridX && gridZ == that.gridZ;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gridX, gridZ);
  }

  /**
   * Type of dungeon tile.
   */
  public enum TileType {
    /** Base/spawn tile - always at (0,0) */
    BASE,
    /** Combat or puzzle room */
    ROOM,
    /** Connecting hallway */
    HALLWAY,
    /** Corner base piece */
    BASE_CORNER
  }
}
