package MBRound18.hytale.shared.interfaces.ui;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.packets.interface_.HideEventTitle;
import com.hypixel.hytale.protocol.packets.interface_.ShowEventTitle;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Engine-level HUD abstraction that can route HUD requests to Custom UI or
 * fallback transports (event titles) when Custom HUD packets are unsafe.
 */
public final class EngineHud {
  public enum Mode {
    CUSTOM_UI,
    EVENT_TITLE,
    NONE
  }

  public interface CustomUiAdapter {
    void show(@Nonnull PlayerRef playerRef, @Nonnull String uiPath, @Nonnull Map<String, String> vars);

    void clear(@Nonnull PlayerRef playerRef);
  }

  private static final float EVENT_FADE_IN = 0.0f;
  private static final float EVENT_FADE_OUT = 0.0f;
  private static final float EVENT_DURATION = 6.0f;
  private static final long EVENT_MIN_UPDATE_MS = 6000L;

  private static volatile Mode mode = Mode.EVENT_TITLE;
  private static volatile CustomUiAdapter customUiAdapter;
  private static final ConcurrentHashMap<UUID, TitleState> LAST_EVENT_TITLES = new ConcurrentHashMap<>();

  private static final HudSequenceController.HudPresenter PRESENTER = new HudSequenceController.HudPresenter() {
    @Override
    public void show(@Nonnull PlayerRef playerRef, @Nonnull String uiPath, @Nonnull Map<String, String> vars) {
      EngineHud.show(playerRef, uiPath, vars);
    }

    @Override
    public void clear(@Nonnull PlayerRef playerRef) {
      EngineHud.clear(playerRef);
    }
  };

  private EngineHud() {
  }

  @Nonnull
  public static HudSequenceController.HudPresenter presenter() {
    return Objects.requireNonNull(PRESENTER, "presenter");
  }

  @Nonnull
  public static Mode getMode() {
    return Objects.requireNonNull(mode, "mode");
  }

  public static void setMode(@Nullable Mode nextMode) {
    mode = nextMode == null ? Mode.EVENT_TITLE : nextMode;
  }

  public static void setCustomUiAdapter(@Nullable CustomUiAdapter adapter) {
    customUiAdapter = adapter;
  }

  public static boolean isCustomUiMode() {
    return mode == Mode.CUSTOM_UI && customUiAdapter != null;
  }

  public static void show(@Nullable PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    if (playerRef == null || !playerRef.isValid()) {
      return;
    }
    if (mode == Mode.CUSTOM_UI && customUiAdapter != null) {
      customUiAdapter.show(playerRef, uiPath, vars);
      return;
    }
    if (mode == Mode.NONE) {
      return;
    }
    showEventTitle(playerRef, uiPath, vars);
  }

  public static void clear(@Nullable PlayerRef playerRef) {
    if (playerRef == null || !playerRef.isValid()) {
      return;
    }
    if (mode == Mode.CUSTOM_UI && customUiAdapter != null) {
      customUiAdapter.clear(playerRef);
      return;
    }
    if (mode == Mode.NONE) {
      return;
    }
    LAST_EVENT_TITLES.remove(playerRef.getUuid());
    PacketHandler handler = playerRef.getPacketHandler();
    if (handler != null) {
      handler.writeNoCache(new HideEventTitle(EVENT_FADE_OUT));
    }
  }

  private static void showEventTitle(@Nonnull PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    TitleLines lines = pickTitleLines(uiPath, vars);
    if (!shouldSendEventTitle(playerRef.getUuid(), Objects.requireNonNull(lines, "lines"))) {
      return;
    }
    ShowEventTitle packet = new ShowEventTitle();
    packet.fadeInDuration = EVENT_FADE_IN;
    packet.fadeOutDuration = EVENT_FADE_OUT;
    packet.duration = EVENT_DURATION;
    packet.isMajor = false;
    packet.primaryTitle = toMessage(Objects.requireNonNull(lines.primary(), "primary"));
    String secondary = lines.secondary();
    packet.secondaryTitle = secondary == null ? null : toMessage(secondary);
    PacketHandler handler = playerRef.getPacketHandler();
    if (handler != null) {
      handler.writeNoCache(packet);
    }
  }

  private static boolean shouldSendEventTitle(@Nonnull UUID uuid, @Nonnull TitleLines lines) {
    long now = System.currentTimeMillis();
    TitleState previous = LAST_EVENT_TITLES.get(uuid);
    if (previous != null) {
      if (now < previous.activeUntilMs) {
        return false;
      }
      if (previous.matches(lines) && now - previous.lastSentMs < EVENT_MIN_UPDATE_MS) {
        return false;
      }
      if (!previous.matches(lines) && now - previous.lastSentMs < EVENT_MIN_UPDATE_MS) {
        return false;
      }
    }
    long activeUntil = now + Math.round(EVENT_DURATION * 1000.0f);
    LAST_EVENT_TITLES.put(uuid, new TitleState(lines.primary, lines.secondary, now, activeUntil));
    return true;
  }

  private static FormattedMessage toMessage(@Nonnull String text) {
    FormattedMessage message = new FormattedMessage();
    message.rawText = text;
    message.markupEnabled = false;
    return message;
  }

  @Nonnull
  private static TitleLines pickTitleLines(@Nonnull String uiPath, @Nonnull Map<String, String> vars) {
    String primary = null;
    String secondary = null;
    if (!vars.isEmpty()) {
      List<String> keys = new ArrayList<>(vars.keySet());
      Collections.sort(keys);
      for (String key : keys) {
        String value = vars.get(key);
        if (value == null || value.isBlank()) {
          continue;
        }
        if (primary == null) {
          primary = value;
        } else if (secondary == null) {
          secondary = value;
          break;
        }
      }
    }
    if (primary == null || primary.isBlank()) {
      primary = humanizePath(uiPath);
    }
    if (secondary != null && secondary.isBlank()) {
      secondary = null;
    }
    return new TitleLines(Objects.requireNonNull(primary, "primary"), secondary);
  }

  private static String humanizePath(@Nonnull String uiPath) {
    String trimmed = uiPath.trim();
    int slash = trimmed.lastIndexOf('/');
    String base = slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    if (base.toLowerCase(Locale.ROOT).endsWith(".ui")) {
      base = base.substring(0, base.length() - 3);
    }
    return base.replace('_', ' ').replace('-', ' ').trim();
  }

  private record TitleLines(@Nonnull String primary, @Nullable String secondary) {
  }

  private static final class TitleState {
    private final String primary;
    private final String secondary;
    private final long lastSentMs;
    private final long activeUntilMs;

    private TitleState(String primary, String secondary, long lastSentMs, long activeUntilMs) {
      this.primary = primary;
      this.secondary = secondary;
      this.lastSentMs = lastSentMs;
      this.activeUntilMs = activeUntilMs;
    }

    private boolean matches(TitleLines lines) {
      return Objects.equals(primary, lines.primary) && Objects.equals(secondary, lines.secondary);
    }
  }
}
