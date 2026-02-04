package MBRound18.hytale.friends.data;

import java.util.UUID;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class PartyMemberRecord {
  private UUID uuid;
  private String name;

  public PartyMemberRecord() {
  }

  public PartyMemberRecord(@Nonnull UUID uuid, @Nonnull String name) {
    this.uuid = Objects.requireNonNull(uuid, "uuid");
    this.name = Objects.requireNonNull(name, "name");
  }

  @Nonnull
  public UUID getUuid() {
    return Objects.requireNonNull(uuid, "uuid");
  }

  public void setUuid(@Nonnull UUID uuid) {
    this.uuid = Objects.requireNonNull(uuid, "uuid");
  }

  @Nonnull
  public String getName() {
    return Objects.requireNonNull(name, "name");
  }

  public void setName(@Nonnull String name) {
    this.name = Objects.requireNonNull(name, "name");
  }
}
