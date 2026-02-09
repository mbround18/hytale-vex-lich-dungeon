package MBRound18.hytale.vexlichdungeon.data;

import javax.annotation.Nonnull;

/**
 * Configuration settings for the VexLichDungeon plugin.
 * Loaded from config.json in the plugin data directory.
 */
public class DungeonConfig {

  // Generation settings
  private int radius = 5;
  private int tileSize = 19;
  private double roomProbability = 0.70;
  private int targetTileCount = 21;
  private int batchSize = 10;
  private int safeRoomBaseRooms = 5;
  private double safeRoomScaleFactor = 1.0;
  private int eventRoomInterval = 8;

  // Gameplay settings
  private boolean autoGenerateOnJoin = true;
  private boolean allowRegeneration = false;
  private int maxPlayersPerInstance = 4;
  private boolean removeInstanceWhenEmpty = true;

  // Rewards and progression
  private boolean enableScoring = true;
  private int completionReward = 1000;
  private boolean enableLeaderboard = true;
  private java.util.List<String> prefabPrefixAllowList = new java.util.ArrayList<>();

  public int getRadius() {
    return radius;
  }

  public void setRadius(int radius) {
    this.radius = radius;
  }

  public int getTileSize() {
    return tileSize;
  }

  public void setTileSize(int tileSize) {
    this.tileSize = tileSize;
  }

  public double getRoomProbability() {
    return roomProbability;
  }

  public void setRoomProbability(double roomProbability) {
    this.roomProbability = roomProbability;
  }

  public int getTargetTileCount() {
    return targetTileCount;
  }

  public void setTargetTileCount(int targetTileCount) {
    this.targetTileCount = targetTileCount;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public int getSafeRoomBaseRooms() {
    return safeRoomBaseRooms;
  }

  public void setSafeRoomBaseRooms(int safeRoomBaseRooms) {
    this.safeRoomBaseRooms = safeRoomBaseRooms;
  }

  public double getSafeRoomScaleFactor() {
    return safeRoomScaleFactor;
  }

  public void setSafeRoomScaleFactor(double safeRoomScaleFactor) {
    this.safeRoomScaleFactor = safeRoomScaleFactor;
  }

  /**
   * Gets how many rooms must be visited before spawning an event room.
   */
  public int getEventRoomInterval() {
    return eventRoomInterval;
  }

  /**
   * Sets how many rooms must be visited before spawning an event room.
   */
  public void setEventRoomInterval(int eventRoomInterval) {
    this.eventRoomInterval = Math.max(1, eventRoomInterval);
  }

  public boolean isAutoGenerateOnJoin() {
    return autoGenerateOnJoin;
  }

  public void setAutoGenerateOnJoin(boolean autoGenerateOnJoin) {
    this.autoGenerateOnJoin = autoGenerateOnJoin;
  }

  public boolean isAllowRegeneration() {
    return allowRegeneration;
  }

  public void setAllowRegeneration(boolean allowRegeneration) {
    this.allowRegeneration = allowRegeneration;
  }

  public int getMaxPlayersPerInstance() {
    return maxPlayersPerInstance;
  }

  public void setMaxPlayersPerInstance(int maxPlayersPerInstance) {
    this.maxPlayersPerInstance = maxPlayersPerInstance;
  }

  public boolean isRemoveInstanceWhenEmpty() {
    return removeInstanceWhenEmpty;
  }

  public void setRemoveInstanceWhenEmpty(boolean removeInstanceWhenEmpty) {
    this.removeInstanceWhenEmpty = removeInstanceWhenEmpty;
  }

  public boolean isEnableScoring() {
    return enableScoring;
  }

  public void setEnableScoring(boolean enableScoring) {
    this.enableScoring = enableScoring;
  }

  public int getCompletionReward() {
    return completionReward;
  }

  public void setCompletionReward(int completionReward) {
    this.completionReward = completionReward;
  }

  public boolean isEnableLeaderboard() {
    return enableLeaderboard;
  }

  public void setEnableLeaderboard(boolean enableLeaderboard) {
    this.enableLeaderboard = enableLeaderboard;
  }

  @Nonnull
  public java.util.List<String> getPrefabPrefixAllowList() {
    if (prefabPrefixAllowList == null || prefabPrefixAllowList.isEmpty()) {
      return new java.util.ArrayList<>();
    }
    return new java.util.ArrayList<>(prefabPrefixAllowList);
  }

  public void setPrefabPrefixAllowList(java.util.List<String> prefabPrefixAllowList) {
    if (prefabPrefixAllowList == null || prefabPrefixAllowList.isEmpty()) {
      this.prefabPrefixAllowList = new java.util.ArrayList<>();
      return;
    }
    this.prefabPrefixAllowList = new java.util.ArrayList<>(prefabPrefixAllowList);
  }

  /**
   * Creates a default configuration with sensible defaults.
   */
  @Nonnull
  public static DungeonConfig createDefault() {
    return new DungeonConfig();
  }
}
