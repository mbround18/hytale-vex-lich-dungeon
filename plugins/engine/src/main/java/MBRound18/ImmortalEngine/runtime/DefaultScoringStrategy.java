package MBRound18.ImmortalEngine.runtime;

/**
 * Default scoring strategy that uses provided point values.
 */
public class DefaultScoringStrategy implements ScoringStrategy {

  @Override
  public int scoreKill(String enemyType, int basePoints) {
    return Math.max(0, basePoints);
  }

  @Override
  public int scoreRoomClear() {
    return 0;
  }

  @Override
  public int scoreRoundClear() {
    return 0;
  }
}