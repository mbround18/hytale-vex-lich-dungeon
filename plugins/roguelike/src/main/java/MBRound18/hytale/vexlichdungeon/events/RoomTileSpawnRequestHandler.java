package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.vexlichdungeon.prefab.PrefabSpawner;
import com.hypixel.hytale.event.EventBus;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handles RoomTileSpawnRequestedEvent by delegating to PrefabSpawner.
 * This decouples the room spawning request from the actual implementation.
 */
public class RoomTileSpawnRequestHandler {
  @Nullable
  private final PrefabSpawner prefabSpawner;

  public RoomTileSpawnRequestHandler(@Nullable PrefabSpawner prefabSpawner) {
    this.prefabSpawner = prefabSpawner;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void register(@Nonnull EventBus eventBus) {
    eventBus.register(
        (Class) RoomTileSpawnRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof RoomTileSpawnRequestedEvent event) {
            onRoomTileSpawnRequested(event);
          }
        });
  }

  private void onRoomTileSpawnRequested(@Nonnull RoomTileSpawnRequestedEvent event) {
    if (prefabSpawner == null) {
      return;
    }
    // Delegate to spawner - it will emit RoomGeneratedEvent after placement
    prefabSpawner.spawnTile(
        event.getTile(),
        event.getWorld(),
        event.getWorldX(),
        event.getWorldY(),
        event.getWorldZ(),
        false);
  }
}
