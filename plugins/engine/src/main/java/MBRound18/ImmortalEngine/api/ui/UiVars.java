package MBRound18.ImmortalEngine.api.ui;

import javax.annotation.Nullable;

public final class UiVars {
  private UiVars() {
  }

  public static String textId(@Nullable String id) {
    if (id == null) {
      return null;
    }
    String trimmed = id.trim();
    if (trimmed.isEmpty()) {
      return trimmed;
    }
    String base = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
    return base + ".Text";
  }

  public static String textSpansId(@Nullable String id) {
    if (id == null) {
      return null;
    }
    String trimmed = id.trim();
    if (trimmed.isEmpty()) {
      return trimmed;
    }
    String base = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
    return base + ".TextSpans";
  }
}
