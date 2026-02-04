package MBRound18.hytale.vexlichdungeon.data;

import com.hypixel.hytale.math.vector.Vector3d;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player spawn positions for dungeon generation.
 * Since the configured spawn in instance.bson may not match where players
 * actually spawn, we capture their first spawn location and use that
 * as the center point for dungeon generation.
 */
public class PlayerSpawnTracker {

  private final ConcurrentHashMap<String, Vector3d> firstSpawnByWorld = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Boolean> isDungeonGeneratedByWorld = new ConcurrentHashMap<>();

  /**
   * Records the first player spawn position for a world.
   * If a spawn has already been recorded for this world, this call is ignored.
   * 
   * @param worldName World name
   * @param position  Player's spawn position
   * @return true if this was the first spawn recorded, false if already tracked
   */
  public boolean recordFirstSpawn(@Nonnull String worldName, @Nonnull Vector3d position) {
    Vector3d existingSpawn = firstSpawnByWorld.putIfAbsent(worldName, position);
    return existingSpawn == null;
  }

  /**
   * Gets the first recorded spawn position for a world.
   * 
   * @param worldName World name
   * @return First spawn position, or null if no spawn recorded yet
   */
  @Nullable
  public Vector3d getFirstSpawn(@Nonnull String worldName) {
    return firstSpawnByWorld.get(worldName);
  }

  /**
   * Checks if a spawn position has been recorded for a world.
   * 
   * @param worldName World name
   * @return true if a spawn has been recorded
   */
  public boolean hasSpawn(@Nonnull String worldName) {
    return firstSpawnByWorld.containsKey(worldName);
  }

  /**
   * Marks that dungeon generation has been triggered for a world.
   * This prevents multiple generation attempts.
   * 
   * @param worldName World name
   * @return true if this was the first generation marked, false if already marked
   */
  public boolean markGenerationTriggered(@Nonnull String worldName) {
    Boolean existing = isDungeonGeneratedByWorld.putIfAbsent(worldName, true);
    return existing == null;
  }

  /**
   * Checks if dungeon generation has been triggered for a world.
   * 
   * @param worldName World name
   * @return true if generation has been triggered
   */
  public boolean isGenerationTriggered(@Nonnull String worldName) {
    return isDungeonGeneratedByWorld.getOrDefault(worldName, false);
  }

  /**
   * Clears tracking data for a world.
   * Useful for testing or if generation needs to be reset.
   * 
   * @param worldName World name
   */
  public void clearWorld(@Nonnull String worldName) {
    firstSpawnByWorld.remove(worldName);
    isDungeonGeneratedByWorld.remove(worldName);
  }
}
