package MBRound18.ImmortalEngine.api;

import java.util.Objects;

/**
 * Immutable action emitted by the engine for the adapter to execute.
 */
public final class EngineAction {

  private final EngineActionType type;
  private final String payload;

  public EngineAction(EngineActionType type, String payload) {
    this.type = Objects.requireNonNull(type, "type");
    this.payload = payload;
  }

  public EngineActionType getType() {
    return type;
  }

  public String getPayload() {
    return payload;
  }
}