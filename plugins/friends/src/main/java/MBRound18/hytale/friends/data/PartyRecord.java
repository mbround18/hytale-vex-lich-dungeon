package MBRound18.hytale.friends.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class PartyRecord {
  private UUID partyId;
  private UUID leaderId;
  private long createdAtEpochMs;
  private List<PartyMemberRecord> members = new ArrayList<>();

  public PartyRecord() {
  }

  public PartyRecord(@Nonnull UUID partyId, @Nonnull UUID leaderId, long createdAtEpochMs,
      @Nonnull List<PartyMemberRecord> members) {
    this.partyId = Objects.requireNonNull(partyId, "partyId");
    this.leaderId = Objects.requireNonNull(leaderId, "leaderId");
    this.createdAtEpochMs = createdAtEpochMs;
    this.members = new ArrayList<>(Objects.requireNonNull(members, "members"));
  }

  @Nonnull
  public UUID getPartyId() {
    return Objects.requireNonNull(partyId, "partyId");
  }

  public void setPartyId(@Nonnull UUID partyId) {
    this.partyId = Objects.requireNonNull(partyId, "partyId");
  }

  @Nonnull
  public UUID getLeaderId() {
    return Objects.requireNonNull(leaderId, "leaderId");
  }

  public void setLeaderId(@Nonnull UUID leaderId) {
    this.leaderId = Objects.requireNonNull(leaderId, "leaderId");
  }

  public long getCreatedAtEpochMs() {
    return createdAtEpochMs;
  }

  public void setCreatedAtEpochMs(long createdAtEpochMs) {
    this.createdAtEpochMs = createdAtEpochMs;
  }

  @Nonnull
  public List<PartyMemberRecord> getMembers() {
    return Objects.requireNonNull(members, "members");
  }

  public void setMembers(@Nonnull List<PartyMemberRecord> members) {
    this.members = new ArrayList<>(Objects.requireNonNull(members, "members"));
  }
}
