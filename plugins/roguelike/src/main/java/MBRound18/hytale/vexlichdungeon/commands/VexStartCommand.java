package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class VexStartCommand extends AbstractCommand {
  public VexStartCommand() {
    super("start", "Start Vex The Lich Dungeon (WIP)");
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    context.sendMessage(Message.raw(EngineLang.t("command.vex.start.requested")));
    return CompletableFuture.completedFuture(null);
  }
}
