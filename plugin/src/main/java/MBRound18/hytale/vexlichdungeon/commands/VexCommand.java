package MBRound18.hytale.vexlichdungeon.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class VexCommand extends AbstractCommandCollection {
  public VexCommand() {
    super("vex", "Vex The Lich command base");
    addAliases("vexlich", "vexdungeon");

    addSubCommand(new VexScoreCommand());
    addSubCommand(new VexStartCommand());
    addSubCommand(new VexStopCommand());
  }
}
