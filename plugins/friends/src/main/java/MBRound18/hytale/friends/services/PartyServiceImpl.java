package MBRound18.hytale.friends.services;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public class PartyServiceImpl implements PartyService {
  private final FriendsDataStore dataStore;
  private final EngineLog log;

  public PartyServiceImpl(@Nonnull FriendsDataStore dataStore, @Nonnull EngineLog log) {
    this.dataStore = dataStore;
    this.log = log;
  }

  @Nonnull
  @Override
  public Optional<PartySnapshot> getParty(@Nonnull UUID memberId) {
    PartyRecord party = dataStore.getPartyByMember(memberId);
    return party == null ? Optional.empty() : Optional.of(toSnapshot(party));
  }

  @Nonnull
  @Override
  public PartyActionResult createParty(@Nonnull UUID leaderId, @Nonnull String leaderName) {
    if (dataStore.getPartyByMember(leaderId) != null) {
      return PartyActionResult.failure("You are already in a party.");
    }
    PartyRecord party = new PartyRecord(UUID.randomUUID(), leaderId, System.currentTimeMillis(),
        List.of(new PartyMemberRecord(leaderId, leaderName)));
    dataStore.indexParty(party);
    dataStore.saveAll();
    return PartyActionResult.success("Party created.");
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
      return PartyActionResult.failure("Unable to create party.");
    }
    if (!inviterId.equals(party.getLeaderId())) {
      return PartyActionResult.failure("Only the party leader can invite.");
    }
    if (dataStore.getPartyByMember(targetId) != null) {
      return PartyActionResult.failure("Target is already in a party.");
    }
    PartyInvite invite = new PartyInvite(party.getPartyId(), inviterId, targetId,
        System.currentTimeMillis());
    dataStore.getInvites().put(targetId, invite);
    return PartyActionResult.success("Invite sent to " + targetName + ".");
  }

  @Nonnull
  @Override
  public PartyActionResult acceptInvite(@Nonnull UUID targetId) {
    PartyInvite invite = dataStore.getInvites().get(targetId);
    if (invite == null) {
      return PartyActionResult.failure("No party invite found.");
    }
    PartyRecord party = dataStore.getParties().get(invite.getPartyId());
    if (party == null) {
      dataStore.getInvites().remove(targetId);
      return PartyActionResult.failure("Party no longer exists.");
    }
    if (dataStore.getPartyByMember(targetId) != null) {
      dataStore.getInvites().remove(targetId);
      return PartyActionResult.failure("You are already in a party.");
    }
    String name = findOnlinePlayerName(targetId);
    if (name == null) {
      name = targetId.toString();
    }
    party.getMembers().add(new PartyMemberRecord(targetId, name));
    dataStore.syncPartyIndex(party);
    dataStore.getInvites().remove(targetId);
    dataStore.saveAll();
    return PartyActionResult.success("Joined party.");
  }

  @Nonnull
  @Override
  public PartyActionResult declineInvite(@Nonnull UUID targetId) {
    if (dataStore.getInvites().remove(targetId) != null) {
      return PartyActionResult.success("Invite declined.");
    }
    return PartyActionResult.failure("No party invite found.");
  }

  @Nonnull
  @Override
  public PartyActionResult leave(@Nonnull UUID memberId) {
    PartyRecord party = dataStore.getPartyByMember(memberId);
    if (party == null) {
      return PartyActionResult.failure("You are not in a party.");
    }
    boolean removed = party.getMembers().removeIf(member -> memberId.equals(member.getUuid()));
    if (!removed) {
      return PartyActionResult.failure("You are not in a party.");
    }
    if (party.getMembers().isEmpty()) {
      dataStore.removeParty(party);
      dataStore.saveAll();
      return PartyActionResult.success("Party disbanded.");
    }
    if (memberId.equals(party.getLeaderId())) {
      PartyMemberRecord newLeader = party.getMembers().get(0);
      party.setLeaderId(newLeader.getUuid());
    }
    dataStore.syncPartyIndex(party);
    dataStore.saveAll();
    return PartyActionResult.success("Left the party.");
  }

  @Nonnull
  @Override
  public PartyActionResult disband(@Nonnull UUID leaderId) {
    PartyRecord party = dataStore.getPartyByMember(leaderId);
    if (party == null) {
      return PartyActionResult.failure("You are not in a party.");
    }
    if (!leaderId.equals(party.getLeaderId())) {
      return PartyActionResult.failure("Only the party leader can disband.");
    }
    dataStore.removeParty(party);
    dataStore.saveAll();
    return PartyActionResult.success("Party disbanded.");
  }

  @Nonnull
  @Override
  public Collection<PartySnapshot> getParties() {
    Collection<PartyRecord> values = dataStore.getParties().values();
    if (values.isEmpty()) {
      return List.of();
    }
    List<PartySnapshot> snapshots = new ArrayList<>(values.size());
    for (PartyRecord record : values) {
      snapshots.add(toSnapshot(record));
    }
    return snapshots;
  }

  @Nonnull
  @Override
  public Optional<PartyInvite> getInvite(@Nonnull UUID targetId) {
    return Optional.ofNullable(dataStore.getInvites().get(targetId));
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
    return Optional.ofNullable(dataStore.getReturnLocations().get(memberId));
  }

  @Nonnull
  @Override
  public Optional<ReturnLocation> clearReturnLocation(@Nonnull UUID memberId) {
    ReturnLocation removed = dataStore.getReturnLocations().remove(memberId);
    return Optional.ofNullable(removed);
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

  private String findOnlinePlayerName(UUID uuid) {
    PlayerRef playerRef = Universe.get().getPlayer(uuid);
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
