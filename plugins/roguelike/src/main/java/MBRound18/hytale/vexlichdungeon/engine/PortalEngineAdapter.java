package MBRound18.hytale.vexlichdungeon.engine;

import MBRound18.ImmortalEngine.runtime.DefaultScoringStrategy;
import MBRound18.ImmortalEngine.runtime.PortalEngineRuntime;
import MBRound18.ImmortalEngine.runtime.ScoringStrategy;
import MBRound18.ImmortalEngine.api.RunSummary;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Objects;

/**
 * Adapter that bridges Hytale events to the portal engine runtime.
 */
@SuppressWarnings("removal")
public class PortalEngineAdapter {

  private final PortalEngineRuntime runtime;

  public PortalEngineAdapter() {
    this(new DefaultScoringStrategy());
  }

  public PortalEngineAdapter(ScoringStrategy scoringStrategy) {
    this.runtime = new PortalEngineRuntime(Objects.requireNonNull(scoringStrategy, "scoringStrategy"));
  }

  public PortalEngineRuntime getRuntime() {
    return runtime;
  }

  public void onPlayerEnter(World world, Player player) {
    if (world == null || player == null) {
      return;
    }
    String instanceId = world.getName();
    String playerId = player.getUuid().toString();
    String displayName = player.getDisplayName();
    runtime.onPortalEnter(instanceId, playerId, displayName);
  }

  public void onKill(String instanceId, String playerId, String enemyType, int points) {
    runtime.onKill(instanceId, playerId, enemyType, points);
  }

  public void onRoomCleared(String instanceId) {
    runtime.onRoomCleared(instanceId);
  }

  public void onRoundCleared(String instanceId) {
    runtime.onRoundCleared(instanceId);
  }

  public void onSafeRoomVisited(String instanceId) {
    runtime.onSafeRoomVisited(instanceId);
  }

  public RunSummary finalizeRun(String instanceId) {
    return runtime.finalizeRun(instanceId);
  }
}