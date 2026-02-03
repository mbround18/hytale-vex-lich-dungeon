package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemotabsUi;

public class DemoTabsPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemotabsUi.UI_PATH;

  public DemoTabsPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
