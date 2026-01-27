package com.example.hytale.vexlichdungeon.dungeon;

import com.example.hytale.vexlichdungeon.prefab.PrefabPathHelper;
import java.util.*;
import javax.annotation.Nonnull;

/**
 * Manages prefab selection for dungeon generation.
 * Provides random selection of rooms, hallways, and gates with configurable
 * weights.
 */
public class PrefabSelector {

  private final Random random;

  // Available gate types (excluding blocked)
  private static final String[] GATE_TYPES = {
      "Vex_Seperator_Gate_Closed",
      "Vex_Seperator_Gate_Opened",
      "Vex_Seperator_Gate_Crawl",
      "Vex_Seperator_Gate_Jail",
      "Vex_Seperator_Gate_Lava",
      "Vex_Seperator_Gate_Lighted_Door",
      "Vex_Seperator_Gate_Peep",
      "Vex_Seperator_Gate_Spiked",
      "Vex_Seperator_Gate_Water"
  };

  // Available room types
  private static final String[] ROOM_TYPES = {
      "Vex_Room_S_Archers",
      "Vex_Room_S_Bats",
      "Vex_Room_S_Duck",
      "Vex_Room_S_Empty",
      "Vex_Room_S_Lava_A",
      "Vex_Room_S_Lava_B",
      "Vex_Room_S_Lava_C_Hostile",
      "Vex_Room_S_Mages"
  };

  // Available hallway types
  private static final String[] HALLWAY_TYPES = {
      "Vex_Room_S_Hallway_A", "Vex_Room_S_Hallway_B", "Vex_Room_S_Hallway_C",
      "Vex_Room_S_Hallway_D", "Vex_Room_S_Hallway_E", "Vex_Room_S_Hallway_F",
      "Vex_Room_S_Hallway_G", "Vex_Room_S_Hallway_H", "Vex_Room_S_Hallway_I",
      "Vex_Room_S_Hallway_J", "Vex_Room_S_Hallway_K", "Vex_Room_S_Hallway_L",
      "Vex_Room_S_Hallway_M", "Vex_Room_S_Hallway_N", "Vex_Room_S_Hallway_O",
      "Vex_Room_S_Hallway_P", "Vex_Room_S_Hallway_Q", "Vex_Room_S_Hallway_R",
      "Vex_Room_S_Hallway_S", "Vex_Room_S_Hallway_T", "Vex_Room_S_Hallway_U",
      "Vex_Room_S_Hallway_V"
  };

  /**
   * Creates a new prefab selector with the given random seed.
   * 
   * @param seed Random seed for reproducible generation
   */
  public PrefabSelector(long seed) {
    this.random = new Random(seed);
  }

  /**
   * Selects a random gate prefab (not blocked).
   * 
   * @return Full mod path to a random gate
   */
  @Nonnull
  public String selectRandomGate() {
    String gateName = GATE_TYPES[random.nextInt(GATE_TYPES.length)];
    return PrefabPathHelper.toGatePath(gateName);
  }

  /**
   * Gets the blocked gate prefab path.
   * 
   * @return Full mod path to the blocked gate
   */
  @Nonnull
  public String getBlockedGate() {
    return PrefabPathHelper.getBlockedGatePath();
  }

  /**
   * Selects a random room prefab.
   * 
   * @return Full mod path to a random room
   */
  @Nonnull
  public String selectRandomRoom() {
    String roomName = ROOM_TYPES[random.nextInt(ROOM_TYPES.length)];
    return PrefabPathHelper.toRoomPath(roomName);
  }

  /**
   * Selects a random hallway prefab.
   * 
   * @return Full mod path to a random hallway
   */
  @Nonnull
  public String selectRandomHallway() {
    String hallwayName = HALLWAY_TYPES[random.nextInt(HALLWAY_TYPES.length)];
    return PrefabPathHelper.toHallwayPath(hallwayName);
  }

  /**
   * Selects either a room or hallway based on probability.
   * 
   * @param roomProbability Probability of selecting a room (0.0 to 1.0)
   * @return Full mod path to either a room or hallway
   */
  @Nonnull
  public String selectRoomOrHallway(double roomProbability) {
    if (random.nextDouble() < roomProbability) {
      return selectRandomRoom();
    } else {
      return selectRandomHallway();
    }
  }

  /**
   * Generates a random rotation (0, 90, 180, or 270 degrees).
   * 
   * @return Random rotation in degrees
   */
  public int selectRandomRotation() {
    return random.nextInt(4) * 90; // 0, 90, 180, or 270
  }

  /**
   * Generates a random cardinal direction.
   * 
   * @return Random cardinal direction
   */
  @Nonnull
  public CardinalDirection selectRandomDirection() {
    CardinalDirection[] directions = CardinalDirection.values();
    return directions[random.nextInt(directions.length)];
  }

  /**
   * Gets the base courtyard prefab path.
   * 
   * @return Full mod path to the courtyard base
   */
  @Nonnull
  public String getBasePrefab() {
    return PrefabPathHelper.getCourtYardBasePath();
  }

  /**
   * Gets all available gate types (excluding blocked).
   * 
   * @return Array of gate type names
   */
  @Nonnull
  public static String[] getAvailableGates() {
    return GATE_TYPES.clone();
  }

  /**
   * Gets all available room types.
   * 
   * @return Array of room type names
   */
  @Nonnull
  public static String[] getAvailableRooms() {
    return ROOM_TYPES.clone();
  }

  /**
   * Gets all available hallway types.
   * 
   * @return Array of hallway type names
   */
  @Nonnull
  public static String[] getAvailableHallways() {
    return HALLWAY_TYPES.clone();
  }
}
