package MBRound18.hytale.vexlichdungeon.ui.core;

import MBRound18.ImmortalEngine.api.ui.UiPath;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public class AbstractCustomUIHud extends CustomUIHud {
  private final String hudPath;

  public AbstractCustomUIHud(
      @Nonnull String hudPath,
      @Nonnull PlayerRef playerRef) {
    super(playerRef);
    this.hudPath = hudPath;
  }

  @Override
  protected void build(UICommandBuilder builder) {
    String clientPath = UiPath.normalizeForClient(hudPath);
    String path = (clientPath != null) ? clientPath : hudPath;
    builder.append(path);
  }

  public void clear() {
    UICommandBuilder builder = new UICommandBuilder();
    update(true, builder);
  }

  public void set(@Nonnull String selector, @Nonnull String value) {
    UICommandBuilder builder = setEntries(
        List.of(
            new AbstractMap.SimpleEntry<>(selector, value)));
    update(false, builder);
  }

  public void set(@Nonnull List<Map.Entry<String, String>> entries) {
    UICommandBuilder builder = setEntries(entries);
    update(false, builder);
  }

  private UICommandBuilder setEntries(
      @Nonnull List<Map.Entry<String, String>> entries) {
    UICommandBuilder builder = new UICommandBuilder();
    for (Map.Entry<String, String> entry : entries) {
      builder.set(
          sanitizeSetSelector(entry.getKey()), Message.raw(entry.getValue()));
    }
    return builder;
  }

  public String sanitizeSetSelector(@Nonnull String selector) {
    // replce any .Text with .TextSpans
    if (selector.endsWith(".Text")) {
      return selector.substring(0, selector.length() - 5) + "TextSpans";
    }
    return selector;
  }

  public static String formatTime(int seconds) {
    int clamped = Math.max(0, seconds);
    int minutes = clamped / 60;
    int remainder = clamped % 60;
    return String.format("%d:%02d", minutes, remainder);
  }

}
