package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.ImmortalEngine.api.prefab.StitchIndex;
import MBRound18.hytale.shared.utilities.LoggingHelper;
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

    PrefabEdgeIndex edgeIndex = PrefabEdgeIndexBuilder.build(discovery, spawner, log);
    if (edgeIndex == null) {
      return null;
    }
    StitchIndex stitchIndex = edgeIndex.toStitchIndex();
    int stitchCount = stitchIndex.getStitchesToPrefabs().size();
    int roomCount = discovery.getAllDungeonPrefabs().size() + discovery.getAllEventPrefabs().size();
    log.info("Built stitch index: %d stitch patterns, %d rooms/events", stitchCount, roomCount);
    return stitchIndex;
  }
}
