package MBRound18.ImmortalEngine.api.social;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Registry for optional social services (friends/party).
 */
public final class SocialServices {
  private static volatile FriendsService friendsService;
  private static volatile PartyService partyService;

  private SocialServices() {
  }

  public static void registerFriends(@Nullable FriendsService service) {
    friendsService = service;
  }

  public static void registerParty(@Nullable PartyService service) {
    partyService = service;
  }

  public static Optional<FriendsService> getFriends() {
    return Optional.ofNullable(friendsService);
  }

  public static Optional<PartyService> getParty() {
    return Optional.ofNullable(partyService);
  }

  public static void clear() {
    friendsService = null;
    partyService = null;
  }
}
