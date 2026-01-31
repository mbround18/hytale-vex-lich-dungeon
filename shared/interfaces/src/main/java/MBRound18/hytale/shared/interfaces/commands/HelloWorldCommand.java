package MBRound18.hytale.shared.interfaces.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Objects;
import javax.annotation.Nonnull;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCommand;
import MBRound18.hytale.shared.interfaces.debug.interactions.HelloInteractions;
import MBRound18.hytale.shared.interfaces.pages.HelloWorldPage;

public class HelloWorldCommand extends AbstractCommand<Object> {
  /**
   * Register the command metadata (name, description, confirmation requirement).
   */
  public HelloWorldCommand() {
    super(
        /**
         * Command Name
         */
        "helloworld",
        /**
         * Command Description
         */
        "Says Hello World to the Player via custom UI Page",
        /**
         * Requires confirmation
         */
        false,
        (ref, store) -> new Object());
  }

  @Override
  protected void execute(
      /**
       * Command Context
       */
      @Nonnull CommandContext context,
      /**
       * Entity Store
       */
      @Nonnull Store<EntityStore> store,
      /**
       * 
       */
      @Nonnull Ref<EntityStore> ref,
      /**
       * 
       */
      @Nonnull PlayerRef playerRef,
      /**
       * World
       */
      @Nonnull World world) {
    /**
     * Resolve the executing player from the entity store.
     */
    /**
     * Open the HelloWorldPage for the Player who executed the command
     */
    Player player = store.getComponent(ref, Player.getComponentType());
    player = Objects.requireNonNull(player, "player");
    HelloInteractions interactions = new HelloInteractions(ref, store);
    interactions.sendWrittenBy(player);
    /**
     * Build the page instance targeted to the executing player.
     */
    HelloWorldPage page = new HelloWorldPage(playerRef);
    /**
     * Open the custom UI page for the player.
     */
    player.getPageManager().openCustomPage(ref, store, page);
  }
}
