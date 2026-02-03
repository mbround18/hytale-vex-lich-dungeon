package MBRound18.hytale.shared.interfaces.commands;

import MBRound18.hytale.shared.interfaces.ui.UiTemplate;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import javax.annotation.Nonnull;

public interface UiCommandHandler {
  @Nonnull
  Map<String, UiTemplate> getUiTemplates();

  @Nonnull
  Map<String, UiTemplate> getHudTemplates();

  boolean openUi(@Nonnull PlayerRef playerRef, @Nonnull UiTemplate template, @Nonnull Map<String, String> vars);

  boolean openHud(@Nonnull PlayerRef playerRef, @Nonnull UiTemplate template, @Nonnull Map<String, String> vars);

  boolean clearHud(@Nonnull PlayerRef playerRef);

  default boolean handleCustomShow(@Nonnull CommandContext context, @Nonnull PlayerRef playerRef,
      @Nonnull String id, @Nonnull Map<String, String> vars) {
    return false;
  }

  default boolean supportsReload() {
    return false;
  }

  default boolean reloadTemplates(@Nonnull CommandContext context) {
    return false;
  }

  default boolean supportsDemo() {
    return false;
  }

  default boolean startDemo(@Nonnull CommandContext context, @Nonnull PlayerRef playerRef,
      @Nonnull Map<String, String> vars) {
    return false;
  }

  default boolean supportsTest() {
    return false;
  }

  default boolean startTest(@Nonnull CommandContext context, @Nonnull PlayerRef playerRef, int delaySeconds) {
    return false;
  }
}
