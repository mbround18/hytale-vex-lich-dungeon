package MBRound18.ImmortalEngine.api.ui;

import javax.annotation.Nullable;

public final class UiPath {
  private UiPath() {
  }

  @Nullable
  public static String normalizeForClient(@Nullable String path) {
    if (path == null) {
      return null;
    }
    String trimmed = path.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.startsWith("/")) {
      trimmed = trimmed.substring(1);
    }
    if (trimmed.startsWith("Common/UI/Custom/")) {
      return trimmed.substring("Common/UI/Custom/".length());
    }
    if (trimmed.startsWith("UI/Custom/")) {
      return trimmed.substring("UI/Custom/".length());
    }
    if (trimmed.startsWith("Custom/")) {
      return trimmed.substring("Custom/".length());
    }
    if (trimmed.startsWith("Common/UI/")) {
      return trimmed.substring("Common/UI/".length());
    }
    return trimmed;
  }
}
