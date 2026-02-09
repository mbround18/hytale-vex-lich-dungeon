package MBRound18.hytale.dungeonmaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class DebugServerConfig {
  public boolean enabled = true;
  public String containerName = "default";
  public String bindHost = "0.0.0.0";
  public int port = 3390;
  public int maxEvents = 2000;
  public List<String> instanceAllowlist = new ArrayList<>();
  public List<String> instanceDenylist = new ArrayList<>();
  public boolean minimalPayload = true;

  public Predicate<String> buildInstanceFilter() {
    List<String> allow = normalize(instanceAllowlist);
    List<String> deny = normalize(instanceDenylist);
    return (name) -> {
      if (name == null || name.isBlank()) {
        return true;
      }
      String key = name.toLowerCase(Locale.ROOT);
      if (deny.contains(key)) {
        return false;
      }
      if (!allow.isEmpty()) {
        return allow.contains(key);
      }
      return true;
    };
  }

  private List<String> normalize(List<String> values) {
    List<String> normalized = new ArrayList<>();
    if (values == null) {
      return normalized;
    }
    for (String value : values) {
      if (value == null || value.isBlank()) {
        continue;
      }
      normalized.add(value.toLowerCase(Locale.ROOT));
    }
    return normalized;
  }
}
