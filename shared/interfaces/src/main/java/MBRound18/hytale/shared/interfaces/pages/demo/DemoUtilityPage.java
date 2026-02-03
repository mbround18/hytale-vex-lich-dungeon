package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemoutilityUi;

public class DemoUtilityPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemoutilityUi.UI_PATH;

  public DemoUtilityPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
