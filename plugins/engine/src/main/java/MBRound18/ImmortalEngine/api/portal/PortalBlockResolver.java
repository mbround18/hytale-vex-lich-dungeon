package MBRound18.ImmortalEngine.api.portal;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Resolves portal block types by candidate ids with graceful fallback matching.
 *
 * <p>This resolver first attempts direct lookups, then falls back to
 * case-insensitive and path-suffix matching to accommodate asset ids that
 * include folders (e.g., {@code Items/Portal/...}).</p>
 */
public final class PortalBlockResolver {
  private final List<String> candidates;

  public PortalBlockResolver(List<String> candidates) {
    Objects.requireNonNull(candidates, "candidates");
    this.candidates = List.copyOf(candidates);
  }

  public List<String> getCandidates() {
    return candidates;
  }

  @Nullable
  public BlockType resolve(@Nullable EngineLog log) {
    if (candidates.isEmpty()) {
      if (log != null) {
        log.warn("[PORTAL] No portal block candidates provided.");
      }
      return null;
    }

    for (String candidate : candidates) {
      BlockType block = resolveDirect(candidate);
      if (block != null) {
        return block;
      }
    }

    List<String> keys = new ArrayList<>(BlockType.getAssetMap().getAssetMap().keySet());
    for (String candidate : candidates) {
      String lowered = candidate.toLowerCase(Locale.ROOT);
      for (String key : keys) {
        if (key.equalsIgnoreCase(candidate)) {
          return BlockType.getAssetMap().getAsset(key);
        }
        String keyLower = key.toLowerCase(Locale.ROOT);
        if (keyLower.endsWith("/" + lowered) || keyLower.endsWith("\\" + lowered)) {
          return BlockType.getAssetMap().getAsset(key);
        }
      }
    }

    if (log != null) {
      int logged = 0;
      for (String key : keys) {
        String lowered = key.toLowerCase(Locale.ROOT);
        if (lowered.contains("portal") && logged < 5) {
          log.info("[PORTAL] Portal-related block key candidate: %s", key);
          logged++;
        }
      }
      log.warn("[PORTAL] Portal block type not found in asset map (tried %s)",
          String.join(", ", candidates));
    }
    return null;
  }

  @Nullable
  private BlockType resolveDirect(String candidate) {
    if (candidate == null || candidate.isBlank()) {
      return null;
    }
    BlockType block = BlockType.getAssetMap().getAsset(candidate);
    if (block == null || block.isUnknown()) {
      return null;
    }
    return block;
  }
}
