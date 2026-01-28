package MBRound18.PortalEngine.api.prefab;

import MBRound18.PortalEngine.api.logging.EngineLog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

/**
 * Builds an index of stitch patterns to matching dungeon prefabs.
 */
public final class StitchIndexBuilder {
  private static final String STITCH_ROOT = "Server/Prefabs/Stitch/";
  private static final String DUNGEON_ROOT = "Server/Prefabs/Dungeon/";

  private StitchIndexBuilder() {
  }

  @Nullable
  public static StitchIndex loadOrBuild(Path assetsZipPath, Path indexPath, EngineLog log) {
    if (assetsZipPath == null || !Files.exists(assetsZipPath)) {
      if (log != null) {
        log.warn("Assets ZIP missing; cannot build stitch index.");
      }
      return null;
    }
    long size = assetsZipPath.toFile().length();
    long modified = assetsZipPath.toFile().lastModified();
    StitchIndex cached = StitchIndexStore.load(indexPath, size, modified, log);
    if (cached != null) {
      return cached;
    }
    StitchIndex built = build(assetsZipPath, log);
    if (built != null) {
      StitchIndexStore.save(indexPath, size, modified, built, log);
    }
    return built;
  }

  @Nullable
  public static StitchIndex build(Path assetsZipPath, EngineLog log) {
    if (assetsZipPath == null || !Files.exists(assetsZipPath)) {
      return null;
    }

    Map<String, List<String>> mapping = new LinkedHashMap<>();
    try (ZipFile zipFile = new ZipFile(assetsZipPath.toFile())) {
      List<PrefabEntry> stitches = collectPrefabs(zipFile, STITCH_ROOT, log);
      List<PrefabEntry> dungeons = collectPrefabs(zipFile, DUNGEON_ROOT, log);

      for (PrefabEntry stitch : stitches) {
        List<String> matches = new ArrayList<>();
        for (PrefabEntry dungeon : dungeons) {
          if (stitch.bounds == null || dungeon.bounds == null) {
            continue;
          }
          if (stitch.bounds.getWidth() == dungeon.bounds.getWidth()
              && stitch.bounds.getDepth() == dungeon.bounds.getDepth()) {
            matches.add(dungeon.prefabPath);
          }
        }
        mapping.put(stitch.prefabPath, matches);
      }

      if (log != null) {
        log.info("Built stitch index: %d stitch patterns, %d dungeon prefabs",
            stitches.size(), dungeons.size());
      }
      return new StitchIndex(mapping);
    } catch (Exception e) {
      if (log != null) {
        log.warn("Failed to build stitch index: %s", e.getMessage());
      }
      return null;
    }
  }

  private static List<PrefabEntry> collectPrefabs(ZipFile zipFile, String root, EngineLog log) {
    List<PrefabEntry> results = new ArrayList<>();
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      String name = entry.getName();
      if (entry.isDirectory() || !name.endsWith(".prefab.json")) {
        continue;
      }
      if (!name.startsWith(root)) {
        continue;
      }
      String prefabPath = toPrefabPath(name);
      PrefabBounds bounds = PrefabBoundsReader.read(zipFile, entry, log);
      results.add(new PrefabEntry(prefabPath, bounds));
    }
    return results;
  }

  private static String toPrefabPath(String entryName) {
    String trimmed = entryName.substring("Server/Prefabs/".length());
    if (trimmed.endsWith(".prefab.json")) {
      trimmed = trimmed.substring(0, trimmed.length() - ".prefab.json".length());
    }
    return trimmed;
  }

  private static final class PrefabEntry {
    private final String prefabPath;
    private final PrefabBounds bounds;

    private PrefabEntry(String prefabPath, @Nullable PrefabBounds bounds) {
      this.prefabPath = prefabPath;
      this.bounds = bounds;
    }
  }
}
