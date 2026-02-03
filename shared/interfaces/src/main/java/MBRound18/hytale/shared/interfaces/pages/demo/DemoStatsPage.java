package MBRound18.hytale.shared.interfaces.pages.demo;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemostatsUi;

public class DemoStatsPage extends AbstractDemoPage {
  private static final String UI_PATH = DemosPagesDemostatsUi.UI_PATH;

  public DemoStatsPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, UI_PATH);
  }
}
