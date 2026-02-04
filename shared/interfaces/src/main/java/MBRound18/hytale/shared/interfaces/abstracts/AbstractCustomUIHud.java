package MBRound18.hytale.shared.interfaces.abstracts;

import MBRound18.hytale.shared.interfaces.ui.UiPath;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.shared.utilities.UiThread;

public abstract class AbstractCustomUIHud<T> extends CustomUIHud {
  private final String hudPath;
  private final T uiModel;
  @SuppressWarnings("unused")
  private CommandContext commandContext;
  @SuppressWarnings("unused")
  private Store<EntityStore> store;
  @SuppressWarnings("unused")
  private Ref<EntityStore> ref;
  private static final LoggingHelper logger = new LoggingHelper(AbstractCustomUIHud.class);

  protected AbstractCustomUIHud(@Nonnull Class<T> uiClass,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef) {
    super(Objects.requireNonNull(playerRef, "playerRef"));

    // Resolve the path and model from the class
    Objects.requireNonNull(uiClass, "uiClass");
    this.hudPath = Objects.requireNonNull(resolveUiPath(uiClass), "uiPath");
    this.uiModel = Objects.requireNonNull(instantiateUiModel(uiClass), "uiModel");

    // CRITICAL: Save the ECS references!
    // Without this, getStore() returns null -> Crash.
    this.store = Objects.requireNonNull(store, "store");
    this.ref = Objects.requireNonNull(ref, "ref");
  }

  protected AbstractCustomUIHud(@Nonnull String uiPath, @Nonnull PlayerRef playerRef) {
    super(Objects.requireNonNull(playerRef, "playerRef"));
    this.hudPath = Objects.requireNonNull(uiPath, "uiPath");
    this.uiModel = null;
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref != null && ref.isValid()) {
      this.store = ref.getStore();
      this.ref = ref;
    } else {
      logger.warn("Failed to get reference data for " + playerRef.getUsername());
    }

  }

  protected AbstractCustomUIHud(@Nonnull Class<T> uiClass, @Nonnull PlayerRef playerRef) {
    super(Objects.requireNonNull(playerRef, "playerRef"));
    Objects.requireNonNull(uiClass, "uiClass");
    this.hudPath = Objects.requireNonNull(resolveUiPath(uiClass), "uiPath");
    this.uiModel = Objects.requireNonNull(instantiateUiModel(uiClass), "uiModel");
  }

  public boolean isActiveHud(@Nonnull PlayerRef playerRef) {
    if (!playerRef.isValid()) {
      return false;
    }
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return false;
    }
    Store<EntityStore> store = ref.getStore();
    com.hypixel.hytale.server.core.entity.entities.Player player = store.getComponent(
        ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
    if (player == null) {
      return false;
    }
    com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager hudManager = player.getHudManager();
    return hudManager != null && hudManager.getCustomHud() == this;
  }

  @Nullable
  protected com.hypixel.hytale.server.core.entity.entities.Player validateAndGetPlayer() {
    // 1. Check Player Reference
    if (!getPlayerRef().isValid()) {
      return null;
    }

    // 2. Check Entity Store Reference
    Ref<EntityStore> ref = getPlayerRef().getReference();
    if (ref == null || !ref.isValid()) {
      return null;
    }

    // 3. Get Player Component from ECS
    // (Safe because we are already inside UiThread.runOnPlayerWorld)
    var store = ref.getStore();
    var playerComp = store.getComponent(ref,
        com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());

    if (playerComp == null) {
      return null;
    }

    // 4. Verify HUD Ownership
    // This ensures we stop updating if another HUD replaced this one
    var hudManager = playerComp.getHudManager();
    if (hudManager == null || !isActiveHud(getPlayerRef())) {
      return null;
    }

    return playerComp;
  }

  @Override
  protected void build(@Nonnull UICommandBuilder builder) {
    String clientPath = UiPath.normalizeForClient(hudPath);
    String path = (clientPath != null) ? clientPath : hudPath;
    builder.append(path);
  }

  public void run() {
    return;
  }

  public void onClear() {
    return;
  }

  public void clear() {
    onClear();
    UICommandBuilder builder = new UICommandBuilder();
    update(true, Objects.requireNonNull(builder, "builder"));
  }

  public void set(@Nonnull PlayerRef playerRef, @Nonnull String selector, @Nonnull String value) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      if (!isActiveHud(playerRef)) {
        return;
      }
      UICommandBuilder builder = new UICommandBuilder();
      builder.set(sanitizeSetSelector(selector), Objects.requireNonNull(value, "value"));

      update(false, Objects.requireNonNull(builder, "builder"));
    });

  }

  public void set(@Nonnull PlayerRef playerRef, @Nonnull String selector, @Nonnull Message value) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      if (!isActiveHud(playerRef)) {
        return;
      }
      UICommandBuilder builder = new UICommandBuilder();
      builder.set(sanitizeSetSelector(selector), Objects.requireNonNull(value, "value"));
      update(false, Objects.requireNonNull(builder, "builder"));
    });
  }

  public String sanitizeSetSelector(@Nonnull String selector) {
    String sanitized = Objects.requireNonNull(selector, "selector");
    if (!sanitized.startsWith("#") && !sanitized.startsWith(".")) {
      sanitized = "#" + sanitized;
    }
    if (!sanitized.contains(".")) {
      // error we arent targeting any fields
      throw new IllegalArgumentException(
          "Selector must target a specific field using dot notation: " + selector);
    }
    return sanitized;
  }

  @Nonnull
  protected final T getUiModel() {
    return Objects.requireNonNull(uiModel, "uiModel");
  }

  public static String formatTime(int seconds) {
    int clamped = Math.max(0, seconds);
    int minutes = clamped / 60;
    int remainder = clamped % 60;
    return String.format("%d:%02d", minutes, remainder);
  }

  private static <T> String resolveUiPath(@Nonnull Class<T> uiClass) {
    try {
      java.lang.reflect.Field field = uiClass.getField("UI_PATH");
      Object value = field.get(null);
      if (value instanceof String) {
        return (String) value;
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve UI_PATH for " + uiClass.getName(), e);
    }
    throw new IllegalStateException("UI_PATH is missing or not a String on " + uiClass.getName());
  }

  private static <T> T instantiateUiModel(@Nonnull Class<T> uiClass) {
    try {
      return uiClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to instantiate UI model " + uiClass.getName(), e);
    }
  }

  /**
   * Ensures the specified HUD class is active for the player.
   * If a different HUD is open, or none is open, it creates and opens the new
   * one.
   *
   * @param playerRef The player.
   * @param hudClass  The class of the HUD you want (e.g. DemoHudStatsHud.class).
   * @param factory   A lambda to create the HUD if it's missing.
   * @return The active HUD instance (guaranteed to be of type T).
   */
  public static <T extends AbstractCustomUIHud<?>> T ensureActive(
      PlayerRef playerRef,
      Class<T> hudClass) {
    if (!playerRef.isValid()) {
      return null;
    }
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return null;
    }
    Store<EntityStore> store = ref.getStore();
    Player playerComp = store.getComponent(ref, Player.getComponentType());
    if (playerComp == null)
      return null;
    HudManager hudManager = playerComp.getHudManager();
    var currentHud = hudManager.getCustomHud();

    try {
      Class<T> clazz = hudClass;
      T instance = clazz.getDeclaredConstructor(PlayerRef.class).newInstance(playerRef);

      if (!instance.isActiveHud(playerRef)) {
        hudManager.setCustomHud(playerRef, instance);
      }
      return instance;

      // if (currentHud == null || !hudClass.isInstance(currentHud)) {
      // hudManager.setCustomHud(playerRef, instance);

      // instance.show();

      // return instance;
      // }

      // instance.show()

      // return instance;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
