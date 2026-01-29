package MBRound18.ImmortalEngine.api.social;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Snapshot of a party for UI and shared logic.
 */
public final class PartySnapshot {
  private final UUID partyId;
  private final UUID leaderId;
  private final List<PartyMemberSnapshot> members;
  private final long createdAtEpochMs;

  public PartySnapshot(@Nonnull UUID partyId, @Nonnull UUID leaderId,
      @Nonnull List<PartyMemberSnapshot> members, long createdAtEpochMs) {
    this.partyId = partyId;
    this.leaderId = leaderId;
    this.members = List.copyOf(members);
    this.createdAtEpochMs = createdAtEpochMs;
  }

  @Nonnull
  public UUID getPartyId() {
    return partyId;
  }

  @Nonnull
  public UUID getLeaderId() {
    return leaderId;
  }

  @Nonnull
  public List<PartyMemberSnapshot> getMembers() {
    return Collections.unmodifiableList(members);
  }

  public long getCreatedAtEpochMs() {
    return createdAtEpochMs;
  }
}
