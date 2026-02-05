package MBRound18.hytale.vexlichdungeon.prefab;

import javax.annotation.Nonnull;
import java.util.Objects;

public final class PrefabDiscovered {
  private final String prefabPath;
  private final PrefabCategory category;
  private final PrefabSource source;

  public PrefabDiscovered(@Nonnull String prefabPath, @Nonnull PrefabCategory category, @Nonnull PrefabSource source) {
    this.prefabPath = Objects.requireNonNull(prefabPath, "prefabPath");
    this.category = Objects.requireNonNull(category, "category");
    this.source = Objects.requireNonNull(source, "source");
  }

  @Nonnull
  @SuppressWarnings("null")
  public String getPrefabPath() {
    return prefabPath;
  }

  @Nonnull
  @SuppressWarnings("null")
  public PrefabCategory getCategory() {
    return category;
  }

  @Nonnull
  @SuppressWarnings("null")
  public PrefabSource getSource() {
    return source;
  }
}
