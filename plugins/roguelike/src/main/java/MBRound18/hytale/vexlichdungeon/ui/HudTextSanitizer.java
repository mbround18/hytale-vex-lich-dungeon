package MBRound18.hytale.vexlichdungeon.ui;

import java.util.regex.Pattern;
import javax.annotation.Nullable;

public final class HudTextSanitizer {
  public static final String EMPTY_VALUE = "---";
  private static final Pattern ZERO_PATTERN = Pattern.compile("^-?0+(?:\\.0+)?$");

  private HudTextSanitizer() {
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

  public static String formatLabeledValue(String label, int value) {
    String body = value == 0 ? EMPTY_VALUE : Integer.toString(value);
    return sanitize(label + " " + body);
  }

  public static String formatDelta(int delta) {
    if (delta == 0) {
      return EMPTY_VALUE;
    }
    return sanitize((delta > 0 ? "+" : "") + delta);
  }
}
