package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemomodalUi;

public class DemoModalPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemomodalUi.UI_PATH;

  public DemoModalPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
