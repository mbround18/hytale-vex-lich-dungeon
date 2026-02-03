package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemotoolbarUi;

public class DemoToolbarPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemotoolbarUi.UI_PATH;

  public DemoToolbarPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
