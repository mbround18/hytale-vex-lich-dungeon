package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VexRemoveInstancesCommand extends AbstractCommand {
  private static final String PERMISSION_REMOVE = "vex.removeinstances";

  private final DataStore dataStore;

  public VexRemoveInstancesCommand(@Nonnull DataStore dataStore) {
    super("removeinstances", "Remove all Vex dungeon instance data");
    this.dataStore = dataStore;
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    if (!context.sender().hasPermission(PERMISSION_REMOVE)) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.removeinstances.permission")));
      return CompletableFuture.completedFuture(null);
    }

    int count = dataStore.getAllInstances().size();
    dataStore.clearAllInstances();
    context.sendMessage(Message.raw(EngineLang.t("command.vex.removeinstances.success", count)));
    return CompletableFuture.completedFuture(null);
  }
}
