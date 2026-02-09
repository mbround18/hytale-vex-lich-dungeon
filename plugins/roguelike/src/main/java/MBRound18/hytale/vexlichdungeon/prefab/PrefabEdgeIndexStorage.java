package MBRound18.hytale.vexlichdungeon.prefab;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.dungeon.CardinalDirection;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PrefabEdgeIndexStorage {
  private static final int VERSION = 1;
  private static final String ROOMS_DB = "rooms.db";

  private PrefabEdgeIndexStorage() {
  }

  @Nullable
  public static PrefabEdgeIndex load(@Nonnull Path dataDirectory, @Nonnull LoggingHelper log) {
    Path path = dataDirectory.resolve(ROOMS_DB);
    if (!Files.exists(path)) {
      return null;
    }
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
      int version = in.readInt();
      if (version != VERSION) {
        log.warn("rooms.db version mismatch (found %d, expected %d). Ignoring cache.", version, VERSION);
        return null;
      }
      int stitchCount = in.readInt();
      PrefabEdgeIndex.Builder builder = new PrefabEdgeIndex.Builder();
      CardinalDirection[] directions = CardinalDirection.values();

      for (int i = 0; i < stitchCount; i++) {
        String stitchId = in.readUTF();
        int candidateCount = in.readInt();
        for (int j = 0; j < candidateCount; j++) {
          String prefabPath = in.readUTF();
          int rotation = in.readInt();
          int edgeOrdinal = in.readUnsignedByte();
          if (edgeOrdinal < 0 || edgeOrdinal >= directions.length) {
            continue;
          }
          CardinalDirection edge = directions[edgeOrdinal];
          builder.addMatches(prefabPath, rotation, edge, Collections.singleton(stitchId));
        }
      }
      return builder.build();
    } catch (Exception e) {
      log.warn("Failed to load rooms.db: %s", e.getMessage());
      return null;
    }
  }

  public static void save(@Nonnull Path dataDirectory, @Nonnull PrefabEdgeIndex index,
      @Nonnull LoggingHelper log) {
    try {
      Files.createDirectories(dataDirectory);
    } catch (IOException e) {
      log.warn("Failed to create data directory for rooms.db: %s", e.getMessage());
      return;
    }
    Path path = dataDirectory.resolve(ROOMS_DB);
    Map<String, List<PrefabEdgeIndex.EdgeCandidate>> mapping = index.getCandidatesByStitch();

    try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
      out.writeInt(VERSION);
      out.writeInt(mapping.size());
      for (Map.Entry<String, List<PrefabEdgeIndex.EdgeCandidate>> entry : mapping.entrySet()) {
        out.writeUTF(entry.getKey());
        List<PrefabEdgeIndex.EdgeCandidate> candidates = entry.getValue();
        out.writeInt(candidates.size());
        for (PrefabEdgeIndex.EdgeCandidate candidate : candidates) {
          out.writeUTF(candidate.prefabPath());
          out.writeInt(candidate.rotation());
          out.writeByte(candidate.edge().ordinal());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to save rooms.db: %s", e.getMessage());
    }
  }
}
