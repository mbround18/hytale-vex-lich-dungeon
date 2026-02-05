package MBRound18.hytale.vexlichdungeon.prefab;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;

public final class PrefabHookRegistry {
  private static final CopyOnWriteArrayList<PrefabHook> HOOKS = new CopyOnWriteArrayList<>();

  private PrefabHookRegistry() {
  }

  public static void register(@Nonnull PrefabHook hook) {
    HOOKS.addIfAbsent(hook);
  }

  public static void unregister(@Nonnull PrefabHook hook) {
    HOOKS.remove(hook);
  }

  @Nonnull
  @SuppressWarnings("null")
  public static List<PrefabHook> getHooks() {
    return List.copyOf(HOOKS);
  }
}
