package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemotoastUi;

public class DemoToastPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemotoastUi.UI_PATH;

  public DemoToastPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
