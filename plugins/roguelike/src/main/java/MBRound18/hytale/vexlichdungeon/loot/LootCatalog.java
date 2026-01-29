package MBRound18.hytale.vexlichdungeon.loot;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LootCatalog {
  private final Map<String, List<String>> categoryItems = new HashMap<>();

  public void load(Path itemsRoot, LootTableConfig config, EngineLog log) {
    if (itemsRoot == null || config == null || config.getCategories() == null) {
      return;
    }
    for (LootTableConfig.LootCategory category : config.getCategories()) {
      if (category.getId() == null || category.getPath() == null) {
        continue;
      }
      Path categoryPath = itemsRoot.resolve(category.getPath());
      List<String> items = scanCategory(categoryPath, log);
      categoryItems.put(category.getId(), items);
    }
  }

  public List<String> getItems(String categoryId) {
    return categoryItems.getOrDefault(categoryId, List.of());
  }

  private List<String> scanCategory(Path path, EngineLog log) {
    List<String> items = new ArrayList<>();
    if (path == null || !Files.exists(path)) {
      if (log != null) {
        log.warn("Loot category path missing: %s", path);
      }
      return items;
    }
    try {
      Files.walk(path)
          .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
          .forEach(p -> {
            String name = p.getFileName().toString();
            if (name.endsWith(".json")) {
              items.add(name.substring(0, name.length() - ".json".length()));
            }
          });
    } catch (IOException e) {
      if (log != null) {
        log.warn("Failed to scan loot items at %s: %s", path, e.getMessage());
      }
    }
    return items;
  }
}
