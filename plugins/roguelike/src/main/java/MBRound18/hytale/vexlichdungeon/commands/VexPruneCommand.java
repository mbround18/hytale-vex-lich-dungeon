package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.hytale.vexlichdungeon.VexLichDungeonPlugin;
import MBRound18.hytale.vexlichdungeon.events.DungeonGenerationEventHandler;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class VexPruneCommand extends AbstractCommand {
  private static final String PERMISSION_PRUNE = "vex.prune";

  public VexPruneCommand() {
    super("prune", "Prune empty Vex dungeon instances");
  }

  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    if (!context.sender().hasPermission(PERMISSION_PRUNE)) {
      context.sendMessage(Message.raw("Missing permission: " + PERMISSION_PRUNE));
      return CompletableFuture.completedFuture(null);
    }

    VexLichDungeonPlugin plugin = VexLichDungeonPlugin.getInstance();
    if (plugin == null) {
      context.sendMessage(Message.raw("VexLichDungeon plugin not initialized."));
      return CompletableFuture.completedFuture(null);
    }

    DungeonGenerationEventHandler handler = plugin.getDungeonEventHandler();
    if (handler == null) {
      context.sendMessage(Message.raw("Dungeon event handler not available."));
      return CompletableFuture.completedFuture(null);
    }

    int removed = handler.pruneEmptyInstances();
    context.sendMessage(Message.raw("Pruned " + removed + " empty instance(s)."));
    return CompletableFuture.completedFuture(null);
  }
}
