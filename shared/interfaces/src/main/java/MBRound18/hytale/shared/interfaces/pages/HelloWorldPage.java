package MBRound18.hytale.shared.interfaces.pages;

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Objects;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.PagesHelloworldpageUi;

public class HelloWorldPage
    /**
     * BasicCustomUIPage gives us a Simple UI without Event Handlingt
     * Alternatively, we can use InteractiveCustomUIPage which has event handling.
     */
    extends BasicCustomUIPage {
  private static final String UI_PATH = PagesHelloworldpageUi.UI_PATH;

  /**
   * Constructor
   * 
   * @param playerRef
   */
  public HelloWorldPage(@Nonnull PlayerRef playerRef) {
    super(
        /**
         * PlayerRef which player sees the UI
         */
        Objects.requireNonNull(playerRef, "playerRef"),
        /**
         * CantClose, Player cannot close the UI
         * CanDismiss, ESC to close
         * CanDismissOrCloseThroughInteraction, like using esc key or clicking outside
         * the UI
         */
        CustomPageLifetime.CanDismissOrCloseThroughInteraction);
  }

  /**
   * Build Called once to load the UI file, NO SETS ALLOWED!
   * Instead set the default values within the UI file itself.
   * 
   * @param builder
   */
  @Override
  public void build(UICommandBuilder builder) {
    // POM-generated UI path
    builder.append(UI_PATH);
  }

}
