package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.vexlichdungeon.logging.PluginLog;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads prefabs directly from a ZIP file.
 * Bypasses Hytale's PrefabStore to allow custom asset packs.
 */
public class PrefabLoader {

  private final PluginLog log;
  private final ZipFile zipFile;

  public PrefabLoader(@Nonnull PluginLog log, @Nonnull ZipFile zipFile) {
    this.log = log;
    this.zipFile = zipFile;
  }

  /**
   * Loads a prefab from the ZIP file by reading and parsing its JSON.
   *
   * @param relativePathToZip Path relative to Server/Prefabs/ (e.g.,
   *                          "Rooms/Vex_Room_S_Lava_B")
   * @return The BlockSelection prefab
   * @throws IOException If the prefab cannot be read or parsed
   */
  @Nonnull
  public BlockSelection loadPrefab(@Nonnull String relativePathToZip) throws IOException {
    // Construct the full path within the ZIP
    String zipEntryPath = "Server/Prefabs/" + relativePathToZip + ".prefab.json";
    ZipEntry entry = zipFile.getEntry(zipEntryPath);

    if (entry == null) {
      throw new IOException("Prefab not found in ZIP: " + zipEntryPath);
    }

    // Read the JSON content
    String jsonContent;
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      jsonContent = sb.toString();
    }

    // Attempt to deserialize using Hytale's built-in JSON utilities
    // BlockSelection should have a static fromJson method or similar
    try {
      // This is a placeholder - actual deserialization depends on Hytale's JSON API
      // For now, we'll try to use reflection to find the right method
      return BlockSelection.class.getMethod("fromJson", String.class)
          .invoke(null, jsonContent) != null
              ? (BlockSelection) BlockSelection.class.getMethod("fromJson", String.class)
                  .invoke(null, jsonContent)
              : null;
    } catch (Exception e) {
      // Fallback: log and rethrow
      log.error("Failed to deserialize prefab JSON: %s", e.getMessage());
      throw new IOException("Could not deserialize prefab: " + zipEntryPath, e);
    }
  }
}
