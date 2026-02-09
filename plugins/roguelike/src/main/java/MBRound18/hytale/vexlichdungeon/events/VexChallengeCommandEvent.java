package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.DebugEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VexChallengeCommandEvent extends DebugEvent {
  @Nonnull
  private final PlayerRef playerRef;
  @Nullable
  private final UUID worldId;
  @Nullable
  private final String worldName;
  private final int countdownSeconds;
  @Nonnull
  private final String prefabPath;

  public VexChallengeCommandEvent(@Nonnull PlayerRef playerRef, @Nullable UUID worldId,
      @Nullable String worldName, int countdownSeconds, @Nonnull String prefabPath) {
    this.playerRef = Objects.requireNonNull(playerRef, "playerRef");
    this.worldId = worldId;
    this.worldName = worldName;
    this.countdownSeconds = countdownSeconds;
    this.prefabPath = Objects.requireNonNull(prefabPath, "prefabPath");
  }

  @Nonnull
  public PlayerRef getPlayerRef() {
    return playerRef;
  }

  @Nullable
  public UUID getWorldId() {
    return worldId;
  }

  @Nullable
  public String getWorldName() {
    return worldName;
  }

  public int getCountdownSeconds() {
    return countdownSeconds;
  }

  @Nonnull
  public String getPrefabPath() {
    return prefabPath;
  }

  @Override
  public Object toPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("player", playerMeta(playerRef));
    data.put("worldId", worldId);
    data.put("worldName", worldName);
    data.put("countdownSeconds", countdownSeconds);
    data.put("prefabPath", prefabPath);
    return data;
  }
}
