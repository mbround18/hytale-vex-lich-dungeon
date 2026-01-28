package MBRound18.hytale.vexlichdungeon.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Custom UI page for dungeon run summary.
 */
public class VexDungeonSummaryPage extends CustomUIPage {

  private final String statsText;
  private final String bodyText;

  public VexDungeonSummaryPage(PlayerRef playerRef, String statsText, String bodyText) {
    super(playerRef, CustomPageLifetime.CanDismiss);
    this.statsText = statsText;
    this.bodyText = bodyText;
  }

  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events,
      Store<EntityStore> store) {
    String uiPath = "UI/Custom/Vex/Pages/VexDungeonSummary.ui";
    String resolvedPath = UiAssetResolver.resolvePath(uiPath);
    String inline = UiAssetResolver.readInlineDocument(uiPath);
    if (inline != null) {
      commands.appendInline(null, inline);
    } else {
      commands.append(resolvedPath != null ? resolvedPath : uiPath);
    }
    commands.set("#SummaryStats", statsText);
    commands.set("#SummaryBody", bodyText);
  }
}