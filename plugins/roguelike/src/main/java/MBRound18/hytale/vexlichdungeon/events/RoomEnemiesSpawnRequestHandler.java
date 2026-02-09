package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import com.hypixel.hytale.event.EventBus;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handles RoomEnemiesSpawnRequestedEvent by delegating to
 * RoguelikeDungeonController.
 * This decouples the enemy spawning request from the actual implementation.
 */
public class RoomEnemiesSpawnRequestHandler {
  @Nullable
  private final RoguelikeDungeonController controller;

  public RoomEnemiesSpawnRequestHandler(@Nullable RoguelikeDungeonController controller) {
    this.controller = controller;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void register(@Nonnull EventBus eventBus) {
    eventBus.register(
        (Class) RoomEnemiesSpawnRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof RoomEnemiesSpawnRequestedEvent event) {
            onRoomEnemiesSpawnRequested(event);
          }
        });
  }

  private void onRoomEnemiesSpawnRequested(@Nonnull RoomEnemiesSpawnRequestedEvent event) {
    if (controller == null) {
      return;
    }
    // Delegate to controller - it will emit EntitySpawnedEvent for each spawned
    // enemy
    controller.spawnEnemiesForRoomRequest(event.getWorld(), event.getRoomX(), event.getRoomZ());
  }
}
