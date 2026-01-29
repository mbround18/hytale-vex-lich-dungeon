package MBRound18.ImmortalEngine.api.prefab;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable mapping from stitch prefab to compatible dungeon prefabs.
 */
public final class StitchIndex {
  private final Map<String, List<String>> stitchesToPrefabs;

  public StitchIndex(Map<String, List<String>> stitchesToPrefabs) {
    this.stitchesToPrefabs = Map.copyOf(stitchesToPrefabs);
  }

  public Map<String, List<String>> getStitchesToPrefabs() {
    return Collections.unmodifiableMap(stitchesToPrefabs);
  }

  public List<String> getMatches(String stitchPrefab) {
    return stitchesToPrefabs.getOrDefault(stitchPrefab, List.of());
  }
}
