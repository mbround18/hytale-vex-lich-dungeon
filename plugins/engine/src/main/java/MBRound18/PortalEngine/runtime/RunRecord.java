package MBRound18.PortalEngine.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable run state used by the runtime.
 */
public class RunRecord {

  private final String instanceId;
  private final Map<String, PlayerRecord> players = new HashMap<>();
  private int totalScore;
  private int totalKills;
  private int roomsCleared;
  private int roundsCleared;
  private int safeRoomsVisited;

  public RunRecord(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public Map<String, PlayerRecord> getPlayers() {
    return players;
  }

  public int getTotalScore() {
    return totalScore;
  }

  public void addScore(int points) {
    totalScore += Math.max(0, points);
  }

  public int getTotalKills() {
    return totalKills;
  }

  public void addKill() {
    totalKills++;
  }

  public int getRoomsCleared() {
    return roomsCleared;
  }

  public void addRoomClear() {
    roomsCleared++;
  }

  public int getRoundsCleared() {
    return roundsCleared;
  }

  public void addRoundClear() {
    roundsCleared++;
  }

  public int getSafeRoomsVisited() {
    return safeRoomsVisited;
  }

  public void addSafeRoomVisited() {
    safeRoomsVisited++;
  }

  public static class PlayerRecord {
    private final String playerId;
    private String displayName;
    private int score;
    private int kills;

    public PlayerRecord(String playerId, String displayName) {
      this.playerId = playerId;
      this.displayName = displayName;
    }

    public String getPlayerId() {
      return playerId;
    }

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public int getScore() {
      return score;
    }

    public void addScore(int points) {
      score += Math.max(0, points);
    }

    public int getKills() {
      return kills;
    }

    public void addKill() {
      kills++;
    }
  }
}