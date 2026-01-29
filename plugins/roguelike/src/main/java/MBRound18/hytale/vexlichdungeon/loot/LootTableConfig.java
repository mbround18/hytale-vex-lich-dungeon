package MBRound18.hytale.vexlichdungeon.loot;

import java.util.List;
import java.util.Map;

public class LootTableConfig {
  private List<LootTier> tiers;
  private List<LootCategory> categories;

  public List<LootTier> getTiers() {
    return tiers;
  }

  public void setTiers(List<LootTier> tiers) {
    this.tiers = tiers;
  }

  public List<LootCategory> getCategories() {
    return categories;
  }

  public void setCategories(List<LootCategory> categories) {
    this.categories = categories;
  }

  public static class LootTier {
    private String name;
    private int minScore;
    private int maxScore;
    private int baseRolls;
    private double rollsPerScore;
    private double rollsPerEnemyPoint;
    private int maxRolls;
    private Map<String, Integer> categoryWeights;

    public String getName() {
      return name;
    }

    public int getMinScore() {
      return minScore;
    }

    public int getMaxScore() {
      return maxScore;
    }

    public int getBaseRolls() {
      return baseRolls;
    }

    public double getRollsPerScore() {
      return rollsPerScore;
    }

    public double getRollsPerEnemyPoint() {
      return rollsPerEnemyPoint;
    }

    public int getMaxRolls() {
      return maxRolls;
    }

    public Map<String, Integer> getCategoryWeights() {
      return categoryWeights;
    }
  }

  public static class LootCategory {
    private String id;
    private String path;
    private int minCount;
    private int maxCount;
    private List<RarityRule> rarityRules;

    public String getId() {
      return id;
    }

    public String getPath() {
      return path;
    }

    public int getMinCount() {
      return minCount;
    }

    public int getMaxCount() {
      return maxCount;
    }

    public List<RarityRule> getRarityRules() {
      return rarityRules;
    }
  }

  public static class RarityRule {
    private String match;
    private double weight;

    public String getMatch() {
      return match;
    }

    public double getWeight() {
      return weight;
    }
  }
}
