package MBRound18.ImmortalEngine.api.social;

import java.util.UUID;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Represents an outstanding party invite.
 */
public final class PartyInvite {
  private final @Nonnull UUID partyId;
  private final @Nonnull UUID inviterId;
  private final @Nonnull UUID targetId;
  private final long createdAtEpochMs;

  public PartyInvite(@Nonnull UUID partyId, @Nonnull UUID inviterId, @Nonnull UUID targetId,
      long createdAtEpochMs) {
    this.partyId = Objects.requireNonNull(partyId, "partyId");
    this.inviterId = Objects.requireNonNull(inviterId, "inviterId");
    this.targetId = Objects.requireNonNull(targetId, "targetId");
    this.createdAtEpochMs = createdAtEpochMs;
  }

  @Nonnull
  public UUID getPartyId() {
    return partyId;
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
