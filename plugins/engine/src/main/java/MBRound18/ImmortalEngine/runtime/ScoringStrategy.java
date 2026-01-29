package MBRound18.ImmortalEngine.runtime;

/**
 * Strategy interface for run scoring.
 */
public interface ScoringStrategy {

  int scoreKill(String enemyType, int basePoints);

  int scoreRoomClear();

  int scoreRoundClear();
}