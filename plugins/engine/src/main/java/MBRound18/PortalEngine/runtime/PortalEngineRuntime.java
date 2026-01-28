package MBRound18.PortalEngine.runtime;

import MBRound18.PortalEngine.api.RunSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Headless runtime for portal-based minigames.
 */
public class PortalEngineRuntime {

  private final RunRegistry registry = new RunRegistry();
  private final ScoringStrategy scoring;

  public PortalEngineRuntime(ScoringStrategy scoring) {
    this.scoring = Objects.requireNonNull(scoring, "scoring");
  }

  public void onPortalEnter(String instanceId, String playerId, String displayName) {
    RunRecord run = registry.getOrCreate(instanceId);
    run.getPlayers().computeIfAbsent(playerId,
        id -> new RunRecord.PlayerRecord(id, displayName));
  }

  public void onKill(String instanceId, String playerId, String enemyType, int basePoints) {
    RunRecord run = registry.getOrCreate(instanceId);
    int points = scoring.scoreKill(enemyType, basePoints);
    run.addScore(points);
    run.addKill();
    RunRecord.PlayerRecord player = run.getPlayers().computeIfAbsent(playerId,
        id -> new RunRecord.PlayerRecord(id, playerId));
    player.addScore(points);
    player.addKill();
  }

  public void onRoomCleared(String instanceId) {
    RunRecord run = registry.getOrCreate(instanceId);
    run.addRoomClear();
    run.addScore(scoring.scoreRoomClear());
  }

  public void onRoundCleared(String instanceId) {
    RunRecord run = registry.getOrCreate(instanceId);
    run.addRoundClear();
    run.addScore(scoring.scoreRoundClear());
  }

  public void onSafeRoomVisited(String instanceId) {
    RunRecord run = registry.getOrCreate(instanceId);
    run.addSafeRoomVisited();
  }

  public RunSummary finalizeRun(String instanceId) {
    RunRecord run = registry.get(instanceId).orElse(null);
    if (run == null) {
      return null;
    }

    RunSummary summary = toSummary(run);
    registry.remove(instanceId);
    return summary;
  }

  public RunSummary toSummary(RunRecord run) {
    List<RunSummary.PlayerSummary> players = new ArrayList<>();
    for (RunRecord.PlayerRecord record : run.getPlayers().values()) {
      players.add(new RunSummary.PlayerSummary(
          record.getPlayerId(),
          record.getDisplayName(),
          record.getScore(),
          record.getKills()));
    }
    return new RunSummary(
        run.getInstanceId(),
        run.getTotalScore(),
        run.getTotalKills(),
        run.getRoomsCleared(),
        run.getRoundsCleared(),
        run.getSafeRoomsVisited(),
        players);
  }
}