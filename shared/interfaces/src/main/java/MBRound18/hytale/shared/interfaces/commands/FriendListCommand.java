package MBRound18.hytale.shared.interfaces.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import MBRound18.hytale.shared.interfaces.abstracts.AbstractCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import MBRound18.hytale.shared.interfaces.pages.FriendListPage;
import MBRound18.hytale.shared.interfaces.debug.interactions.FriendInteractions;
import MBRound18.hytale.shared.interfaces.interfaces.handlers.FriendPageHandler;

import java.util.Objects;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;

public class FriendListCommand extends AbstractCommand<FriendPageHandler> {

  public FriendListCommand(
      @Nonnull BiFunction<Ref<EntityStore>, Store<EntityStore>, FriendPageHandler> handlerFactory) {
    super("friends", "Opens the friends list UI", false, handlerFactory);
  }

  public FriendListCommand() {
    this(FriendInteractions::new);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef,
      @Nonnull World world) {
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      return;
    }
    FriendPageHandler handler = Objects.requireNonNull(handlerFactory.apply(ref, store), "handler");
    FriendListPage page = new FriendListPage(playerRef, handler);
    player.getPageManager().openCustomPage(Objects.requireNonNull(ref, "ref"), store, page);
  }
}