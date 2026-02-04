package MBRound18.hytale.vexlichdungeon.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Configurable spawn pool mapping score ranges to enemy entries.
 * Loaded from spawn_pool.json in the plugin data directory.
 */
public class SpawnPoolConfig {

  private Map<String, List<SpawnPoolEntry>> ranges = new LinkedHashMap<>();

  @Nonnull
  public Map<String, List<SpawnPoolEntry>> getRanges() {
    return Objects.requireNonNull(ranges, "ranges");
  }

  public void setRanges(Map<String, List<SpawnPoolEntry>> ranges) {
    this.ranges = ranges != null ? ranges : new LinkedHashMap<>();
  }

  @Nonnull
  public SpawnPool toSpawnPool() {
    return SpawnPool.fromConfig(this);
  }

  @Nonnull
  public static SpawnPoolConfig createDefault() {
    SpawnPoolConfig config = new SpawnPoolConfig();
    List<SpawnPoolEntry> entries = new ArrayList<>();
    entries.add(new SpawnPoolEntry("Skeleton", 1));
    entries.add(new SpawnPoolEntry("Skeleton_Archer", 5));
    config.ranges.put("0..500", entries);
    return config;
  }
}
