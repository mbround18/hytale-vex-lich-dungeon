package MBRound18.hytale.vexlichdungeon.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class VexCommand extends AbstractCommand {
  public VexCommand() {
    super("vex", "Vex dungeon utilities");
    addSubCommand(new VexUiCommand());
  }

  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    context.sendMessage(Message.raw("Usage: /vex ui <list|test>"));
    return CompletableFuture.completedFuture(null);
  }
}
