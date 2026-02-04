package MBRound18.hytale.friends.data;

import java.util.UUID;
import javax.annotation.Nonnull;

public final class FriendRecord {
  private UUID uuid;
  private String name;
  private long sinceEpochMs;

  public FriendRecord() {
  }

  public FriendRecord(@Nonnull UUID uuid, @Nonnull String name, long sinceEpochMs) {
    this.uuid = uuid;
    this.name = name;
    this.sinceEpochMs = sinceEpochMs;
  }

  @Nonnull
  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(@Nonnull UUID uuid) {
    this.uuid = uuid;
  }

  @Nonnull
  public String getName() {
    return name;
  }

  public void setName(@Nonnull String name) {
    this.name = name;
  }

  public long getSinceEpochMs() {
    return sinceEpochMs;
  }

  public void setSinceEpochMs(long sinceEpochMs) {
    this.sinceEpochMs = sinceEpochMs;
  }
}
