package MBRound18.hytale.vexlichdungeon.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import javax.annotation.Nonnull;

public class VexDebugHudPage extends CustomUIHud {
  private final String uiPath;
  private final Map<String, String> vars;

  public VexDebugHudPage(@Nonnull PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    super(playerRef);
    this.uiPath = uiPath;
    this.vars = vars;
  }

  @Override
  protected void build(UICommandBuilder commands) {
    String resolvedPath = UiAssetResolver.resolvePath(uiPath);
    String inline = UiAssetResolver.readInlineDocument(uiPath);
    if (inline != null) {
      commands.appendInline(null, inline);
    } else {
      commands.append(resolvedPath != null ? resolvedPath : uiPath);
    }
    for (Map.Entry<String, String> entry : vars.entrySet()) {
      String id = entry.getKey();
      if (!id.startsWith("#")) {
        id = "#" + id;
      }
      commands.set(id, entry.getValue());
    }
  }
}
