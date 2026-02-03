package MBRound18.hytale.vexlichdungeon.engine;

import MBRound18.ImmortalEngine.runtime.DefaultScoringStrategy;
import MBRound18.ImmortalEngine.runtime.PortalEngineRuntime;
import MBRound18.ImmortalEngine.runtime.ScoringStrategy;
import MBRound18.ImmortalEngine.api.RunSummary;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Objects;

/**
 * Adapter that bridges Hytale events to the portal engine runtime.
 */
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

  public void onPlayerEnter(World world, PlayerRef playerRef) {
    if (world == null || playerRef == null || !playerRef.isValid()) {
      return;
    }
    String instanceId = world.getName();
    String playerId = playerRef.getUuid().toString();
    String displayName = resolveDisplayName(playerRef);
    runtime.onPortalEnter(instanceId, playerId, displayName);
  }

  private String resolveDisplayName(PlayerRef playerRef) {
    String username = playerRef.getUsername();
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return username == null ? "" : username;
    }
    Store<EntityStore> store = ref.getStore();
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      return username == null ? "" : username;
    }
    String displayName = player.getDisplayName();
    if (displayName == null || displayName.isBlank()) {
      return username == null ? "" : username;
    }
    return displayName;
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
