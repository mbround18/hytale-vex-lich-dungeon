package MBRound18.hytale.vexlichdungeon.loot;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class LootService {
  private final LootTableConfig config;
  private final LootCatalog catalog;
  private final Random random;
  private final EngineLog log;

  public LootService(LootTableConfig config, LootCatalog catalog, long seed, EngineLog log) {
    this.config = config;
    this.catalog = catalog;
    this.random = new Random(seed);
    this.log = log;
  }

  public List<LootRoll> generateLoot(int totalScore, int roomEnemyPoints) {
    if (config == null || config.getTiers() == null || config.getTiers().isEmpty()) {
      return List.of();
    }

    LootTableConfig.LootTier tier = pickTier(totalScore);
    if (tier == null || tier.getCategoryWeights() == null || tier.getCategoryWeights().isEmpty()) {
      return List.of();
    }

    int rolls = tier.getBaseRolls();
    rolls += (int) Math.floor(totalScore * Math.max(0.0, tier.getRollsPerScore()));
    rolls += (int) Math.floor(roomEnemyPoints * Math.max(0.0, tier.getRollsPerEnemyPoint()));
    if (tier.getMaxRolls() > 0) {
      rolls = Math.min(rolls, tier.getMaxRolls());
    }
    rolls = Math.max(0, rolls);

    List<LootRoll> results = new ArrayList<>();
    for (int i = 0; i < rolls; i++) {
      String categoryId = pickWeightedCategory(tier.getCategoryWeights());
      if (categoryId == null) {
        continue;
      }
      LootTableConfig.LootCategory category = getCategory(categoryId);
      if (category == null) {
        continue;
      }
      List<String> items = catalog.getItems(categoryId);
      if (items.isEmpty()) {
        if (log != null) {
          log.warn("No items found for loot category %s", categoryId);
        }
        continue;
      }
      int count = rollCount(category.getMinCount(), category.getMaxCount());
      String item = pickWeightedItem(items, category.getRarityRules());
      if (item != null) {
        results.add(new LootRoll(item, Math.max(1, count)));
      }
    }
    return results;
  }

  private LootTableConfig.LootTier pickTier(int totalScore) {
    LootTableConfig.LootTier fallback = null;
    for (LootTableConfig.LootTier tier : config.getTiers()) {
      if (tier == null) {
        continue;
      }
      if (totalScore >= tier.getMinScore() && totalScore <= tier.getMaxScore()) {
        return tier;
      }
      if (fallback == null && totalScore >= tier.getMinScore()) {
        fallback = tier;
      }
    }
    return fallback;
  }

  private LootTableConfig.LootCategory getCategory(String id) {
    if (config.getCategories() == null) {
      return null;
    }
    for (LootTableConfig.LootCategory category : config.getCategories()) {
      if (category != null && id.equals(category.getId())) {
        return category;
      }
    }
    return null;
  }

  private String pickWeightedCategory(Map<String, Integer> weights) {
    int total = 0;
    for (Integer weight : weights.values()) {
      total += Math.max(0, weight);
    }
    if (total <= 0) {
      return null;
    }
    int roll = random.nextInt(total);
    int running = 0;
    for (Map.Entry<String, Integer> entry : weights.entrySet()) {
      running += Math.max(0, entry.getValue());
      if (roll < running) {
        return entry.getKey();
      }
    }
    return null;
  }

  private String pickWeightedItem(List<String> items, List<LootTableConfig.RarityRule> rules) {
    if (rules == null || rules.isEmpty()) {
      return items.get(random.nextInt(items.size()));
    }
    double total = 0.0;
    double[] weights = new double[items.size()];
    for (int i = 0; i < items.size(); i++) {
      String item = items.get(i);
      double weight = 1.0;
      for (LootTableConfig.RarityRule rule : rules) {
        if (rule != null && rule.getMatch() != null
            && item.toLowerCase().contains(rule.getMatch().toLowerCase())) {
          weight *= Math.max(0.0, rule.getWeight());
        }
      }
      weights[i] = weight;
      total += weight;
    }
    if (total <= 0.0) {
      return items.get(random.nextInt(items.size()));
    }
    double roll = random.nextDouble() * total;
    double running = 0.0;
    for (int i = 0; i < items.size(); i++) {
      running += weights[i];
      if (roll <= running) {
        return items.get(i);
      }
    }
    return items.get(random.nextInt(items.size()));
  }

  private int rollCount(int min, int max) {
    int safeMin = Math.max(1, min);
    int safeMax = Math.max(safeMin, max);
    if (safeMin == safeMax) {
      return safeMin;
    }
    return safeMin + random.nextInt(safeMax - safeMin + 1);
  }

  public static final class LootRoll {
    private final String itemId;
    private final int count;

    public LootRoll(String itemId, int count) {
      this.itemId = itemId;
      this.count = count;
    }

    public String getItemId() {
      return itemId;
    }

    public int getCount() {
      return count;
    }
  }
}
