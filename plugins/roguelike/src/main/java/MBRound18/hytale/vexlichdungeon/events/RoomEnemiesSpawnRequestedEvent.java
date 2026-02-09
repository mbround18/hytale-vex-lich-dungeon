package MBRound18.hytale.vexlichdungeon.events;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;

/**
 * Event fired when enemies need to be spawned for a specific room.
 * This is a request event - listeners should spawn enemies and emit
 * EntitySpawnedEvent.
 */
public class RoomEnemiesSpawnRequestedEvent implements IEvent<Void> {
  @Nonnull
  private final World world;

  private final int roomX;
  private final int roomZ;

  public RoomEnemiesSpawnRequestedEvent(@Nonnull World world, int roomX, int roomZ) {
    this.world = world;
    this.roomX = roomX;
    this.roomZ = roomZ;
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  public int getRoomX() {
    return roomX;
  }

  public int getRoomZ() {
    return roomZ;
  }
}
