package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemopaginationUi;

public class DemoPaginationPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemopaginationUi.UI_PATH;

  public DemoPaginationPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
