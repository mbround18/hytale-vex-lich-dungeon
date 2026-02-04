package MBRound18.hytale.vexlichdungeon.portal;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.vexlichdungeon.VexLichDungeonPlugin;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.data.PortalPlacementRecord;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class PortalManagerSystem extends TickingSystem<ChunkStore> {
  private static final long SWEEP_INTERVAL_MS = 1000L;
  private final LoggingHelper log = new LoggingHelper("PortalManagerSystem");
  private long lastSweepMs = 0L;

  @Override
  public void tick(float deltaSeconds, int systemIndex, @Nonnull Store<ChunkStore> store) {
    long now = System.currentTimeMillis();
    if (now - lastSweepMs < SWEEP_INTERVAL_MS) {
      return;
    }
    lastSweepMs = now;

    Object external = store.getExternalData();
    if (!(external instanceof World)) {
      return;
    }
    World world = (World) external;
    String worldName = world.getName();
    if (worldName == null || worldName.isBlank()) {
      return;
    }

    VexLichDungeonPlugin plugin = VexLichDungeonPlugin.getInstance();
    if (plugin == null) {
      return;
    }
    DataStore dataStore = plugin.getDataStore();
    if (dataStore == null) {
      return;
    }

    List<UUID> expired = new ArrayList<>();
    for (PortalPlacementRecord record : dataStore.getPortalPlacements()) {
      if (record == null) {
        continue;
      }
      String recordWorld = record.getWorldName();
      if (recordWorld == null || !recordWorld.equals(worldName)) {
        continue;
      }
      long expiresAt = record.getExpiresAt();
      if (expiresAt > 0 && now >= expiresAt) {
        UUID portalId = record.getPortalId();
        if (portalId != null) {
          expired.add(portalId);
        }
        world.execute(() -> PortalSnapshotUtil.restore(world, record));
      }
    }

    if (!expired.isEmpty()) {
      for (UUID portalId : expired) {
        dataStore.removePortalPlacement(Objects.requireNonNull(portalId, "portalId"));
      }
      log.info("Removed %d expired portal(s) in world %s", expired.size(), worldName);
    }
  }

  // Snapshot restore handled by PortalSnapshotUtil
}
