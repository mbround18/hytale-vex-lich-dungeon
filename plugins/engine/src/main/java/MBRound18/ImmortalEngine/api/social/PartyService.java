package MBRound18.ImmortalEngine.api.social;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Optional party service exposed via the engine.
 */
public interface PartyService {
  @Nonnull
  Optional<PartySnapshot> getParty(@Nonnull UUID memberId);

  @Nonnull
  PartyActionResult createParty(@Nonnull UUID leaderId, @Nonnull String leaderName);

  @Nonnull
  PartyActionResult invite(@Nonnull UUID inviterId, @Nonnull String inviterName,
      @Nonnull UUID targetId, @Nonnull String targetName);

  @Nonnull
  PartyActionResult acceptInvite(@Nonnull UUID targetId);

  @Nonnull
  PartyActionResult declineInvite(@Nonnull UUID targetId);

  @Nonnull
  PartyActionResult leave(@Nonnull UUID memberId);

  @Nonnull
  PartyActionResult disband(@Nonnull UUID leaderId);

  @Nonnull
  Collection<PartySnapshot> getParties();

  @Nonnull
  Optional<PartyInvite> getInvite(@Nonnull UUID targetId);

  void clearInvite(@Nonnull UUID targetId);

  void recordReturnLocation(@Nonnull UUID memberId, @Nonnull ReturnLocation location);

  @Nonnull
  Optional<ReturnLocation> getReturnLocation(@Nonnull UUID memberId);

  @Nonnull
  Optional<ReturnLocation> clearReturnLocation(@Nonnull UUID memberId);
}
