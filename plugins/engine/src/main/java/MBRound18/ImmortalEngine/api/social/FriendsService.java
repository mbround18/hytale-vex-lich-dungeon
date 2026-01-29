package MBRound18.ImmortalEngine.api.social;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Optional friends service exposed via the engine.
 */
public interface FriendsService {
  boolean addFriend(@Nonnull UUID ownerId, @Nonnull UUID targetId, @Nullable String targetName);

  boolean removeFriend(@Nonnull UUID ownerId, @Nonnull UUID targetId);

  boolean areFriends(@Nonnull UUID ownerId, @Nonnull UUID targetId);

  @Nonnull
  Collection<FriendEntry> getFriends(@Nonnull UUID ownerId);

  @Nonnull
  Optional<FriendEntry> getFriend(@Nonnull UUID ownerId, @Nonnull UUID targetId);
}
