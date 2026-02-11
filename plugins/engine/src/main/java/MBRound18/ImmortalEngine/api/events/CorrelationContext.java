package MBRound18.ImmortalEngine.api.events;

import java.util.UUID;
import java.util.function.Supplier;

public final class CorrelationContext {
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private CorrelationContext() {
  }

  public static String get() {
    return CURRENT.get();
  }

  public static String getOrCreate() {
    String existing = CURRENT.get();
    if (existing != null && !existing.isBlank()) {
      return existing;
    }
    String created = UUID.randomUUID().toString();
    CURRENT.set(created);
    return created;
  }

  public static void set(String id) {
    if (id == null || id.isBlank()) {
      clear();
      return;
    }
    CURRENT.set(id);
  }

  public static void clear() {
    CURRENT.remove();
  }

  public static <T> T runWithId(String id, Supplier<T> work) {
    String previous = CURRENT.get();
    set(id);
    try {
      return work.get();
    } finally {
      restore(previous);
    }
  }

  public static void runWithId(String id, Runnable work) {
    String previous = CURRENT.get();
    set(id);
    try {
      work.run();
    } finally {
      restore(previous);
    }
  }

  public static <T> T runWithNewId(Supplier<T> work) {
    return runWithId(UUID.randomUUID().toString(), work);
  }

  private static void restore(String previous) {
    if (previous == null || previous.isBlank()) {
      clear();
      return;
    }
    CURRENT.set(previous);
  }
}
