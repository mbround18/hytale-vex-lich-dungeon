package MBRound18.hytale.vexlichdungeon.prefab;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

final class StitchPattern {
  private final String id;
  private final int width;
  private final int height;
  private final Map<Long, String> cells;

  StitchPattern(@Nonnull String id, int width, int height, @Nonnull Map<Long, String> cells) {
    this.id = Objects.requireNonNull(id, "id");
    this.width = Math.max(0, width);
    this.height = Math.max(0, height);
    this.cells = new HashMap<>(Objects.requireNonNull(cells, "cells"));
  }

  @Nonnull
  String getId() {
    return id;
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

  static long key(int u, int v) {
    return (((long) v) << 32) | (u & 0xffffffffL);
  }
}
