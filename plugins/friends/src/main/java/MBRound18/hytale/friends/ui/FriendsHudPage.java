package MBRound18.hytale.friends.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import javax.annotation.Nonnull;

public class FriendsHudPage extends CustomUIHud {
  private final String uiPath;
  private final Map<String, String> vars;

  public FriendsHudPage(@Nonnull PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    super(playerRef);
    this.uiPath = uiPath;
    this.vars = vars;
  }

  @Override
  protected void build(UICommandBuilder commands) {
    String resolvedPath = FriendsAssetResolver.resolvePath(uiPath);
    String inline = FriendsAssetResolver.readInlineDocument(uiPath);
    if (inline != null) {
      commands.appendInline(resolvedPath != null ? resolvedPath : uiPath, inline);
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
