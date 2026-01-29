package MBRound18.ImmortalEngine.api.prefab;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Persists a stitch index to disk for fast startup reuse.
 */
public final class StitchIndexStore {
  private static final int MAGIC = 0x53544348; // "STCH"
  private static final int VERSION = 1;

  private StitchIndexStore() {
  }

  @Nullable
  public static StitchIndex load(Path indexPath, long assetsSize, long assetsModified, EngineLog log) {
    if (indexPath == null || !Files.exists(indexPath)) {
      return null;
    }
    try (DataInputStream input = new DataInputStream(
        new BufferedInputStream(Files.newInputStream(indexPath)))) {
      int magic = input.readInt();
      if (magic != MAGIC) {
        return null;
      }
      int version = input.readInt();
      if (version != VERSION) {
        return null;
      }
      long storedSize = input.readLong();
      long storedModified = input.readLong();
      if (storedSize != assetsSize || storedModified != assetsModified) {
        return null;
      }
      int stitchCount = input.readInt();
      Map<String, List<String>> mapping = new LinkedHashMap<>();
      for (int i = 0; i < stitchCount; i++) {
        String stitch = input.readUTF();
        int prefabCount = input.readInt();
        List<String> prefabs = new ArrayList<>(prefabCount);
        for (int j = 0; j < prefabCount; j++) {
          prefabs.add(input.readUTF());
        }
        mapping.put(stitch, prefabs);
      }
      if (log != null) {
        log.info("Loaded stitch index from %s (%d stitch patterns)", indexPath, mapping.size());
      }
      return new StitchIndex(mapping);
    } catch (Exception e) {
      if (log != null) {
        log.warn("Failed to load stitch index %s: %s", indexPath, e.getMessage());
      }
      return null;
    }
  }

  public static void save(Path indexPath, long assetsSize, long assetsModified,
      StitchIndex index, EngineLog log) {
    if (indexPath == null || index == null) {
      return;
    }
    try {
      Files.createDirectories(indexPath.getParent());
      try (DataOutputStream output = new DataOutputStream(
          new BufferedOutputStream(Files.newOutputStream(indexPath)))) {
        output.writeInt(MAGIC);
        output.writeInt(VERSION);
        output.writeLong(assetsSize);
        output.writeLong(assetsModified);
        Map<String, List<String>> mapping = index.getStitchesToPrefabs();
        output.writeInt(mapping.size());
        for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
          output.writeUTF(entry.getKey());
          List<String> prefabs = entry.getValue();
          output.writeInt(prefabs.size());
          for (String prefab : prefabs) {
            output.writeUTF(prefab);
          }
        }
      }
      if (log != null) {
        log.info("Wrote stitch index to %s (%d stitch patterns)", indexPath, index.getStitchesToPrefabs().size());
      }
    } catch (Exception e) {
      if (log != null) {
        log.warn("Failed to write stitch index %s: %s", indexPath, e.getMessage());
      }
    }
  }
}
