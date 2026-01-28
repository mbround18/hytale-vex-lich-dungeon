package MBRound18.hytale.vexlichdungeon.dungeon;

import MBRound18.hytale.vexlichdungeon.data.SpawnPool;
import MBRound18.hytale.vexlichdungeon.data.SpawnPoolEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;

/**
 * Plans enemy spawns based on score budget and spawn pool configuration.
 */
public class EnemySpawnPlanner {

  private final Random random;

  public EnemySpawnPlanner(long seed) {
    this.random = new Random(seed);
  }

  @Nonnull
  public List<SpawnPoolEntry> planEnemies(@Nonnull SpawnPool pool, int scoreBudget) {
    List<SpawnPoolEntry> result = new ArrayList<>();
    if (scoreBudget <= 0) {
      return result;
    }

    List<SpawnPoolEntry> entries = pool.getEntriesForScore(scoreBudget);
    if (entries.isEmpty()) {
      return result;
    }

    int remaining = scoreBudget;
    int safety = 0;
    while (remaining > 0 && safety < 1000) {
      SpawnPoolEntry entry = pickEligible(entries, remaining);
      if (entry == null) {
        break;
      }
      result.add(new SpawnPoolEntry(entry.getEnemy(), entry.getPoints()));
      remaining -= Math.max(1, entry.getPoints());
      safety++;
    }

    return result;
  }

  private SpawnPoolEntry pickEligible(List<SpawnPoolEntry> entries, int remaining) {
    List<SpawnPoolEntry> eligible = new ArrayList<>();
    for (SpawnPoolEntry entry : entries) {
      if (entry.getPoints() <= remaining) {
        eligible.add(entry);
      }
    }
    if (eligible.isEmpty()) {
      return null;
    }
    return eligible.get(random.nextInt(eligible.size()));
  }
}
