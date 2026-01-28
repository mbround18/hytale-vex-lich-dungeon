package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.PortalEngine.api.i18n.EngineLang;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.data.DungeonInstanceData;
import MBRound18.hytale.vexlichdungeon.ui.UIController;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VexScoreCommand extends AbstractCommand {
  private static final String PERMISSION_VIEW = "vex.score.view";
  private static final String PERMISSION_EDIT = "vex.score.edit";

  private final DataStore dataStore;

  public VexScoreCommand(@Nonnull DataStore dataStore) {
    super("score", "Show Vex Lich leaderboard");
    this.dataStore = dataStore;
    setAllowsExtraArguments(true);
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    String[] tokens = tokenize(context.getInputString());
    int index = skipCommandTokens(tokens, "score");
    if (index < tokens.length && "edit".equalsIgnoreCase(tokens[index])) {
      return handleEdit(context, tokens, index + 1);
    }
    return handleShow(context);
  }

  private CompletableFuture<Void> handleShow(@Nonnull CommandContext context) {
    if (!context.sender().hasPermission(PERMISSION_VIEW)) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.score.view.permission")));
      return CompletableFuture.completedFuture(null);
    }

    ScoreSnapshot snapshot = buildSnapshot();
    String body = snapshot.toBodyString();

    if (context.isPlayer()) {
      PlayerRef playerRef = com.hypixel.hytale.server.core.universe.Universe.get()
          .getPlayer(context.sender().getUuid());
      if (!UIController.openScoreboard(playerRef, null, body)) {
        context.sendMessage(Message.raw(body));
      }
    } else {
      context.sendMessage(Message.raw(body));
    }
    return CompletableFuture.completedFuture(null);
  }

  private CompletableFuture<Void> handleEdit(@Nonnull CommandContext context, String[] tokens, int startIndex) {
    if (!context.sender().hasPermission(PERMISSION_EDIT)) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.score.edit.permission")));
      return CompletableFuture.completedFuture(null);
    }
    if (tokens.length - startIndex < 3) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.score.edit.usage")));
      return CompletableFuture.completedFuture(null);
    }

    String playerId = tokens[startIndex];
    String metric = tokens[startIndex + 1].toLowerCase(Locale.ROOT);
    String valueRaw = tokens[startIndex + 2];

    int value;
    try {
      value = Integer.parseInt(valueRaw);
    } catch (NumberFormatException e) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.score.edit.valueNumber")));
      return CompletableFuture.completedFuture(null);
    }

    DungeonInstanceData targetInstance = null;
    DungeonInstanceData.PlayerProgress targetProgress = null;
    for (DungeonInstanceData instance : dataStore.getAllInstances()) {
      for (Map.Entry<String, DungeonInstanceData.PlayerProgress> entry : instance.getPlayerProgress().entrySet()) {
        DungeonInstanceData.PlayerProgress progress = entry.getValue();
        if (entry.getKey().equalsIgnoreCase(playerId) ||
            (progress.getPlayerName() != null && progress.getPlayerName().equalsIgnoreCase(playerId))) {
          targetInstance = instance;
          targetProgress = progress;
          break;
        }
      }
      if (targetProgress != null) {
        break;
      }
    }

    if (targetInstance == null) {
      targetInstance = dataStore.getOrCreateInstance("Vex_Debug");
    }
    if (targetProgress == null) {
      targetProgress = targetInstance.getPlayerProgress().computeIfAbsent(playerId, id -> {
        DungeonInstanceData.PlayerProgress created = new DungeonInstanceData.PlayerProgress();
        created.setPlayerUuid(id);
        return created;
      });
    }

    boolean handled = applyMetric(targetInstance, targetProgress, metric, value);
    if (!handled) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.score.edit.unknownMetric", metric)));
      return CompletableFuture.completedFuture(null);
    }

    dataStore.saveInstances();
    context.sendMessage(Message.raw(EngineLang.t("command.vex.score.edit.updated", playerId, metric, value)));
    return CompletableFuture.completedFuture(null);
  }

  private boolean applyMetric(DungeonInstanceData instance, DungeonInstanceData.PlayerProgress progress,
      String metric, int value) {
    switch (metric) {
      case "score":
      case "playerscore":
        progress.setScore(value);
        return true;
      case "kills":
      case "enemieskilled":
        progress.setEnemiesKilled(value);
        return true;
      case "deaths":
        progress.setDeaths(value);
        return true;
      case "totalscore":
      case "groupscore":
        instance.setTotalScore(value);
        return true;
      case "totalkills":
        instance.setTotalKills(value);
        return true;
      case "roomscleared":
        instance.setRoomsCleared(value);
        return true;
      case "roundscleared":
      case "roundscleareds":
      case "rounds":
        instance.setRoundsCleared(value);
        return true;
      case "saferooms":
      case "saferoomsvisited":
        instance.setSafeRoomsVisited(value);
        return true;
      default:
        return false;
    }
  }

  private ScoreSnapshot buildSnapshot() {
    int bestGroupScore = 0;
    DungeonInstanceData bestInstance = null;
    DungeonInstanceData.PlayerProgress bestPlayer = null;
    int bestPlayerScore = 0;

    for (DungeonInstanceData instance : dataStore.getAllInstances()) {
      if (instance.getTotalScore() > bestGroupScore) {
        bestGroupScore = instance.getTotalScore();
        bestInstance = instance;
      }
      for (DungeonInstanceData.PlayerProgress progress : instance.getPlayerProgress().values()) {
        if (progress.getScore() > bestPlayerScore) {
          bestPlayerScore = progress.getScore();
          bestPlayer = progress;
        }
      }
    }

    return new ScoreSnapshot(bestInstance, bestGroupScore, bestPlayer, bestPlayerScore);
  }

  private static String[] tokenize(String input) {
    String trimmed = input == null ? "" : input.trim();
    if (trimmed.isEmpty()) {
      return new String[0];
    }
    return trimmed.split("\\s+");
  }

  private static int skipCommandTokens(String[] tokens, String commandName) {
    int index = 0;
    if (index < tokens.length && "vex".equalsIgnoreCase(tokens[index])) {
      index++;
    }
    if (index < tokens.length && commandName.equalsIgnoreCase(tokens[index])) {
      index++;
    }
    return index;
  }

  private static final class ScoreSnapshot {
    private final DungeonInstanceData instance;
    private final int groupScore;
    private final DungeonInstanceData.PlayerProgress bestPlayer;
    private final int bestPlayerScore;

    private ScoreSnapshot(DungeonInstanceData instance, int groupScore,
        DungeonInstanceData.PlayerProgress bestPlayer, int bestPlayerScore) {
      this.instance = instance;
      this.groupScore = groupScore;
      this.bestPlayer = bestPlayer;
      this.bestPlayerScore = bestPlayerScore;
    }

    private String toBodyString() {
      if (instance == null && bestPlayer == null) {
        return EngineLang.t("customUI.vexScoreboard.empty");
      }
      StringBuilder builder = new StringBuilder();
      if (instance != null) {
        builder.append(EngineLang.t("customUI.vexScoreboard.topRun", groupScore, instance.getRoundsCleared()));
        builder.append("\n");
      }
      if (bestPlayer != null) {
        String name = bestPlayer.getPlayerName() != null ? bestPlayer.getPlayerName() : bestPlayer.getPlayerUuid();
        builder.append(EngineLang.t("customUI.vexScoreboard.topPlayer", name, bestPlayerScore));
      }
      return builder.toString();
    }
  }
}
