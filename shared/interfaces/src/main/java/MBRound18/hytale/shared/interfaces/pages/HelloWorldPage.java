package MBRound18.hytale.shared.interfaces.pages;

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class HelloWorldPage
    /**
     * BasicCustomUIPage gives us a Simple UI without Event Handlingt
     * Alternatively, we can use InteractiveCustomUIPage which has event handling.
     */
    extends BasicCustomUIPage {

  /**
   * Constructor
   * 
   * @param playerRef
   */
  public HelloWorldPage(PlayerRef playerRef) {
    super(
        /**
         * PlayerRef which player sees the UI
         */
        playerRef,
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
    // Relative path from Common/UI/Custom/*
    builder.append("Pages/HelloWorldPage.ui");
  }

}
