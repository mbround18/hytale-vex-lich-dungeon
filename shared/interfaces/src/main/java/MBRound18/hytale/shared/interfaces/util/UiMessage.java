package MBRound18.hytale.shared.interfaces.util;

import com.hypixel.hytale.server.core.Message;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class UiMessage {
  public static final String EMPTY_VALUE = "---";
  private static final Pattern ZERO_PATTERN = Pattern.compile("^-?0+(?:\\.0+)?$");

  private UiMessage() {
  }

  public static Message raw(@Nullable String value) {
    return Message.raw(sanitize(value));
  }

  public static Message raw(@Nullable Object value) {
    return Message.raw(sanitize(value == null ? null : String.valueOf(value)));
  }

  public static Message format(@Nullable String format, @Nullable Object... args) {
    String rendered = format == null ? null : String.format(format, args == null ? new Object[0] : args);
    return Message.raw(sanitize(rendered));
  }

  public static Message format(@Nonnull Locale locale, @Nullable String format, @Nullable Object... args) {
    String rendered = format == null ? null : String.format(locale, format, args == null ? new Object[0] : args);
    return Message.raw(sanitize(rendered));
  }

  public static String sanitize(@Nullable String raw) {
    if (raw == null) {
      return EMPTY_VALUE;
    }
    String cleaned = raw.replace("/", " ");
    String trimmed = cleaned.trim();
    if (trimmed.isEmpty()) {
      return EMPTY_VALUE;
    }
    if (ZERO_PATTERN.matcher(trimmed).matches()) {
      return EMPTY_VALUE;
    }
    return cleaned;
  }
}
