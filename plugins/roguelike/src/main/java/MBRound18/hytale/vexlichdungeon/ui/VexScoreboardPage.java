package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.ImmortalEngine.api.ui.UiPath;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class VexScoreboardPage extends CustomUIPage {
  private final String headerText;
  private final String bodyText;

  public VexScoreboardPage(PlayerRef playerRef, String headerText, String bodyText) {
    super(playerRef, CustomPageLifetime.CanDismiss);
    this.headerText = headerText;
    this.bodyText = bodyText;
  }

  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events,
      Store<EntityStore> store) {
    String uiPath = "Custom/Vex/Pages/VexScoreboard.ui";
    String resolvedPath = UiAssetResolver.resolvePath(uiPath);
    String inline = UiAssetResolver.readInlineDocument(uiPath);
    if (inline != null) {
      commands.appendInline(null, inline);
    } else {
      String clientPath = UiPath.normalizeForClient(resolvedPath != null ? resolvedPath : uiPath);
      commands.append(clientPath != null ? clientPath : uiPath);
    }
    if (headerText != null) {
      commands.set("#ScoreHeader.Text", headerText);
    }
    if (bodyText != null) {
      commands.set("#ScoreBody.Text", bodyText);
    }
  }
}
