package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemorowsUi;

public class DemoRowsPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemorowsUi.UI_PATH;

  public DemoRowsPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
