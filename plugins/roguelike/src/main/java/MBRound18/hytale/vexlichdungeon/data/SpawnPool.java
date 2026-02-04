package MBRound18.hytale.vexlichdungeon.data;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Parsed spawn pool ranges for fast lookup.
 */
public class SpawnPool {

  public static class SpawnRange {
    private final int minScore;
    private final int maxScore;
    private final List<SpawnPoolEntry> entries;

    public SpawnRange(int minScore, int maxScore, @Nonnull List<SpawnPoolEntry> entries) {
      this.minScore = minScore;
      this.maxScore = maxScore;
      this.entries = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(entries, "entries")));
    }

    public int getMinScore() {
      return minScore;
    }

    public int getMaxScore() {
      return maxScore;
    }

    @Nonnull
    public List<SpawnPoolEntry> getEntries() {
      return Objects.requireNonNull(entries, "entries");
    }

    public boolean matches(int score) {
      return score >= minScore && score <= maxScore;
    }
  }

  private final List<SpawnRange> ranges;

  private SpawnPool(@Nonnull List<SpawnRange> ranges) {
    this.ranges = Collections.unmodifiableList(Objects.requireNonNull(ranges, "ranges"));
  }

  @Nonnull
  public List<SpawnRange> getRanges() {
    return Objects.requireNonNull(ranges, "ranges");
  }

  @Nonnull
  public Optional<SpawnRange> findRange(int score) {
    for (SpawnRange range : ranges) {
      if (range.matches(score)) {
        return Objects.requireNonNull(Optional.of(range), "range");
      }
    }
    return Objects.requireNonNull(Optional.empty(), "empty");
  }

  @Nonnull
  public List<SpawnPoolEntry> getEntriesForScore(int score) {
    return Objects.requireNonNull(findRange(score)
        .map(SpawnRange::getEntries)
        .orElseGet(Collections::emptyList), "entries");
  }

  @Nonnull
  public static SpawnPool fromConfig(@Nonnull SpawnPoolConfig config) {
    return fromConfig(config, null);
  }

  @Nonnull
  public static SpawnPool fromConfig(@Nonnull SpawnPoolConfig config, LoggingHelper log) {
    List<SpawnRange> parsed = new ArrayList<>();
    for (Map.Entry<String, List<SpawnPoolEntry>> entry : config.getRanges().entrySet()) {
      String rangeKey = entry.getKey();
      int[] bounds = parseRange(rangeKey);
      if (bounds == null) {
        if (log != null) {
          log.warn("Invalid spawn pool range key: %s", rangeKey);
        }
        continue;
      }
      List<SpawnPoolEntry> entries = entry.getValue() != null ? entry.getValue() : Collections.emptyList();
      parsed.add(new SpawnRange(bounds[0], bounds[1], Objects.requireNonNull(entries, "entries")));
    }
    return new SpawnPool(parsed);
  }

  private static int[] parseRange(String rangeKey) {
    if (rangeKey == null) {
      return null;
    }
    String[] parts = rangeKey.trim().split("\\.\\.");
    if (parts.length != 2) {
      return null;
    }
    try {
      int min = Integer.parseInt(parts[0].trim());
      int max = Integer.parseInt(parts[1].trim());
      if (max < min) {
        return null;
      }
      return new int[] { min, max };
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
