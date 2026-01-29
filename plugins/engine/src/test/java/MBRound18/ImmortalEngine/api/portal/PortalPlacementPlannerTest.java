package MBRound18.ImmortalEngine.api.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PortalPlacementPlannerTest {

  @Test
  void findsFirstValidPlacementInScanOrder() {
    PortalPlacementPlanner planner = new PortalPlacementPlanner();
    PortalPlacementConfig config = new PortalPlacementConfig(2, 2, 0, 3, 0);
    Vector3d position = new Vector3d(0, 64, 0);
    Vector3f rotation = new Vector3f(0, 0, 0);

    Optional<Vector3i> result = planner.findPlacement(position, rotation, config,
        (x, y, z) -> x == 0 && y == 67 && z == 2);

    assertTrue(result.isPresent());
    assertEquals(new Vector3i(0, 67, 2), result.get());
  }

  @Test
  void returnsEmptyWhenConfigInvalid() {
    PortalPlacementPlanner planner = new PortalPlacementPlanner();
    PortalPlacementConfig config = new PortalPlacementConfig(0, -1, -1, -1, -1);
    Vector3d position = new Vector3d(0, 64, 0);
    Vector3f rotation = new Vector3f(0, 0, 0);

    Optional<Vector3i> result = planner.findPlacement(position, rotation, config, (x, y, z) -> true);

    assertTrue(result.isEmpty());
  }
}
