package MBRound18.hytale.shared.interfaces.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import MBRound18.hytale.shared.interfaces.pages.FriendListPage;
import MBRound18.hytale.shared.interfaces.handlers.FriendPageHandler;
import MBRound18.hytale.shared.interfaces.debug.interactions.FriendInteractions;

public class FriendListCommand extends AbstractPlayerCommand {
  private final FriendPageHandler handler = new FriendInteractions();

  public FriendListCommand() {
    super("friends", "Opens the friends list UI", false);
  }

  @Override
  protected void execute(
      CommandContext context,
      Store<EntityStore> store,
      Ref<EntityStore> ref,
      PlayerRef playerRef,
      World world) {
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      return;
    }
    FriendListPage page = new FriendListPage(playerRef, handler);
    player.getPageManager().openCustomPage(ref, store, page);
  }
}