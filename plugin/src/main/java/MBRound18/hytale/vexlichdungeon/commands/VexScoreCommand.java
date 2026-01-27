package MBRound18.hytale.vexlichdungeon.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class VexScoreCommand extends AbstractCommand {
  public VexScoreCommand() {
    super("score", "Show Vex Lich leaderboard (WIP)");
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    context.sendMessage(Message.raw("[Vex] Leaderboard coming soon — test echo!"));
    return CompletableFuture.completedFuture(null);
  }
}
