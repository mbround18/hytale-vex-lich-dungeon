package MBRound18.hytale.vexlichdungeon.data;

import java.util.*;
import javax.annotation.Nonnull;

/**
 * Tracks data for a single dungeon instance.
 */
public class DungeonInstanceData {

  private String worldName;
  private boolean generated;
  private long generatedTimestamp;
  private long seed;
  private int tileCount;
  private Map<String, PlayerProgress> playerProgress = new HashMap<>();
  private List<String> completedBy = new ArrayList<>();
  private List<String> playersSeen = new ArrayList<>();
  private List<String> currentPlayers = new ArrayList<>();
  private int totalScore;
  private int totalKills;
  private int roomsCleared;
  private int roomsClearedThisRound;
  private int roundsCleared;
  private int safeRoomsVisited;

  public String getWorldName() {
    return worldName;
  }

  public void setWorldName(String worldName) {
    this.worldName = worldName;
  }

  public boolean isGenerated() {
    return generated;
  }

  public void setGenerated(boolean generated) {
    this.generated = generated;
  }

  public long getGeneratedTimestamp() {
    return generatedTimestamp;
  }

  public void setGeneratedTimestamp(long generatedTimestamp) {
    this.generatedTimestamp = generatedTimestamp;
  }

  public long getSeed() {
    return seed;
  }

  public void setSeed(long seed) {
    this.seed = seed;
  }

  public int getTileCount() {
    return tileCount;
  }

  public void setTileCount(int tileCount) {
    this.tileCount = tileCount;
  }

  public Map<String, PlayerProgress> getPlayerProgress() {
    return playerProgress;
  }

  public void setPlayerProgress(Map<String, PlayerProgress> playerProgress) {
    this.playerProgress = playerProgress;
  }

  public List<String> getCompletedBy() {
    return completedBy;
  }

  public void setCompletedBy(List<String> completedBy) {
    this.completedBy = completedBy;
  }

  public List<String> getPlayersSeen() {
    return playersSeen;
  }

  public void setPlayersSeen(List<String> playersSeen) {
    this.playersSeen = playersSeen;
  }

  public List<String> getCurrentPlayers() {
    return currentPlayers;
  }

  public void setCurrentPlayers(List<String> currentPlayers) {
    this.currentPlayers = currentPlayers;
  }

  public int getTotalScore() {
    return totalScore;
  }

  public void setTotalScore(int totalScore) {
    this.totalScore = totalScore;
  }

  public int getTotalKills() {
    return totalKills;
  }

  public void setTotalKills(int totalKills) {
    this.totalKills = totalKills;
  }

  public int getRoomsCleared() {
    return roomsCleared;
  }

  public void setRoomsCleared(int roomsCleared) {
    this.roomsCleared = roomsCleared;
  }

  public int getRoomsClearedThisRound() {
    return roomsClearedThisRound;
  }

  public void setRoomsClearedThisRound(int roomsClearedThisRound) {
    this.roomsClearedThisRound = roomsClearedThisRound;
  }

  public int getRoundsCleared() {
    return roundsCleared;
  }

  public void setRoundsCleared(int roundsCleared) {
    this.roundsCleared = roundsCleared;
  }

  public int getSafeRoomsVisited() {
    return safeRoomsVisited;
  }

  public void setSafeRoomsVisited(int safeRoomsVisited) {
    this.safeRoomsVisited = safeRoomsVisited;
  }

  /**
   * Player progress within a dungeon instance.
   */
  public static class PlayerProgress {
    private String playerUuid;
    private String playerName;
    private int score;
    private long startTime;
    private long completionTime;
    private boolean completed;
    private int deaths;
    private int enemiesKilled;
    private long lastSeen;
    private boolean currentlyInInstance;

    public String getPlayerUuid() {
      return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
      this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
      return playerName;
    }

    public void setPlayerName(String playerName) {
      this.playerName = playerName;
    }

    public int getScore() {
      return score;
    }

    public void setScore(int score) {
      this.score = score;
    }

    public long getStartTime() {
      return startTime;
    }

    public void setStartTime(long startTime) {
      this.startTime = startTime;
    }

    public long getCompletionTime() {
      return completionTime;
    }

    public void setCompletionTime(long completionTime) {
      this.completionTime = completionTime;
    }

    public boolean isCompleted() {
      return completed;
    }

    public void setCompleted(boolean completed) {
      this.completed = completed;
    }

    public int getDeaths() {
      return deaths;
    }

    public void setDeaths(int deaths) {
      this.deaths = deaths;
    }

    public int getEnemiesKilled() {
      return enemiesKilled;
    }

    public void setEnemiesKilled(int enemiesKilled) {
      this.enemiesKilled = enemiesKilled;
    }

    public long getLastSeen() {
      return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
      this.lastSeen = lastSeen;
    }

    public boolean isCurrentlyInInstance() {
      return currentlyInInstance;
    }

    public void setCurrentlyInInstance(boolean currentlyInInstance) {
      this.currentlyInInstance = currentlyInInstance;
    }
  }

  @Nonnull
  public static DungeonInstanceData create(@Nonnull String worldName, long seed, int tileCount) {
    DungeonInstanceData data = new DungeonInstanceData();
    data.setWorldName(worldName);
    data.setGenerated(true);
    data.setGeneratedTimestamp(System.currentTimeMillis());
    data.setSeed(seed);
    data.setTileCount(tileCount);
    return data;
  }
}
