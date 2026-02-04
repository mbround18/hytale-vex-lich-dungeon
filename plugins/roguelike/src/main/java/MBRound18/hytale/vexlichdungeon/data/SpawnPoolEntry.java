package MBRound18.hytale.vexlichdungeon.data;

import javax.annotation.Nonnull;

/**
 * Single spawn pool entry.
 */
public class SpawnPoolEntry {

  private String enemy;
  private int points;

  public SpawnPoolEntry() {
  }

  public SpawnPoolEntry(@Nonnull String enemy, int points) {
    this.enemy = enemy;
    this.points = points;
  }

  public String getEnemy() {
    return enemy;
  }

  public void setEnemy(String enemy) {
    this.enemy = enemy;
  }

  public int getPoints() {
    return points;
  }

  public void setPoints(int points) {
    this.points = points;
  }
}
