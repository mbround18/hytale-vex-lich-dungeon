package MBRound18.hytale.shared.interfaces.abstracts;

import MBRound18.hytale.shared.interfaces.ui.UiPath;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.UiThread;
import MBRound18.hytale.shared.interfaces.helpers.PlayerHelpers;

public abstract class AbstractCustomUIHud extends CustomUIHud {
  private final String hudPath;
  private final PlayerHelpers playerHelpers = new PlayerHelpers();
  @SuppressWarnings("unused")
  private CommandContext commandContext;
  @SuppressWarnings("unused")
  private Store<EntityStore> store;
  @SuppressWarnings("unused")
  private Ref<EntityStore> ref;

  protected AbstractCustomUIHud(@Nonnull String uiPath,
      @Nonnull CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef) {
    super(Objects.requireNonNull(playerRef, "playerRef"));
    this.hudPath = Objects.requireNonNull(uiPath, "uiPath");
    this.commandContext = Objects.requireNonNull(context, "context");
    this.store = Objects.requireNonNull(store, "store");
    this.ref = Objects.requireNonNull(ref, "ref");

  }

  protected AbstractCustomUIHud(@Nonnull String uiPath, @Nonnull PlayerRef playerRef) {
    super(Objects.requireNonNull(playerRef, "playerRef"));
    this.hudPath = Objects.requireNonNull(uiPath, "uiPath");
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

  public void clear() {
    UICommandBuilder builder = new UICommandBuilder();
    update(true, Objects.requireNonNull(builder, "builder"));
  }

  public void set(@Nonnull PlayerRef playerRef, @Nonnull String selector, @Nonnull String value) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      List<Map.Entry<String, String>> entries = java.util.Collections.singletonList(
          new AbstractMap.SimpleEntry<>(sanitizeSetSelector(selector), value));
      UICommandBuilder builder = setEntries(Objects.requireNonNull(entries, "entries"));
      update(false, Objects.requireNonNull(builder, "builder"));
    });

  }

  public void set(@Nonnull PlayerRef playerRef, @Nonnull String selector, @Nonnull Message value) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      UICommandBuilder builder = new UICommandBuilder();
      builder.set(sanitizeSetSelector(selector), Objects.requireNonNull(value, "value"));
      update(false, Objects.requireNonNull(builder, "builder"));
    });

  }

  private UICommandBuilder setEntries(@Nonnull List<? extends Map.Entry<String, ?>> entries) {

    UICommandBuilder builder = new UICommandBuilder();
    for (Map.Entry<String, ?> entry : entries) {
      @Nonnull
      String key = Objects.requireNonNull(entry.getKey(), "entry key");

      @Nonnull
      Message value = Objects.requireNonNull(coerceMessage(entry.getValue()), "entry value");

      builder.set(sanitizeSetSelector(key), value);
    }
    return builder;
  }

  private Message coerceMessage(Object value) {
    if (value instanceof Message) {
      return (Message) value;
    }
    if (value instanceof CharSequence) {
      String text = Objects.requireNonNull(value.toString(), "entry value");
      return Message.raw(text);
    }
    if (value == null) {
      return null;
    }
    String text = Objects.requireNonNull(value.toString(), "entry value");
    return Message.raw(text);
  }

  public String sanitizeSetSelector(@Nonnull String selector) {
    return selector;
  }

  public static String formatTime(int seconds) {
    int clamped = Math.max(0, seconds);
    int minutes = clamped / 60;
    int remainder = clamped % 60;
    return String.format("%d:%02d", minutes, remainder);
  }
}
