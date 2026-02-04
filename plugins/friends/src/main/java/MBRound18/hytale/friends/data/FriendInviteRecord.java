package MBRound18.hytale.friends.data;

import java.util.UUID;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class FriendInviteRecord {
  private final @Nonnull UUID inviterId;
  private final @Nonnull UUID targetId;
  private final long createdAtEpochMs;

  public FriendInviteRecord(@Nonnull UUID inviterId, @Nonnull UUID targetId, long createdAtEpochMs) {
    this.inviterId = Objects.requireNonNull(inviterId, "inviterId");
    this.targetId = Objects.requireNonNull(targetId, "targetId");
    this.createdAtEpochMs = createdAtEpochMs;
  }

  @Nonnull
  public UUID getInviterId() {
    return inviterId;
  }

  @Nonnull
  public UUID getTargetId() {
    return targetId;
  }

  public long getCreatedAtEpochMs() {
    return createdAtEpochMs;
  }
}
