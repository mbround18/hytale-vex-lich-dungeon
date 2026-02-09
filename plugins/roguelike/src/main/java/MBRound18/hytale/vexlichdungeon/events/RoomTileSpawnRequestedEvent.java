package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.vexlichdungeon.dungeon.DungeonTile;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;

/**
 * Event fired when a room tile needs to be spawned into the world.
 * This is a request event - listeners should spawn the tile and emit
 * RoomGeneratedEvent.
 */
public class RoomTileSpawnRequestedEvent implements IEvent<Void> {
  @Nonnull
  private final World world;

  @Nonnull
  private final DungeonTile tile;

  private final int worldX;
  private final int worldY;
  private final int worldZ;

  public RoomTileSpawnRequestedEvent(@Nonnull World world, @Nonnull DungeonTile tile, int worldX, int worldY,
      int worldZ) {
    this.world = world;
    this.tile = tile;
    this.worldX = worldX;
    this.worldY = worldY;
    this.worldZ = worldZ;
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public DungeonTile getTile() {
    return tile;
  }

  public int getWorldX() {
    return worldX;
  }

  public int getWorldY() {
    return worldY;
  }

  public int getWorldZ() {
    return worldZ;
  }
}
