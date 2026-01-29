package MBRound18.ImmortalEngine.api.social;

import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Represents an outstanding party invite.
 */
public final class PartyInvite {
  private final UUID partyId;
  private final UUID inviterId;
  private final UUID targetId;
  private final long createdAtEpochMs;

  public PartyInvite(@Nonnull UUID partyId, @Nonnull UUID inviterId, @Nonnull UUID targetId,
      long createdAtEpochMs) {
    this.partyId = partyId;
    this.inviterId = inviterId;
    this.targetId = targetId;
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
