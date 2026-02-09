package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.dungeon.CardinalDirection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PrefabEdgeIndexBuilder {
  private PrefabEdgeIndexBuilder() {
  }

  @Nullable
  public static PrefabEdgeIndex build(@Nonnull PrefabDiscovery discovery, @Nonnull PrefabSpawner spawner,
      @Nonnull LoggingHelper log) {
    Objects.requireNonNull(discovery, "discovery");
    Objects.requireNonNull(spawner, "spawner");
    Objects.requireNonNull(log, "log");

    List<String> stitches = discovery.getAllStitchPrefabs();
    if (stitches.isEmpty()) {
      log.warn("No stitch prefabs found; edge stitch index disabled.");
      return null;
    }

    List<String> rooms = new ArrayList<>();
    rooms.addAll(discovery.getAllDungeonPrefabs());
    rooms.addAll(discovery.getAllEventPrefabs());

    PrefabEdgeAnalyzer analyzer = new PrefabEdgeAnalyzer(spawner, log);
    List<StitchPattern> patterns = new ArrayList<>();
    for (String stitch : stitches) {
      StitchPattern pattern = analyzer.getStitchPattern(stitch);
      if (pattern != null) {
        patterns.add(pattern);
      }
    }
    if (patterns.isEmpty()) {
      log.warn("No stitch patterns could be parsed; edge stitch index disabled.");
      return null;
    }

    PrefabEdgeIndex.Builder builder = new PrefabEdgeIndex.Builder();

    for (String room : rooms) {
      if (room == null) {
        continue;
      }
      Map<Integer, Map<CardinalDirection, EdgeSlice>> edges = analyzer.getEdges(room);
      if (edges.isEmpty()) {
        continue;
      }
      for (Map.Entry<Integer, Map<CardinalDirection, EdgeSlice>> rotationEntry : edges.entrySet()) {
        int rotation = rotationEntry.getKey();
        Map<CardinalDirection, EdgeSlice> byEdge = rotationEntry.getValue();
        for (Map.Entry<CardinalDirection, EdgeSlice> edgeEntry : byEdge.entrySet()) {
          CardinalDirection edge = edgeEntry.getKey();
          EdgeSlice slice = edgeEntry.getValue();
          Set<String> matches = matchPatterns(slice, patterns);
          if (!matches.isEmpty()) {
            builder.addMatches(room, rotation, edge, matches);
          }
        }
      }
    }

    log.info("Built edge stitch index: %d stitch patterns, %d room/event prefabs",
        patterns.size(), rooms.size());
    return builder.build();
  }

  @Nonnull
  private static Set<String> matchPatterns(@Nonnull EdgeSlice slice, @Nonnull List<StitchPattern> patterns) {
    Set<String> matches = new HashSet<>();
    for (StitchPattern pattern : patterns) {
      if (patternMatches(slice, pattern)) {
        matches.add(pattern.getId());
      }
    }
    return matches;
  }

  private static boolean patternMatches(@Nonnull EdgeSlice slice, @Nonnull StitchPattern pattern) {
    int sliceWidth = slice.getWidth();
    int sliceHeight = slice.getHeight();
    int patternWidth = pattern.getWidth();
    int patternHeight = pattern.getHeight();
    if (patternWidth <= 0 || patternHeight <= 0) {
      return false;
    }
    if (patternWidth > sliceWidth || patternHeight > sliceHeight) {
      return false;
    }
    int maxU = sliceWidth - patternWidth;
    int maxV = sliceHeight - patternHeight;
    Map<Long, String> patternCells = pattern.getCells();

    for (int uOffset = 0; uOffset <= maxU; uOffset++) {
      for (int vOffset = 0; vOffset <= maxV; vOffset++) {
        if (patternMatchesAt(slice, patternCells, uOffset, vOffset)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean patternMatchesAt(@Nonnull EdgeSlice slice, @Nonnull Map<Long, String> patternCells,
      int uOffset, int vOffset) {
    for (Map.Entry<Long, String> entry : patternCells.entrySet()) {
      long key = entry.getKey();
      int u = (int) key;
      int v = (int) (key >>> 32);
      String expected = entry.getValue();
      String actual = slice.get(u + uOffset, v + vOffset);
      if (actual == null || !actual.equals(expected)) {
        return false;
      }
    }
    return true;
  }
}
