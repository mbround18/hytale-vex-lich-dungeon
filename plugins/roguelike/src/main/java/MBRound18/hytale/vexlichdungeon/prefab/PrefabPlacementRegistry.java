package MBRound18.hytale.vexlichdungeon.prefab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;

public final class PrefabPlacementRegistry {
  private static final Map<String, CopyOnWriteArrayList<PrefabPlacement>> PLACEMENTS = new ConcurrentHashMap<>();

  private PrefabPlacementRegistry() {
  }

  public static void add(@Nonnull PrefabPlacement placement) {
    String worldName = Objects.requireNonNull(placement, "placement").getWorldName();
    PLACEMENTS.computeIfAbsent(worldName, key -> new CopyOnWriteArrayList<>()).add(placement);
  }

  @Nonnull
  @SuppressWarnings("null")
  public static List<PrefabPlacement> getPlacements(@Nonnull String worldName) {
    CopyOnWriteArrayList<PrefabPlacement> list = PLACEMENTS.get(worldName);
    if (list == null) {
      return List.of();
    }
    return new ArrayList<>(list);
  }

  @Nonnull
  public static List<PrefabPlacement> getPlacementsByPrefix(@Nonnull String worldName, @Nonnull String prefix) {
    String needle = Objects.requireNonNull(prefix, "prefix");
    List<PrefabPlacement> placements = getPlacements(worldName);
    ArrayList<PrefabPlacement> result = new ArrayList<>();
    for (PrefabPlacement placement : placements) {
      if (placement != null && placement.getPrefabPath().startsWith(needle)) {
        result.add(placement);
      }
    }
    return result;
  }

  public static void clearWorld(@Nonnull String worldName) {
    PLACEMENTS.remove(worldName);
  }
}
