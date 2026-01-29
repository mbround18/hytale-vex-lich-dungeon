package MBRound18.hytale.friends.sound;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FriendsSoundService {
  public static final String SOUND_INVITE = "Friends/SFX_Friends_Invite";
  public static final String SOUND_ACCEPT = "Friends/SFX_Friends_Accept";
  public static final String SOUND_DECLINE = "Friends/SFX_Friends_Decline";

  private FriendsSoundService() {
  }

  public static void play(@Nullable PlayerRef playerRef, @Nonnull String soundId,
      @Nonnull EngineLog log) {
    if (playerRef == null || !playerRef.isValid()) {
      return;
    }
    int index = SoundEvent.getAssetMap().getIndexOrDefault(soundId, SoundEvent.EMPTY_ID);
    if (index == SoundEvent.EMPTY_ID) {
      log.warn("[FRIENDS] Sound event not found: %s", soundId);
      return;
    }
    SoundUtil.playSoundEvent2dToPlayer(playerRef, index, SoundCategory.UI);
  }
}
