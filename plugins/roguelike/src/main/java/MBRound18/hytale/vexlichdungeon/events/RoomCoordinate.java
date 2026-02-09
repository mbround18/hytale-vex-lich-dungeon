package MBRound18.hytale.vexlichdungeon.events;

public final class RoomCoordinate {
  private final int x;
  private final int z;

  public RoomCoordinate(int x, int z) {
    this.x = x;
    this.z = z;
  }

  public int getX() {
    return x;
  }

  public int getZ() {
    return z;
  }
}
