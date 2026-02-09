package MBRound18.hytale.vexlichdungeon.dungeon;

import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages prefab selection for dungeon generation.
 * Dynamically discovers and selects rooms, hallways, and gates from available
 * assets.
 */
public class PrefabSelector {

  private final Random random;
  private final PrefabDiscovery discovery;

  /**
   * Creates a new prefab selector with the given random seed and discovery
   * system.
   * 
   * @param seed      Random seed for reproducible generation
   * @param discovery Prefab discovery system to find available prefabs
   */
  public PrefabSelector(long seed, @Nonnull PrefabDiscovery discovery) {
    this.random = new Random(seed);
    this.discovery = discovery;
  }

  public PrefabDiscovery getDiscovery() {
    return discovery;
  }

  /**
   * Selects a random room prefab from discovered assets.
   * 
   * @return Full mod path to a random room, or null if none available
   */
  @Nullable
  public String selectRandomRoom() {
    return discovery.getRandomRoom();
  }

  /**
   * Selects a random hallway prefab from discovered assets.
   * 
   * @return Full mod path to a random hallway, or null if none available
   */
  @Nullable
  public String selectRandomHallway() {
    return discovery.getRandomHallway();
  }

  /**
   * Selects a random event prefab from discovered assets.
   *
   * @return Full mod path to a random event prefab, or null if none available
   */
  @Nullable
  public String selectRandomEvent() {
    List<String> events = discovery.getAllEventPrefabs();
    if (events.isEmpty()) {
      return null;
    }
    return events.get(random.nextInt(events.size()));
  }

  /**
   * Selects either a room or hallway based on probability.
   * 
   * @param roomProbability Probability of selecting a room (0.0 to 1.0)
   * @return Full mod path to either a room or hallway, or null if none available
   */
  @Nullable
  public String selectRoomOrHallway(double roomProbability) {
    if (random.nextDouble() < roomProbability) {
      return selectRandomRoom();
    } else {
      return selectRandomHallway();
    }
  }

  /**
   * Selects a random gate prefab from discovered assets (not blocked).
   * 
   * @return Full mod path to a random gate, or null if none available
   */
  @Nullable
  public String selectRandomGate() {
    return discovery.getRandomGate();
  }

  /**
   * Gets the blocked gate prefab (for outer edges).
   * 
   * @return Full mod path to the blocked gate, or null if not found
   */
  @Nullable
  public String getBlockedGate() {
    return discovery.getBlockedGate();
  }

  /**
   * Gets the base courtyard prefab path.
   * Returns path without "Prefabs/" prefix for consistency with PrefabDiscovery.
   * 
   * @return Mod path to the base courtyard prefab (e.g.,
   *         "Rooms/Vex_Base_Courtyard")
   */
  @Nonnull
  public String getBasePrefab() {
    return "Base/Vex_Courtyard_Base";
  }

  /**
   * Generates a random rotation (0, 90, 180, or 270 degrees).
   * 
   * @return Random rotation in degrees
   */
  public int selectRandomRotation() {
    return random.nextInt(4) * 90; // 0, 90, 180, or 270
  }

  @Nullable
  public <T> T selectRandom(@Nonnull List<T> items) {
    if (items == null || items.isEmpty()) {
      return null;
    }
    return items.get(random.nextInt(items.size()));
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
}
