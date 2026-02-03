package MBRound18.hytale.vexlichdungeon.loot;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;

public final class LootTableLoader {
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Nullable
  public LootTableConfig load(Path path, LoggingHelper log) {
    if (path == null || !Files.exists(path)) {
      if (log != null) {
        log.warn("Loot table not found at %s", path);
      }
      return null;
    }
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      LootTableConfig config = gson.fromJson(reader, LootTableConfig.class);
      if (config == null) {
        if (log != null) {
          log.warn("Loot table is empty at %s", path);
        }
      }
      return config;
    } catch (Exception e) {
      if (log != null) {
        log.warn("Failed to load loot table %s: %s", path, e.getMessage());
      }
      return null;
    }
  }
}
