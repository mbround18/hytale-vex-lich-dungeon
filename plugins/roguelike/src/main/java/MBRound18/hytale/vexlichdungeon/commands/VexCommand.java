package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.ImmortalEngine.api.logging.EngineLog;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import javax.annotation.Nonnull;

public final class VexCommand extends AbstractCommandCollection {
  public VexCommand(@Nonnull DataStore dataStore, @Nonnull EngineLog log) {
    super("vex", "Vex The Lich command base");
    addAliases("vexlich", "vexdungeon");

    addSubCommand(new VexChallengeCommand(log, dataStore));
    addSubCommand(new VexUiCommand(dataStore));
    addSubCommand(new VexScoreCommand(dataStore));
    addSubCommand(new VexStartCommand());
    addSubCommand(new VexStopCommand());
    addSubCommand(new VexRemoveInstancesCommand(dataStore));
    addSubCommand(new VexPurgeCommand(dataStore));
  }
}
