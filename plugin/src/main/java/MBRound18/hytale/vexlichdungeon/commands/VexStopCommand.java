package MBRound18.hytale.vexlichdungeon.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class VexStopCommand extends AbstractCommand {
  public VexStopCommand() {
    super("stop", "Stop Vex The Lich Dungeon (WIP)");
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    context.sendMessage(Message.raw("[Vex] Stop requested — test echo!"));
    return CompletableFuture.completedFuture(null);
  }
}
