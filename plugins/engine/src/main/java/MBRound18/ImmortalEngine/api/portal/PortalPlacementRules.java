package MBRound18.ImmortalEngine.api.portal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class PortalPlacementRules {
  private final Set<String> requiredGroundIds;
  private final Set<String> clearableIds;
  private final int clearRadius;
  private final int clearAbove;

  public PortalPlacementRules(@Nonnull Set<String> requiredGroundIds, @Nonnull Set<String> clearableIds,
      int clearRadius, int clearAbove) {
    this.requiredGroundIds = normalize(requiredGroundIds);
    this.clearableIds = normalize(clearableIds);
    this.clearRadius = Math.max(0, clearRadius);
    this.clearAbove = Math.max(0, clearAbove);
  }

  @Nonnull
  public Set<String> getRequiredGroundIds() {
    return Objects.requireNonNull(requiredGroundIds, "requiredGroundIds");
  }

  @Nonnull
  public Set<String> getClearableIds() {
    return Objects.requireNonNull(clearableIds, "clearableIds");
  }

  public int getClearRadius() {
    return clearRadius;
  }

  public int getClearAbove() {
    return clearAbove;
  }

  private Set<String> normalize(Set<String> input) {
    if (input == null || input.isEmpty()) {
      return Collections.emptySet();
    }
    Set<String> normalized = new LinkedHashSet<>();
    for (String id : input) {
      if (id == null) {
        continue;
      }
      String value = id.trim().toLowerCase();
      if (!value.isEmpty()) {
        normalized.add(value);
      }
    }
    return Collections.unmodifiableSet(normalized);
  }
}
