package MBRound18.ImmortalEngine.api.social;

import java.util.UUID;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Represents a friend entry stored for a player.
 */
public final class FriendEntry {
  private final UUID uuid;
  private final String name;
  private final long sinceEpochMs;

  public FriendEntry(@Nonnull UUID uuid, @Nonnull String name, long sinceEpochMs) {
    this.uuid = Objects.requireNonNull(uuid, "uuid");
    this.name = Objects.requireNonNull(name, "name");
    this.sinceEpochMs = sinceEpochMs;
  }

  @Nonnull
  public UUID getUuid() {
    return Objects.requireNonNull(uuid, "uuid");
  }

  @Nonnull
  public String getName() {
    return Objects.requireNonNull(name, "name");
  }

  public long getSinceEpochMs() {
    return sinceEpochMs;
  }
}
