package MBRound18.ImmortalEngine.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of active runs by instance id.
 */
public class RunRegistry {

  private final Map<String, RunRecord> runs = new ConcurrentHashMap<>();

  public RunRecord getOrCreate(String instanceId) {
    return runs.computeIfAbsent(instanceId, RunRecord::new);
  }

  public Optional<RunRecord> get(String instanceId) {
    return Optional.ofNullable(runs.get(instanceId));
  }

  public void remove(String instanceId) {
    runs.remove(instanceId);
  }
}