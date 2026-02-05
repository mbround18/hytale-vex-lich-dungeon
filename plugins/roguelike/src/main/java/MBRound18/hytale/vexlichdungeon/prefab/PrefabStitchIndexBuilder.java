package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.ImmortalEngine.api.prefab.StitchIndex;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PrefabStitchIndexBuilder {
  private PrefabStitchIndexBuilder() {
  }

  @Nullable
  public static StitchIndex build(@Nonnull PrefabDiscovery discovery, @Nonnull PrefabSpawner spawner,
      @Nonnull LoggingHelper log) {
    Objects.requireNonNull(discovery, "discovery");
    Objects.requireNonNull(spawner, "spawner");
    Objects.requireNonNull(log, "log");

    List<String> stitches = discovery.getAllStitchPrefabs();
    if (stitches.isEmpty()) {
      log.warn("No stitch prefabs found; stitch index disabled.");
      return null;
    }

    List<String> dungeons = discovery.getAllDungeonPrefabs();
    List<String> events = discovery.getAllEventPrefabs();
    List<String> rooms = new ArrayList<>(dungeons.size() + events.size());
    rooms.addAll(dungeons);
    rooms.addAll(events);

    Map<String, List<String>> mapping = new HashMap<>();
    for (String stitch : stitches) {
      PrefabInspector.PrefabDimensions stitchDims = spawner.getPrefabDimensions(
          Objects.requireNonNull(stitch, "stitch"));
      List<String> matches = new ArrayList<>();
      for (String room : rooms) {
        PrefabInspector.PrefabDimensions roomDims = spawner.getPrefabDimensions(
            Objects.requireNonNull(room, "room"));
        if (stitchDims.width == roomDims.width && stitchDims.depth == roomDims.depth) {
          matches.add(room);
        }
      }
      mapping.put(stitch, matches);
    }

    log.info("Built stitch index: %d stitch patterns, %d rooms/events", stitches.size(), rooms.size());
    return new StitchIndex(mapping);
  }
}
