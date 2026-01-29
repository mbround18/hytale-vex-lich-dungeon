package MBRound18.ImmortalEngine.api.portal;

/**
 * High-level reasons a portal placement can fail.
 */
public enum PortalPlacementFailure {
  NONE,
  INVALID_INPUT,
  NO_SAFE_SPOT,
  BLOCK_NOT_FOUND,
  PLACE_FAILED
}
