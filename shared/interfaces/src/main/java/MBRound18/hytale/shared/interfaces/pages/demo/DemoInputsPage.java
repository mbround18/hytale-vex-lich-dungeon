package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemoinputsUi;

public class DemoInputsPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemoinputsUi.UI_PATH;

  public DemoInputsPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
