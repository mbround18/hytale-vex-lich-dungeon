package MBRound18.ImmortalEngine.api.events;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Base event class for debug-friendly payloads.
 * <p>
 * Override {@link #debugPayload()} to return a serializable payload for
 * SSE/telemetry.
 * The payload may be a value or a {@link CompletableFuture} resolved on the
 * game thread.
 */
public abstract class DebugEvent implements IEvent<Void> {

  /**
   * Return a serializable payload or a {@link CompletableFuture} for async
   * resolution.
   * Default is {@code null} which falls back to generic serialization.
   */
  public Object toPayload() {
    Map<String, Object> fields = debugFields();
    if (fields == null || fields.isEmpty()) {
      return null;
    }
    return fields;
  }

  /**
   * Optional field map for quick payloads. Empty or null means no custom payload.
   */
  protected Map<String, Object> debugFields() {
    return null;
  }

  protected static CompletableFuture<Object> completedPayload(Object payload) {
    return CompletableFuture.completedFuture(payload);
  }

  protected static Map<String, Object> worldMeta(World world) {
    if (world == null) {
      return null;
    }
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("name", world.getName());
    Object id = invokeAny(world, "getUuid", "getId");
    if (id != null) {
      data.put("id", id);
    }
    data.put("playerCount", world.getPlayerCount());
    Object dimension = invokeAny(world, "getDimension");
    if (dimension != null) {
      String dimensionName = invokeString(dimension, "getName");
      if (dimensionName != null) {
        data.put("dimension", dimensionName);
      }
    }
    return data;
  }

  protected static Map<String, Object> playerMeta(PlayerRef playerRef) {
    if (playerRef == null) {
      return null;
    }
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("uuid", playerRef.getUuid());
    data.put("name", playerRef.getUsername());
    return data;
  }

  protected static Map<String, Object> playerMeta(Object playerId, String playerName) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("uuid", playerId);
    data.put("name", playerName);
    return data;
  }

  protected static CompletableFuture<Object> onWorldThread(World world, Supplier<Object> supplier) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    if (world == null) {
      try {
        future.complete(supplier.get());
      } catch (Throwable e) {
        future.completeExceptionally(e);
      }
      return future;
    }
    try {
      world.execute(() -> {
        try {
          future.complete(supplier.get());
        } catch (Throwable e) {
          future.completeExceptionally(e);
        }
      });
    } catch (Throwable e) {
      try {
        future.complete(supplier.get());
      } catch (Throwable inner) {
        future.completeExceptionally(inner);
      }
    }
    return future;
  }

  private static Object invokeAny(Object target, String... methodNames) {
    for (String name : methodNames) {
      try {
        return target.getClass().getMethod(name).invoke(target);
      } catch (Throwable ignored) {
      }
    }
    return null;
  }

  private static String invokeString(Object target, String... methodNames) {
    Object value = invokeAny(target, methodNames);
    return value == null ? null : String.valueOf(value);
  }
}
