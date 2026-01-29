package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VexPurgeCommand extends AbstractCommand {
  private static final String PERMISSION_PURGE = "vex.purge";

  private final DataStore dataStore;

  public VexPurgeCommand(@Nonnull DataStore dataStore) {
    super("purge", "Remove all Vex dungeon instance data and delete dungeons.json");
    this.dataStore = dataStore;
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    if (!context.sender().hasPermission(PERMISSION_PURGE)) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.purge.permission")));
      return CompletableFuture.completedFuture(null);
    }

    int count = dataStore.getAllInstances().size();
    dataStore.clearAllInstances();

    boolean deleted = false;
    Path dungeonsPath = dataStore.getDataDirectory().resolve("dungeons.json");
    try {
      deleted = Files.deleteIfExists(dungeonsPath);
    } catch (Exception e) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.purge.failed", e.getMessage())));
      return CompletableFuture.completedFuture(null);
    }

    context.sendMessage(Message.raw(EngineLang.t("command.vex.purge.success", count, deleted)));
    return CompletableFuture.completedFuture(null);
  }
}
