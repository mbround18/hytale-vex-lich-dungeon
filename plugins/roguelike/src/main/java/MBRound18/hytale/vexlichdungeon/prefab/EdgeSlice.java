package MBRound18.hytale.vexlichdungeon.prefab;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class EdgeSlice {
  private final int width;
  private final int height;
  private final Map<Long, String> cells;

  EdgeSlice(int width, int height) {
    this.width = Math.max(0, width);
    this.height = Math.max(0, height);
    this.cells = new HashMap<>();
  }

  void put(int u, int v, @Nonnull String name) {
    if (u < 0 || v < 0 || u >= width || v >= height) {
      return;
    }
    cells.put(key(u, v), name);
  }

  @Nullable
  String get(int u, int v) {
    return cells.get(key(u, v));
  }

  int getWidth() {
    return width;
  }

  int getHeight() {
    return height;
  }

  @Nonnull
  Map<Long, String> getCells() {
    return Collections.unmodifiableMap(cells);
  }

  private static long key(int u, int v) {
    return (((long) v) << 32) | (u & 0xffffffffL);
  }
}
