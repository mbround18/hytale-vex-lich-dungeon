package MBRound18.hytale.dungeonmaster.helpers;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class EventSerializationHelper {

  private static final long DEBUG_PAYLOAD_TIMEOUT_MS = 50L;

  private static final Gson GSON = createSafeGson();

  private EventSerializationHelper() {
  }

  public static Gson getGson() {
    return GSON;
  }

  public static JsonElement serializeSafe(Object src) {
    try {
      JsonElement custom = serializeWithEventPayload(src);
      if (custom != null) {
        return custom;
      }
      if (src != null && src.getClass().getName().equals("MBRound18.ImmortalEngine.api.events.WorldEnteredEvent")) {
        return serializeWorldEnteredEvent(src);
      }
      return GSON.toJsonTree(src);
    } catch (Throwable e) {
      try {
        return SafeJsonSerializer.serialize(src);
      } catch (Throwable ignored) {
        JsonObject err = new JsonObject();
        err.addProperty("_error", "Serialization failed");
        err.addProperty("_message", e.getMessage());
        err.addProperty("_class", src != null ? src.getClass().getName() : "null");
        return err;
      }
    }
  }

  public static String toJson(Object src) {
    return GSON.toJson(src);
  }

  private static Gson createSafeGson() {
    return new GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .registerTypeAdapter(Player.class, (JsonSerializer<Player>) (src, type, context) -> {
          JsonObject json = new JsonObject();
          putIfPresent(json, "name", invokeString(src, "getUsername", "getName"));
          putIfPresent(json, "uuid", invokeString(src, "getUuid"));
          putIfPresent(json, "entityId", invokeString(src, "getEntityId", "getId"));
          return json;
        })
        .registerTypeAdapter(World.class, (JsonSerializer<World>) (src, type, context) -> {
          JsonObject json = new JsonObject();
          putIfPresent(json, "name", invokeString(src, "getName"));
          Object dimension = invoke(src, "getDimension");
          if (dimension != null) {
            putIfPresent(json, "dimension", invokeString(dimension, "getName"));
          }
          return json;
        })
        .registerTypeHierarchyAdapter(Entity.class, (JsonSerializer<Entity>) (src, type, context) -> {
          JsonObject json = new JsonObject();
          putIfPresent(json, "uuid", invokeString(src, "getUuid"));
          putIfPresent(json, "id", invokeString(src, "getEntityId", "getId"));
          putIfPresent(json, "type", invokeString(src, "getType"));
          return json;
        })
        .registerTypeHierarchyAdapter(Throwable.class, (JsonSerializer<Throwable>) (src, type, context) -> {
          JsonObject json = new JsonObject();
          json.addProperty("type", src.getClass().getName());
          json.addProperty("message", src.getMessage());
          return json;
        })
        .setExclusionStrategies(new ExclusionStrategy() {
          @Override
          public boolean shouldSkipField(FieldAttributes f) {
            String type = f.getDeclaredType().getTypeName();
            return type.startsWith("java.util.logging")
                || type.startsWith("java.security")
                || type.startsWith("java.lang.Thread")
                || type.startsWith("java.lang.Throwable");
          }

          @Override
          public boolean shouldSkipClass(Class<?> clazz) {
            return false;
          }
        })
        .create();
  }

  private static void putIfPresent(JsonObject json, String key, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    json.addProperty(key, value);
  }

  private static Object invoke(Object target, String methodName) {
    try {
      return target.getClass().getMethod(methodName).invoke(target);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static String invokeString(Object target, String... methodNames) {
    for (String methodName : methodNames) {
      Object value = invoke(target, methodName);
      if (value != null) {
        return String.valueOf(value);
      }
    }
    return null;
  }

  private static JsonObject buildWorldMetadata(Object world) {
    JsonObject json = new JsonObject();
    putIfPresent(json, "name", invokeString(world, "getName"));
    putIfPresent(json, "id", invokeString(world, "getUuid", "getId"));
    putIfPresent(json, "playerCount", invokeString(world, "getPlayerCount"));
    Object dimension = invoke(world, "getDimension");
    if (dimension != null) {
      putIfPresent(json, "dimension", invokeString(dimension, "getName"));
    }
    return json;
  }

  private static JsonObject buildPlayerRefMetadata(Object playerRef) {
    JsonObject json = new JsonObject();
    putIfPresent(json, "uuid", invokeString(playerRef, "getUuid"));
    putIfPresent(json, "name", invokeString(playerRef, "getUsername", "getName"));
    return json;
  }

  private static JsonElement serializeWorldEnteredEvent(Object src) {
    JsonObject json = new JsonObject();
    Object world = invoke(src, "getWorld");
    if (world != null) {
      json.add("world", buildWorldMetadata(world));
    }
    Object playerRef = invoke(src, "getPlayerRef");
    if (playerRef != null) {
      json.add("player", buildPlayerRefMetadata(playerRef));
    }
    return json;
  }

  private static JsonElement serializeWithEventPayload(Object src) {
    if (src == null) {
      return null;
    }
    Object payload = invokePayloadMethod(src);
    if (payload == null) {
      return null;
    }
    Object resolved = resolvePayloadFuture(payload);
    if (resolved == null) {
      return null;
    }
    return SafeJsonSerializer.serialize(resolved);
  }

  private static Object invokePayloadMethod(Object src) {
    String[] candidates = new String[] { "toPayload", "debugPayload", "serialize", "toDebugPayload", "debugJson" };
    for (String name : candidates) {
      Method method = findPublicZeroArgMethod(src, name);
      if (method == null) {
        continue;
      }
      try {
        return method.invoke(src);
      } catch (Throwable ignored) {
        return null;
      }
    }
    return null;
  }

  private static Method findPublicZeroArgMethod(Object target, String name) {
    try {
      Method method = target.getClass().getMethod(name);
      if (method.getParameterCount() == 0) {
        return method;
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static Object resolvePayloadFuture(Object payload) {
    if (payload instanceof CompletableFuture<?> future) {
      try {
        return future.get(DEBUG_PAYLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      } catch (Throwable ignored) {
        return null;
      }
    }
    return payload;
  }
}
