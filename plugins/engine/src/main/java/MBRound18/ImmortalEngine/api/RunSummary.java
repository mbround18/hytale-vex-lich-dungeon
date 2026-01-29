package MBRound18.ImmortalEngine.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Snapshot summary emitted at the end of a run.
 */
public final class RunSummary {

  private final String instanceId;
  private final int totalScore;
  private final int totalKills;
  private final int roomsCleared;
  private final int roundsCleared;
  private final int safeRoomsVisited;
  private final List<PlayerSummary> players;

  public RunSummary(String instanceId, int totalScore, int totalKills, int roomsCleared,
      int roundsCleared, int safeRoomsVisited, List<PlayerSummary> players) {
    this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
    this.totalScore = totalScore;
    this.totalKills = totalKills;
    this.roomsCleared = roomsCleared;
    this.roundsCleared = roundsCleared;
    this.safeRoomsVisited = safeRoomsVisited;
    this.players = players == null ? List.of() : List.copyOf(players);
  }

  public String getInstanceId() {
    return instanceId;
  }

  public int getTotalScore() {
    return totalScore;
  }

  public int getTotalKills() {
    return totalKills;
  }

  public int getRoomsCleared() {
    return roomsCleared;
  }

  public int getRoundsCleared() {
    return roundsCleared;
  }

  public int getSafeRoomsVisited() {
    return safeRoomsVisited;
  }

  public List<PlayerSummary> getPlayers() {
    return Collections.unmodifiableList(players);
  }

  public static final class PlayerSummary {
    private final String playerId;
    private final String displayName;
    private final int score;
    private final int kills;

    public PlayerSummary(String playerId, String displayName, int score, int kills) {
      this.playerId = playerId;
      this.displayName = displayName;
      this.score = score;
      this.kills = kills;
    }

    public String getPlayerId() {
      return playerId;
    }

    public String getDisplayName() {
      return displayName;
    }

    public int getScore() {
      return score;
    }

    public int getKills() {
      return kills;
    }
  }
}