package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.hytale.vexlichdungeon.VexLichDungeonPlugin;
import MBRound18.hytale.vexlichdungeon.prefab.PrefabDiscovery;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class VexPrefabCommand extends AbstractCommand {
  public VexPrefabCommand() {
    super("prefab", "Prefab utilities");
    addSubCommand(new ListSubCommand());
  }

  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    context.sendMessage(Message.raw("Usage: /vex prefab <list>"));
    return CompletableFuture.completedFuture(null);
  }

  private static final class ListSubCommand extends AbstractCommand {
    private static final String PERMISSION_LIST = "vex.prefab.list";

    private ListSubCommand() {
      super("list", "List available prefabs");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!context.sender().hasPermission(PERMISSION_LIST)) {
        context.sendMessage(Message.raw("Missing permission: " + PERMISSION_LIST));
        return CompletableFuture.completedFuture(null);
      }

      VexLichDungeonPlugin plugin = VexLichDungeonPlugin.getInstance();
      if (plugin == null) {
        context.sendMessage(Message.raw("VexLichDungeon plugin not initialized."));
        return CompletableFuture.completedFuture(null);
      }

      PrefabDiscovery discovery = plugin.getPrefabDiscovery();
      if (discovery == null) {
        context.sendMessage(Message.raw("Prefab discovery not available."));
        return CompletableFuture.completedFuture(null);
      }

      List<String> prefabs = discovery.getAllPrefabs();
      context.sendMessage(Message.raw("Prefabs: " + prefabs.size()));
      for (String prefab : prefabs) {
        context.sendMessage(Message.raw("- " + prefab));
      }
      return CompletableFuture.completedFuture(null);
    }
  }
}
