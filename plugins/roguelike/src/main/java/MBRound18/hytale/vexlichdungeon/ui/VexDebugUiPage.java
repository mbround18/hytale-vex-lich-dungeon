package MBRound18.hytale.vexlichdungeon.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import javax.annotation.Nonnull;

public class VexDebugUiPage extends CustomUIPage {
  private final String uiPath;
  private final Map<String, String> vars;

  public VexDebugUiPage(@Nonnull PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    super(playerRef, CustomPageLifetime.CanDismiss);
    this.uiPath = uiPath;
    this.vars = vars;
  }

  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events,
      Store<EntityStore> store) {
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
