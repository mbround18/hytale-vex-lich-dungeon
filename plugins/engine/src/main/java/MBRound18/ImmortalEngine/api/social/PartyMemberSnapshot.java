package MBRound18.ImmortalEngine.api.social;

import java.util.UUID;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Snapshot of a party member used for UI and lookups.
 */
public final class PartyMemberSnapshot {
  private final UUID uuid;
  private final String name;
  private final boolean leader;

  public PartyMemberSnapshot(@Nonnull UUID uuid, @Nonnull String name, boolean leader) {
    this.uuid = Objects.requireNonNull(uuid, "uuid");
    this.name = Objects.requireNonNull(name, "name");
    this.leader = leader;
  }

  @Nonnull
  public UUID getUuid() {
    return Objects.requireNonNull(uuid, "uuid");
  }

  @Nonnull
  public String getName() {
    return Objects.requireNonNull(name, "name");
  }

  public boolean isLeader() {
    return leader;
  }
}
