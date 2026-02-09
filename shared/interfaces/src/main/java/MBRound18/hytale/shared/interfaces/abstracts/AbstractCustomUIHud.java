package MBRound18.hytale.shared.interfaces.abstracts;

import MBRound18.hytale.shared.interfaces.ui.UiPath;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.shared.utilities.UiThread;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractCustomUIHud<T> extends CustomUIHud {
  private static final LoggingHelper logger = new LoggingHelper(AbstractCustomUIHud.class);

  @Nonnull
  private final String hudPath;
  @Nullable
  private final T uiModel;

  // ECS References
  @SuppressWarnings("unused")
  private CommandContext commandContext;
  @SuppressWarnings("unused")
  private Store<EntityStore> store;
  @SuppressWarnings("unused")
  private Ref<EntityStore> ref;

  // --- Constructors ---

  protected AbstractCustomUIHud(@Nonnull Class<T> uiClass,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef) {
    super(Objects.requireNonNull(playerRef, "playerRef"));
    Objects.requireNonNull(uiClass, "uiClass");

    this.hudPath = Objects.requireNonNull(resolveUiPath(uiClass), "uiPath");
    this.uiModel = instantiateUiModel(uiClass);
    this.store = Objects.requireNonNull(store, "store");
    this.ref = Objects.requireNonNull(ref, "ref");
  }

  protected AbstractCustomUIHud(@Nonnull String uiPath, @Nonnull PlayerRef playerRef) {
    super(Objects.requireNonNull(playerRef, "playerRef"));
    this.hudPath = Objects.requireNonNull(uiPath, "uiPath");
    this.uiModel = null; // No model for raw path constructor

    // Attempt to resolve ECS references automatically
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref != null && ref.isValid()) {
      this.store = ref.getStore();
      this.ref = ref;
    } else {
      logger.warn("Failed to get reference data for " + playerRef.getUsername());
    }
  }

  protected AbstractCustomUIHud(@Nonnull Class<T> uiClass, @Nonnull PlayerRef playerRef) {
    this(uiClass,
        Objects.requireNonNull(playerRef.getReference().getStore(), "store"),
        Objects.requireNonNull(playerRef.getReference(), "ref"),
        playerRef);
  }

  // --- Core Logic ---

  public boolean isActiveHud(@Nonnull PlayerRef playerRef) {
    // Use the provided playerRef to validate
    Player player = validateAndGetPlayer(playerRef);
    if (player == null)
      return false;

    HudManager hudManager = player.getHudManager();
    return hudManager != null && hudManager.getCustomHud() == this;
  }

  @Nullable
  protected Player validateAndGetPlayer() {
    return validateAndGetPlayer(getPlayerRef());
  }

  @Nullable
  protected Player validateAndGetPlayer(@Nonnull PlayerRef targetRef) {
    if (!targetRef.isValid())
      return null;

    Ref<EntityStore> ref = targetRef.getReference();
    if (ref == null || !ref.isValid())
      return null;

    Store<EntityStore> store = ref.getStore();
    if (store == null)
      return null;

    return store.getComponent(ref, Player.getComponentType());
  }

  @Override
  protected void build(@Nonnull UICommandBuilder builder) {
    String clientPath = UiPath.normalizeForClient(hudPath);
    String path = (clientPath != null) ? clientPath : hudPath;
    builder.append(path);
  }

  /**
   * Hook called before the HUD is cleared.
   */
  public void onClear() {
    // Optional override
  }

  /**
   * Closes this HUD for the instantiated player ref.
   */
  public void close() {
    close(getPlayerRef());
  }

  /**
   * Closes this HUD for the specified player ref.
   */
  public void close(@Nonnull PlayerRef targetRef) {
    UiThread.runOnPlayerWorld(targetRef, () -> {
      if (!isActiveHud(targetRef))
        return;

      onClear();

      UICommandBuilder builder = new UICommandBuilder();
      update(true, builder);
    });
  }

  // --- Dynamic Updates ---

  public void set(@Nonnull String selector, @Nonnull String value) {
    set(getPlayerRef(), selector, Message.raw(value));
  }

  public void set(@Nonnull PlayerRef targetRef, @Nonnull String selector, @Nonnull String value) {
    set(targetRef, selector, Message.raw(value));
  }

  public void set(@Nonnull String selector, @Nonnull Message value) {
    set(getPlayerRef(), selector, value);
  }

  public void set(@Nonnull PlayerRef targetRef, @Nonnull String selector, @Nonnull Message value) {
    UiThread.runOnPlayerWorld(targetRef, () -> {
      if (!isActiveHud(targetRef))
        return;

      if (selector.isEmpty()) {
        logger.warn("Empty selector provided to set() for " + targetRef.getUsername());
        return;
      }

      String sanitizedSelector = sanitizeSetSelector(selector);

      UICommandBuilder builder = new UICommandBuilder();
      builder.set(sanitizedSelector, Objects.requireNonNull(value, "value"));

      // false = partial update (don't rebuild entire UI)
      update(false, builder);
    });
  }

  /**
   * Ensures selectors are formatted correctly for Hytale UI (e.g.,
   * "#element.text").
   */
  public String sanitizeSetSelector(@Nonnull String selector) {
    String sanitized = selector.trim();

    // If it doesn't start with ID (#) or Class (.), assume ID
    if (!sanitized.startsWith("#") && !sanitized.startsWith(".")) {
      sanitized = "#" + sanitized;
    }

    if (!sanitized.contains(".")) {
      throw new IllegalArgumentException(
          "Selector must target a specific property using dot notation (e.g. #score.text), got: " + selector);
    }
    return sanitized;
  }

  @Nullable
  protected final T getUiModel() {
    return uiModel;
  }

  // --- Static Helpers ---

  public static String formatTime(int seconds) {
    int clamped = Math.max(0, seconds);
    int minutes = clamped / 60;
    int remainder = clamped % 60;
    return String.format("%d:%02d", minutes, remainder);
  }

  private static <T> String resolveUiPath(@Nonnull Class<T> uiClass) {
    try {
      Field field = uiClass.getField("UI_PATH");
      Object value = field.get(null);
      if (value instanceof String) {
        return (String) value;
      }
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException("Class " + uiClass.getName() + " missing 'public static final String UI_PATH'",
          e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve UI_PATH for " + uiClass.getName(), e);
    }
    throw new IllegalStateException("UI_PATH in " + uiClass.getName() + " is not a String");
  }

  private static <T> T instantiateUiModel(@Nonnull Class<T> uiClass) {
    try {
      return uiClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      logger.error("Failed to instantiate UI model " + uiClass.getName() + ": " + e.getMessage());
      return null;
    }
  }

  /**
   * Gets the currently active HUD of the specified type, if one exists.
   * Does NOT create a new HUD if one doesn't exist.
   */
  @Nullable
  private static <T extends AbstractCustomUIHud<?>> T getCurrentHud(PlayerRef playerRef, Class<T> hudClass) {
    if (!playerRef.isValid())
      return null;

    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid())
      return null;

    Store<EntityStore> store = ref.getStore();
    if (store == null)
      return null;

    Player playerComp = store.getComponent(ref, Player.getComponentType());
    if (playerComp == null)
      return null;

    HudManager hudManager = playerComp.getHudManager();
    if (hudManager == null)
      return null;

    var currentHud = hudManager.getCustomHud();
    if (currentHud != null && hudClass.isInstance(currentHud)) {
      return hudClass.cast(currentHud);
    }

    return null;
  }

  /**
   * Ensures the specified HUD class is active for the player.
   * If the player already has this HUD open, it returns the existing instance.
   * If not, it creates a new one, opens it, and returns it.
   */
  public static <T extends AbstractCustomUIHud<?>> T ensureActive(PlayerRef playerRef, Class<T> hudClass) {
    // 1. Check if the player ALREADY has this HUD open
    T existing = getCurrentHud(playerRef, hudClass);
    if (existing != null) {
      return existing;
    }

    // 2. Need to create a new instance - re-validate and get HudManager
    if (!playerRef.isValid())
      return null;

    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid())
      return null;

    Store<EntityStore> store = ref.getStore();
    if (store == null)
      return null;

    Player playerComp = store.getComponent(ref, Player.getComponentType());
    if (playerComp == null)
      return null;

    HudManager hudManager = playerComp.getHudManager();
    if (hudManager == null)
      return null;

    try {
      T instance = hudClass.getDeclaredConstructor(PlayerRef.class).newInstance(playerRef);
      hudManager.setCustomHud(playerRef, instance);
      return instance;
    } catch (Exception e) {
      logger.error("Failed to ensure active HUD " + hudClass.getSimpleName(), e);
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Safely closes any CustomHud matching the specified class.
   */
  public static <T extends AbstractCustomUIHud<?>> void closeHud(PlayerRef playerRef, Class<T> hudClass,
      @Nullable Consumer<T> onCloseAction) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      T hud = getCurrentHud(playerRef, hudClass);
      if (hud == null)
        return;

      // Call optional close action before clearing
      if (onCloseAction != null) {
        onCloseAction.accept(hud);
      }

      // Trigger the close
      hud.close(playerRef);
    });
  }
}
