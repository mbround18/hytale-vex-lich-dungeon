package MBRound18.ImmortalEngine.api.portal;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import java.util.Objects;
import java.util.Optional;

/**
 * Scans for a safe portal placement near a player position.
 *
 * <p>The scan order is deterministic to make it easier to reason about where
 * portals appear: forward distance, lateral offset, then vertical positions
 * from top to bottom.</p>
 */
public final class PortalPlacementPlanner {

  @FunctionalInterface
  public interface PortalSpotValidator {
    boolean isSafe(int x, int y, int z);
  }

  /**
   * Finds the first valid placement given a validator callback.
   *
   * @param position  Player position
   * @param rotation  Player rotation (yaw in {@code y}, pitch in {@code x})
   * @param config    Scan limits
   * @param validator Callback to decide if a candidate location is safe
   * @return First valid placement or empty if none match
   */
  public Optional<Vector3i> findPlacement(Vector3d position, Vector3f rotation,
      PortalPlacementConfig config, PortalSpotValidator validator) {
    return findPlacement(position, rotation, config, validator, false);
  }

  public Optional<Vector3i> findPlacement(Vector3d position, Vector3f rotation,
      PortalPlacementConfig config, PortalSpotValidator validator, boolean scanAllDirections) {
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(rotation, "rotation");
    Objects.requireNonNull(config, "config");
    Objects.requireNonNull(validator, "validator");

    if (!config.isValid()) {
      return Optional.empty();
    }

    Optional<Vector3i> forward = scanForward(position, rotation, config, validator);
    if (forward.isPresent() || !scanAllDirections) {
      return forward;
    }
    return scanAround(position, config, validator);
  }

  private Optional<Vector3i> scanForward(Vector3d position, Vector3f rotation,
      PortalPlacementConfig config, PortalSpotValidator validator) {
    float yaw = rotation.y;
    float pitch = rotation.x;
    double yawRad = Math.toRadians(yaw);
    double pitchRad = Math.toRadians(pitch);
    double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad);
    double forwardZ = Math.cos(yawRad) * Math.cos(pitchRad);
    double rightX = Math.cos(yawRad);
    double rightZ = Math.sin(yawRad);

    int baseY = (int) Math.floor(position.y);
    for (int dist = config.getMinForward(); dist <= config.getMaxForward(); dist++) {
      for (int offset = -config.getLateralRange(); offset <= config.getLateralRange(); offset++) {
        int x = (int) Math.floor(position.x + forwardX * dist + rightX * offset);
        int z = (int) Math.floor(position.z + forwardZ * dist + rightZ * offset);
        int topY = baseY + config.getVerticalAbove();
        int bottomY = baseY - config.getVerticalBelow();
        for (int y = topY; y >= bottomY; y--) {
          if (!validator.isSafe(x, y, z)) {
            continue;
          }
          return Optional.of(new Vector3i(x, y, z));
        }
      }
    }
    return Optional.empty();
  }

  private Optional<Vector3i> scanAround(Vector3d position, PortalPlacementConfig config,
      PortalSpotValidator validator) {
    int baseX = (int) Math.floor(position.x);
    int baseZ = (int) Math.floor(position.z);
    int baseY = (int) Math.floor(position.y);
    int topY = baseY + config.getVerticalAbove();
    int bottomY = baseY - config.getVerticalBelow();
    int radius = Math.max(config.getMaxForward(), config.getLateralRange());
    for (int r = 0; r <= radius; r++) {
      for (int dx = -r; dx <= r; dx++) {
        for (int dz = -r; dz <= r; dz++) {
          if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
            continue;
          }
          int x = baseX + dx;
          int z = baseZ + dz;
          for (int y = topY; y >= bottomY; y--) {
            if (!validator.isSafe(x, y, z)) {
              continue;
            }
            return Optional.of(new Vector3i(x, y, z));
          }
        }
      }
    }
    return Optional.empty();
  }
}
