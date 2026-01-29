package MBRound18.hytale.friends.services;

import MBRound18.ImmortalEngine.api.social.FriendEntry;
import MBRound18.ImmortalEngine.api.social.FriendsService;
import MBRound18.hytale.friends.data.FriendRecord;
import MBRound18.hytale.friends.data.FriendsDataStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FriendsServiceImpl implements FriendsService {
  private final FriendsDataStore dataStore;

  public FriendsServiceImpl(@Nonnull FriendsDataStore dataStore) {
    this.dataStore = dataStore;
  }

  @Override
  public boolean addFriend(@Nonnull UUID ownerId, @Nonnull UUID targetId, @Nullable String targetName) {
    if (ownerId.equals(targetId)) {
      return false;
    }
    String name = targetName != null ? targetName : targetId.toString();
    long now = System.currentTimeMillis();
    putFriend(ownerId, new FriendRecord(targetId, name, now));
    putFriend(targetId, new FriendRecord(ownerId, ownerId.toString(), now));
    dataStore.saveAll();
    return true;
  }

  @Override
  public boolean removeFriend(@Nonnull UUID ownerId, @Nonnull UUID targetId) {
    boolean removed = removeFriendInternal(ownerId, targetId);
    removed |= removeFriendInternal(targetId, ownerId);
    if (removed) {
      dataStore.saveAll();
    }
    return removed;
  }

  @Override
  public boolean areFriends(@Nonnull UUID ownerId, @Nonnull UUID targetId) {
    Map<UUID, FriendRecord> map = dataStore.getFriends().get(ownerId);
    return map != null && map.containsKey(targetId);
  }

  @Nonnull
  @Override
  public Collection<FriendEntry> getFriends(@Nonnull UUID ownerId) {
    Map<UUID, FriendRecord> map = dataStore.getFriends().get(ownerId);
    if (map == null || map.isEmpty()) {
      return java.util.List.of();
    }
    Collection<FriendEntry> entries = new ArrayList<>(map.size());
    for (FriendRecord record : map.values()) {
      entries.add(new FriendEntry(record.getUuid(), record.getName(), record.getSinceEpochMs()));
    }
    return entries;
  }

  @Nonnull
  @Override
  public Optional<FriendEntry> getFriend(@Nonnull UUID ownerId, @Nonnull UUID targetId) {
    Map<UUID, FriendRecord> map = dataStore.getFriends().get(ownerId);
    if (map == null) {
      return Optional.empty();
    }
    FriendRecord record = map.get(targetId);
    if (record == null) {
      return Optional.empty();
    }
    return Optional.of(new FriendEntry(record.getUuid(), record.getName(), record.getSinceEpochMs()));
  }

  private void putFriend(UUID ownerId, FriendRecord record) {
    Map<UUID, FriendRecord> map = dataStore.getFriends().computeIfAbsent(ownerId,
        id -> new ConcurrentHashMap<>());
    map.put(record.getUuid(), record);
  }

  private boolean removeFriendInternal(UUID ownerId, UUID targetId) {
    Map<UUID, FriendRecord> map = dataStore.getFriends().get(ownerId);
    if (map == null) {
      return false;
    }
    return map.remove(targetId) != null;
  }
}
