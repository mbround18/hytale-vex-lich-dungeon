package MBRound18.ImmortalEngine.api.social;

import javax.annotation.Nullable;

/**
 * Simple result wrapper for party actions.
 */
public final class PartyActionResult {
  private final boolean success;
  private final String message;

  private PartyActionResult(boolean success, @Nullable String message) {
    this.success = success;
    this.message = message;
  }

  public static PartyActionResult success(@Nullable String message) {
    return new PartyActionResult(true, message);
  }

  public static PartyActionResult failure(@Nullable String message) {
    return new PartyActionResult(false, message);
  }

  public boolean isSuccess() {
    return success;
  }

  @Nullable
  public String getMessage() {
    return message;
  }
}
