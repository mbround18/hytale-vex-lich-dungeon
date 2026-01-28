package MBRound18.PortalEngine.runtime;

/**
 * Strategy interface for run scoring.
 */
public interface ScoringStrategy {

  int scoreKill(String enemyType, int basePoints);

  int scoreRoomClear();

  int scoreRoundClear();
}