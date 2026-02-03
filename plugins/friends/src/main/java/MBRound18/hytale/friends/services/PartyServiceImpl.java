package MBRound18.hytale.friends.services;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.ImmortalEngine.api.social.PartyActionResult;
import MBRound18.ImmortalEngine.api.social.PartyInvite;
import MBRound18.ImmortalEngine.api.social.PartyMemberSnapshot;
import MBRound18.ImmortalEngine.api.social.PartyService;
import MBRound18.ImmortalEngine.api.social.PartySnapshot;
import MBRound18.ImmortalEngine.api.social.ReturnLocation;
import MBRound18.hytale.friends.data.FriendsDataStore;
import MBRound18.hytale.friends.data.PartyMemberRecord;
import MBRound18.hytale.friends.data.PartyRecord;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import javax.annotation.Nonnull;

public class PartyServiceImpl implements PartyService {
  private final FriendsDataStore dataStore;
  @SuppressWarnings("unused")
  private final LoggingHelper log;

  public PartyServiceImpl(@Nonnull FriendsDataStore dataStore, @Nonnull LoggingHelper log) {
    this.dataStore = dataStore;
    this.log = log;
  }

  @Nonnull
  @Override
  public Optional<PartySnapshot> getParty(@Nonnull UUID memberId) {
    PartyRecord party = dataStore.getPartyByMember(memberId);
    return party == null
        ? Objects.requireNonNull(Optional.empty(), "empty")
        : Objects.requireNonNull(Optional.of(toSnapshot(party)), "partySnapshot");
  }

  @Nonnull
  @Override
  public PartyActionResult createParty(@Nonnull UUID leaderId, @Nonnull String leaderName) {
    if (dataStore.getPartyByMember(leaderId) != null) {
      return Objects.requireNonNull(PartyActionResult.failure("You are already in a party."), "failure");
    }
    PartyRecord party = new PartyRecord(Objects.requireNonNull(UUID.randomUUID(), "partyId"), leaderId,
        System.currentTimeMillis(),
        Objects.requireNonNull(List.of(new PartyMemberRecord(leaderId, leaderName)), "members"));
    dataStore.indexParty(party);
    dataStore.saveAll();
    return Objects.requireNonNull(PartyActionResult.success("Party created."), "success");
  }

  @Nonnull
  @Override
  public PartyActionResult invite(@Nonnull UUID inviterId, @Nonnull String inviterName,
      @Nonnull UUID targetId, @Nonnull String targetName) {
    PartyRecord party = dataStore.getPartyByMember(inviterId);
    if (party == null) {
      PartyActionResult created = createParty(inviterId, inviterName);
      if (!created.isSuccess()) {
        return created;
      }
      party = dataStore.getPartyByMember(inviterId);
    }
    if (party == null) {
      return Objects.requireNonNull(PartyActionResult.failure("Unable to create party."), "failure");
    }
    if (!inviterId.equals(party.getLeaderId())) {
      return Objects.requireNonNull(PartyActionResult.failure("Only the party leader can invite."), "failure");
    }
    if (dataStore.getPartyByMember(targetId) != null) {
      return Objects.requireNonNull(PartyActionResult.failure("Target is already in a party."), "failure");
    }
    PartyInvite invite = new PartyInvite(party.getPartyId(), inviterId, targetId,
        System.currentTimeMillis());
    dataStore.getInvites().put(targetId, invite);
    return Objects.requireNonNull(PartyActionResult.success("Invite sent to " + targetName + "."), "success");
  }

  @Nonnull
  @Override
  public PartyActionResult acceptInvite(@Nonnull UUID targetId) {
    PartyInvite invite = dataStore.getInvites().get(targetId);
    if (invite == null) {
      return Objects.requireNonNull(PartyActionResult.failure("No party invite found."), "failure");
    }
    PartyRecord party = dataStore.getParties().get(invite.getPartyId());
    if (party == null) {
      dataStore.getInvites().remove(targetId);
      return Objects.requireNonNull(PartyActionResult.failure("Party no longer exists."), "failure");
    }
    if (dataStore.getPartyByMember(targetId) != null) {
      dataStore.getInvites().remove(targetId);
      return Objects.requireNonNull(PartyActionResult.failure("You are already in a party."), "failure");
    }
    String name = findOnlinePlayerName(targetId);
    if (name == null) {
      name = targetId.toString();
    }
    party.getMembers().add(new PartyMemberRecord(targetId, Objects.requireNonNull(name, "name")));
    dataStore.syncPartyIndex(party);
    dataStore.getInvites().remove(targetId);
    dataStore.saveAll();
    return Objects.requireNonNull(PartyActionResult.success("Joined party."), "success");
  }

  @Nonnull
  @Override
  public PartyActionResult declineInvite(@Nonnull UUID targetId) {
    if (dataStore.getInvites().remove(targetId) != null) {
      return Objects.requireNonNull(PartyActionResult.success("Invite declined."), "success");
    }
    return Objects.requireNonNull(PartyActionResult.failure("No party invite found."), "failure");
  }

  @Nonnull
  @Override
  public PartyActionResult leave(@Nonnull UUID memberId) {
    PartyRecord party = dataStore.getPartyByMember(memberId);
    if (party == null) {
      return Objects.requireNonNull(PartyActionResult.failure("You are not in a party."), "failure");
    }
    boolean removed = party.getMembers().removeIf(member -> memberId.equals(member.getUuid()));
    if (!removed) {
      return Objects.requireNonNull(PartyActionResult.failure("You are not in a party."), "failure");
    }
    if (party.getMembers().isEmpty()) {
      dataStore.removeParty(party);
      dataStore.saveAll();
      return Objects.requireNonNull(PartyActionResult.success("Party disbanded."), "success");
    }
    if (memberId.equals(party.getLeaderId())) {
      PartyMemberRecord newLeader = party.getMembers().get(0);
      party.setLeaderId(newLeader.getUuid());
    }
    dataStore.syncPartyIndex(party);
    dataStore.saveAll();
    return Objects.requireNonNull(PartyActionResult.success("Left the party."), "success");
  }

  @Nonnull
  @Override
  public PartyActionResult disband(@Nonnull UUID leaderId) {
    PartyRecord party = dataStore.getPartyByMember(leaderId);
    if (party == null) {
      return Objects.requireNonNull(PartyActionResult.failure("You are not in a party."), "failure");
    }
    if (!leaderId.equals(party.getLeaderId())) {
      return Objects.requireNonNull(PartyActionResult.failure("Only the party leader can disband."), "failure");
    }
    dataStore.removeParty(party);
    dataStore.saveAll();
    return Objects.requireNonNull(PartyActionResult.success("Party disbanded."), "success");
  }

  @Nonnull
  @Override
  public Collection<PartySnapshot> getParties() {
    Collection<PartyRecord> values = dataStore.getParties().values();
    if (values.isEmpty()) {
      return Objects.requireNonNull(List.of(), "empty");
    }
    List<PartySnapshot> snapshots = new ArrayList<>(values.size());
    for (PartyRecord record : values) {
      if (record != null) {
        snapshots.add(toSnapshot(Objects.requireNonNull(record, "record")));
      }
    }
    return snapshots;
  }

  @Nonnull
  @Override
  public Optional<PartyInvite> getInvite(@Nonnull UUID targetId) {
    return Objects.requireNonNull(Optional.ofNullable(dataStore.getInvites().get(targetId)), "invite");
  }

  @Override
  public void clearInvite(@Nonnull UUID targetId) {
    dataStore.getInvites().remove(targetId);
  }

  @Override
  public void recordReturnLocation(@Nonnull UUID memberId, @Nonnull ReturnLocation location) {
    dataStore.getReturnLocations().put(memberId, location);
  }

  @Nonnull
  @Override
  public Optional<ReturnLocation> getReturnLocation(@Nonnull UUID memberId) {
    return Objects.requireNonNull(Optional.ofNullable(dataStore.getReturnLocations().get(memberId)), "location");
  }

  @Nonnull
  @Override
  public Optional<ReturnLocation> clearReturnLocation(@Nonnull UUID memberId) {
    ReturnLocation removed = dataStore.getReturnLocations().remove(memberId);
    return Objects.requireNonNull(Optional.ofNullable(removed), "removed");
  }

  @Nonnull
  private PartySnapshot toSnapshot(@Nonnull PartyRecord party) {
    List<PartyMemberSnapshot> members = new ArrayList<>();
    for (PartyMemberRecord record : party.getMembers()) {
      members.add(new PartyMemberSnapshot(record.getUuid(), record.getName(),
          record.getUuid().equals(party.getLeaderId())));
    }
    return new PartySnapshot(party.getPartyId(), party.getLeaderId(), members,
        party.getCreatedAtEpochMs());
  }

  private String findOnlinePlayerName(@Nonnull UUID uuid) {
    PlayerRef playerRef = Universe.get().getPlayer(Objects.requireNonNull(uuid, "uuid"));
    if (playerRef == null || !playerRef.isValid()) {
      return null;
    }
    String username = playerRef.getUsername();
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return username;
    }
    Store<EntityStore> store = ref.getStore();
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      return username;
    }
    String displayName = player.getDisplayName();
    return displayName == null || displayName.isBlank() ? username : displayName;
  }
}
