package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemogridUi;

public class DemoGridPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemogridUi.UI_PATH;

  public DemoGridPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
