package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.vexlichdungeon.dungeon.CardinalDirection;
import MBRound18.ImmortalEngine.api.prefab.StitchIndex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PrefabEdgeIndex {
  public record EdgeCandidate(@Nonnull String prefabPath, int rotation, @Nonnull CardinalDirection edge) {
    public EdgeCandidate {
      Objects.requireNonNull(prefabPath, "prefabPath");
      Objects.requireNonNull(edge, "edge");
    }
  }

  private final Map<String, Map<Integer, Map<CardinalDirection, Set<String>>>> stitchesByPrefab;
  private final Map<String, List<EdgeCandidate>> candidatesByStitch;

  PrefabEdgeIndex(@Nonnull Map<String, Map<Integer, Map<CardinalDirection, Set<String>>>> stitchesByPrefab,
      @Nonnull Map<String, List<EdgeCandidate>> candidatesByStitch) {
    this.stitchesByPrefab = Objects.requireNonNull(stitchesByPrefab, "stitchesByPrefab");
    this.candidatesByStitch = Objects.requireNonNull(candidatesByStitch, "candidatesByStitch");
  }

  @Nonnull
  public Set<String> getStitchesForEdge(@Nonnull String prefabPath, int rotation,
      @Nonnull CardinalDirection edge) {
    Map<Integer, Map<CardinalDirection, Set<String>>> byRotation = stitchesByPrefab.get(prefabPath);
    if (byRotation == null) {
      return Set.of();
    }
    Map<CardinalDirection, Set<String>> byEdge = byRotation.get(rotation);
    if (byEdge == null) {
      return Set.of();
    }
    Set<String> stitches = byEdge.get(edge);
    return stitches == null ? Set.of() : stitches;
  }

  @Nonnull
  public List<EdgeCandidate> getCandidatesForStitch(@Nonnull String stitchId,
      @Nonnull CardinalDirection edge) {
    List<EdgeCandidate> candidates = candidatesByStitch.get(stitchId);
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }
    List<EdgeCandidate> filtered = new ArrayList<>();
    for (EdgeCandidate candidate : candidates) {
      if (candidate.edge == edge) {
        filtered.add(candidate);
      }
    }
    return filtered.isEmpty() ? List.of() : Collections.unmodifiableList(filtered);
  }

  @Nonnull
  public List<EdgeCandidate> getCandidatesForStitches(@Nonnull Set<String> stitchIds,
      @Nonnull CardinalDirection edge) {
    if (stitchIds.isEmpty()) {
      return List.of();
    }
    List<EdgeCandidate> combined = new ArrayList<>();
    for (String stitchId : stitchIds) {
      combined.addAll(getCandidatesForStitch(stitchId, edge));
    }
    return combined.isEmpty() ? List.of() : Collections.unmodifiableList(combined);
  }

  @Nonnull
  public Map<String, List<EdgeCandidate>> getCandidatesByStitch() {
    return candidatesByStitch;
  }

  @Nonnull
  public StitchIndex toStitchIndex() {
    Map<String, List<String>> mapping = new HashMap<>();
    for (Map.Entry<String, List<EdgeCandidate>> entry : candidatesByStitch.entrySet()) {
      String stitchId = entry.getKey();
      LinkedHashSet<String> prefabs = new LinkedHashSet<>();
      for (EdgeCandidate candidate : entry.getValue()) {
        prefabs.add(candidate.prefabPath);
      }
      mapping.put(stitchId, new ArrayList<>(prefabs));
    }
    return new StitchIndex(mapping);
  }

  static final class Builder {
    private final Map<String, Map<Integer, Map<CardinalDirection, Set<String>>>> stitchesByPrefab = new HashMap<>();
    private final Map<String, List<EdgeCandidate>> candidatesByStitch = new HashMap<>();

    void addMatches(@Nonnull String prefabPath, int rotation, @Nonnull CardinalDirection edge,
        @Nonnull Set<String> stitchIds) {
      if (stitchIds.isEmpty()) {
        return;
      }
      Map<Integer, Map<CardinalDirection, Set<String>>> byRotation = stitchesByPrefab
          .computeIfAbsent(prefabPath, k -> new HashMap<>());
      Map<CardinalDirection, Set<String>> byEdge = byRotation
          .computeIfAbsent(rotation, k -> new EnumMap<>(CardinalDirection.class));
      Set<String> current = byEdge.computeIfAbsent(edge, k -> new HashSet<>());
      current.addAll(stitchIds);

      for (String stitchId : stitchIds) {
        candidatesByStitch
            .computeIfAbsent(stitchId, k -> new ArrayList<>())
            .add(new EdgeCandidate(prefabPath, rotation, edge));
      }
    }

    PrefabEdgeIndex build() {
      Map<String, Map<Integer, Map<CardinalDirection, Set<String>>>> frozen = new HashMap<>();
      for (Map.Entry<String, Map<Integer, Map<CardinalDirection, Set<String>>>> entry : stitchesByPrefab.entrySet()) {
        Map<Integer, Map<CardinalDirection, Set<String>>> byRotation = new HashMap<>();
        for (Map.Entry<Integer, Map<CardinalDirection, Set<String>>> rotEntry : entry.getValue().entrySet()) {
          Map<CardinalDirection, Set<String>> byEdge = new EnumMap<>(CardinalDirection.class);
          for (Map.Entry<CardinalDirection, Set<String>> edgeEntry : rotEntry.getValue().entrySet()) {
            byEdge.put(edgeEntry.getKey(), Collections.unmodifiableSet(new HashSet<>(edgeEntry.getValue())));
          }
          byRotation.put(rotEntry.getKey(), Collections.unmodifiableMap(byEdge));
        }
        frozen.put(entry.getKey(), Collections.unmodifiableMap(byRotation));
      }
      Map<String, List<EdgeCandidate>> frozenCandidates = new HashMap<>();
      for (Map.Entry<String, List<EdgeCandidate>> entry : candidatesByStitch.entrySet()) {
        frozenCandidates.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
      }
      return new PrefabEdgeIndex(Collections.unmodifiableMap(frozen), Collections.unmodifiableMap(frozenCandidates));
    }
  }
}
