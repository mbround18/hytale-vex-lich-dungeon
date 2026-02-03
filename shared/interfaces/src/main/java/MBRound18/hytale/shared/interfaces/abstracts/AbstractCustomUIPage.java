package MBRound18.hytale.shared.interfaces.abstracts;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AbstractCustomUIPage extends CustomUIPage {
  public interface UiDocumentResolver {
    @Nullable
    String resolvePath(@Nonnull String uiPath);

    @Nullable
    String readInlineDocument(@Nonnull String uiPath);
  }

  private final @Nonnull String uiPath;
  private final @Nonnull Map<String, String> initialState;
  private final UiDocumentResolver resolver;

  public AbstractCustomUIPage(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime,
      @Nonnull String uiPath, @Nonnull Map<String, String> vars, @Nullable UiDocumentResolver resolver) {
    super(playerRef, lifetime);
    this.uiPath = Objects.requireNonNull(uiPath, "uiPath");
    this.initialState = Objects.requireNonNull(vars, "vars");
    this.resolver = resolver;
  }

  @Override
  public final void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commands,
      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
    @Nullable
    String resolvedPath = resolver != null ? resolver.resolvePath(uiPath) : null;
    @Nullable
    String inline = resolver != null ? resolver.readInlineDocument(uiPath) : null;
    if (inline != null) {
      commands.appendInline(null, inline);
    } else {
      String basePath = resolvedPath != null ? resolvedPath : uiPath;
      commands.append(basePath);
    }
  }

  @Nonnull
  protected final Map<String, String> getInitialState() {
    return Objects.requireNonNull(initialState, "initialState");
  }

  @Nonnull
  protected final String getUiPath() {
    return Objects.requireNonNull(uiPath, "uiPath");
  }

  @Nullable
  protected final UiDocumentResolver getResolver() {
    return resolver;
  }

  protected void init(@Nonnull UICommandBuilder commands) {
    applyInitialState(commands, initialState);
  }

  public final void applyInitialState() {
    UICommandBuilder builder = new UICommandBuilder();
    init(builder);
    sendUpdate(builder, false);
  }

  public static void applyInitialState(@Nonnull UICommandBuilder commands,
      @Nonnull Map<String, String> state) {
    for (Map.Entry<String, String> entry : state.entrySet()) {
      String id = entry.getKey();
      String value = entry.getValue();
      if (id == null || id.isBlank()) {
        continue;
      }
      commands.set(id, Objects.requireNonNull(value == null ? "" : value, "value"));
    }
  }

}
