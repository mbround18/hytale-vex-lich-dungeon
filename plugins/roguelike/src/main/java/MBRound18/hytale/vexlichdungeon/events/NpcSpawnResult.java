package MBRound18.hytale.vexlichdungeon.events;

import java.util.UUID;
import javax.annotation.Nullable;

public final class NpcSpawnResult {
  private final boolean success;
  @Nullable
  private final UUID entityId;
  @Nullable
  private final String error;

  public NpcSpawnResult(boolean success, @Nullable UUID entityId, @Nullable String error) {
    this.success = success;
    this.entityId = entityId;
    this.error = error;
  }

  public boolean isSuccess() {
    return success;
  }

  @Nullable
  public UUID getEntityId() {
    return entityId;
  }

  @Nullable
  public String getError() {
    return error;
  }

  public static NpcSpawnResult success(@Nullable UUID entityId) {
    return new NpcSpawnResult(true, entityId, null);
  }

  public static NpcSpawnResult failure(@Nullable String error) {
    return new NpcSpawnResult(false, null, error);
  }
}
