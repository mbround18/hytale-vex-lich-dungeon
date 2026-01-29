package MBRound18.ImmortalEngine.api.prefab;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

/**
 * Reads block bounds from prefab JSON stored inside an assets ZIP.
 */
public final class PrefabBoundsReader {
  private PrefabBoundsReader() {
  }

  @Nullable
  public static PrefabBounds read(ZipFile zipFile, ZipEntry entry, EngineLog log) {
    if (zipFile == null || entry == null) {
      return null;
    }
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
      JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
      JsonArray blocks = json.getAsJsonArray("blocks");
      if (blocks == null || blocks.isEmpty()) {
        return null;
      }

      int minX = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxY = Integer.MIN_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxZ = Integer.MIN_VALUE;

      for (JsonElement elem : blocks) {
        JsonObject block = elem.getAsJsonObject();
        int x = block.get("x").getAsInt();
        int y = block.get("y").getAsInt();
        int z = block.get("z").getAsInt();
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);
      }

      if (minX == Integer.MAX_VALUE || minY == Integer.MAX_VALUE || minZ == Integer.MAX_VALUE) {
        return null;
      }

      return new PrefabBounds(minX, maxX, minY, maxY, minZ, maxZ);
    } catch (Exception e) {
      if (log != null) {
        log.warn("Failed to read prefab bounds from %s: %s", entry.getName(), e.getMessage());
      }
      return null;
    }
  }
}
