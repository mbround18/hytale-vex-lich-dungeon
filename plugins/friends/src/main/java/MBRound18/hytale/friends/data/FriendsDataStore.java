package MBRound18.hytale.friends.data;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.ImmortalEngine.api.social.PartyInvite;
import MBRound18.ImmortalEngine.api.social.ReturnLocation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FriendsDataStore {
  private final LoggingHelper log;
  private final Path dataDirectory;
  private final Gson gson;

  private final Map<UUID, Map<UUID, FriendRecord>> friends = new ConcurrentHashMap<>();
  private final Map<UUID, PartyRecord> parties = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> memberToParty = new ConcurrentHashMap<>();
  private final Map<UUID, PartyInvite> invites = new ConcurrentHashMap<>();
  private final Map<UUID, ReturnLocation> returnLocations = new ConcurrentHashMap<>();
  private final Map<UUID, FriendInviteRecord> friendInvites = new ConcurrentHashMap<>();

  public FriendsDataStore(@Nonnull LoggingHelper log, @Nonnull Path dataDirectory) {
    this.log = log;
    this.dataDirectory = dataDirectory;
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  @Nonnull
  public LoggingHelper getLog() {
    return log;
  }

  public void initialize() {
    try {
      if (!Files.exists(dataDirectory)) {
        Files.createDirectories(dataDirectory);
      }
      loadFriends();
      loadParties();
    } catch (IOException e) {
      log.warn("[FRIENDS] Failed to initialize data store: %s", e.getMessage());
    }
  }

  public void saveAll() {
    saveFriends();
    saveParties();
  }

  @Nonnull
  public Map<UUID, Map<UUID, FriendRecord>> getFriends() {
    return friends;
  }

  @Nonnull
  public Map<UUID, PartyRecord> getParties() {
    return parties;
  }

  @Nonnull
  public Map<UUID, UUID> getMemberToParty() {
    return memberToParty;
  }

  @Nonnull
  public Map<UUID, PartyInvite> getInvites() {
    return invites;
  }

  @Nonnull
  public Map<UUID, ReturnLocation> getReturnLocations() {
    return returnLocations;
  }

  @Nonnull
  public Map<UUID, FriendInviteRecord> getFriendInvites() {
    return friendInvites;
  }

  @Nullable
  public PartyRecord getPartyByMember(@Nonnull UUID memberId) {
    UUID partyId = memberToParty.get(memberId);
    return partyId != null ? parties.get(partyId) : null;
  }

  public void indexParty(@Nonnull PartyRecord party) {
    parties.put(party.getPartyId(), party);
    for (PartyMemberRecord member : party.getMembers()) {
      memberToParty.put(member.getUuid(), party.getPartyId());
    }
  }

  public void removeParty(@Nonnull PartyRecord party) {
    parties.remove(party.getPartyId());
    for (PartyMemberRecord member : party.getMembers()) {
      memberToParty.remove(member.getUuid());
    }
  }

  public void syncPartyIndex(@Nonnull PartyRecord party) {
    for (PartyMemberRecord member : party.getMembers()) {
      memberToParty.put(member.getUuid(), party.getPartyId());
    }
  }

  private void loadFriends() throws IOException {
    Path path = dataDirectory.resolve("friends.json");
    if (!Files.exists(path)) {
      return;
    }
    Type type = new TypeToken<Map<String, List<FriendRecord>>>() {
    }.getType();
    try (Reader reader = Files.newBufferedReader(path)) {
      Map<String, List<FriendRecord>> raw = gson.fromJson(reader, type);
      if (raw == null) {
        return;
      }
      for (Map.Entry<String, List<FriendRecord>> entry : raw.entrySet()) {
        UUID ownerId = parseUuid(entry.getKey());
        if (ownerId == null) {
          continue;
        }
        Map<UUID, FriendRecord> map = new ConcurrentHashMap<>();
        for (FriendRecord record : entry.getValue()) {
          if (record == null || record.getUuid() == null) {
            continue;
          }
          map.put(record.getUuid(), record);
        }
        friends.put(ownerId, map);
      }
    }
    log.info("[FRIENDS] Loaded friends: %d players", friends.size());
  }

  private void saveFriends() {
    Path path = dataDirectory.resolve("friends.json");
    Map<String, List<FriendRecord>> raw = new ConcurrentHashMap<>();
    for (Map.Entry<UUID, Map<UUID, FriendRecord>> entry : friends.entrySet()) {
      raw.put(entry.getKey().toString(), new ArrayList<>(entry.getValue().values()));
    }
    try (Writer writer = Files.newBufferedWriter(path)) {
      gson.toJson(raw, writer);
    } catch (IOException e) {
      log.warn("[FRIENDS] Failed to save friends: %s", e.getMessage());
    }
  }

  private void loadParties() throws IOException {
    Path path = dataDirectory.resolve("parties.json");
    if (!Files.exists(path)) {
      return;
    }
    Type type = new TypeToken<List<PartyRecord>>() {
    }.getType();
    try (Reader reader = Files.newBufferedReader(path)) {
      List<PartyRecord> loaded = gson.fromJson(reader, type);
      if (loaded == null) {
        return;
      }
      for (PartyRecord record : loaded) {
        if (record == null || record.getPartyId() == null) {
          continue;
        }
        indexParty(record);
      }
    }
    log.info("[FRIENDS] Loaded parties: %d", parties.size());
  }

  private void saveParties() {
    Path path = dataDirectory.resolve("parties.json");
    Collection<PartyRecord> values = parties.values();
    List<PartyRecord> list = values.isEmpty() ? Collections.emptyList() : new ArrayList<>(values);
    try (Writer writer = Files.newBufferedWriter(path)) {
      gson.toJson(list, writer);
    } catch (IOException e) {
      log.warn("[FRIENDS] Failed to save parties: %s", e.getMessage());
    }
  }

  @Nullable
  private UUID parseUuid(@Nullable String raw) {
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
