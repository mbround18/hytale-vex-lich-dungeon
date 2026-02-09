package MBRound18.hytale.dungeonmaster.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.IdentityHashMap;
import java.util.Map;

public final class SafeJsonSerializer {
  private static final int MAX_DEPTH = 4;

  private SafeJsonSerializer() {
  }

  public static JsonElement serialize(Object value) {
    try {
      return serialize(value, 0, new IdentityHashMap<>());
    } catch (Throwable ignored) {
      return new JsonPrimitive(String.valueOf(value));
    }
  }

  private static JsonElement serialize(Object value, int depth, IdentityHashMap<Object, Boolean> seen) {
    if (value == null) {
      return JsonNull.INSTANCE;
    }

    if (value instanceof JsonElement element) {
      return element;
    }

    if (depth >= MAX_DEPTH) {
      return new JsonPrimitive(String.valueOf(value));
    }

    if (value instanceof String str) {
      return new JsonPrimitive(str);
    }

    if (value instanceof Number num) {
      return new JsonPrimitive(num);
    }

    if (value instanceof Boolean bool) {
      return new JsonPrimitive(bool);
    }

    if (value instanceof Character ch) {
      return new JsonPrimitive(ch);
    }

    Class<?> cls = value.getClass();
    if (cls.isEnum()) {
      return new JsonPrimitive(((Enum<?>) value).name());
    }

    if (value instanceof Map<?, ?> map) {
      JsonObject obj = new JsonObject();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        String key = String.valueOf(entry.getKey());
        obj.add(key, serialize(entry.getValue(), depth + 1, seen));
      }
      return obj;
    }

    if (value instanceof Iterable<?> iterable) {
      JsonArray arr = new JsonArray();
      for (Object item : iterable) {
        arr.add(serialize(item, depth + 1, seen));
      }
      return arr;
    }

    if (cls.isArray()) {
      int length = Array.getLength(value);
      JsonArray arr = new JsonArray();
      for (int i = 0; i < length; i++) {
        arr.add(serialize(Array.get(value, i), depth + 1, seen));
      }
      return arr;
    }

    if (seen.containsKey(value)) {
      return new JsonPrimitive("[circular]");
    }
    seen.put(value, Boolean.TRUE);

    if (cls.isRecord()) {
      JsonObject obj = new JsonObject();
      for (RecordComponent component : cls.getRecordComponents()) {
        if (component == null || component.getAccessor() == null) {
          continue;
        }
        if (!Modifier.isPublic(component.getAccessor().getModifiers())) {
          continue;
        }
        try {
          Object componentValue = component.getAccessor().invoke(value);
          obj.add(component.getName(), serialize(componentValue, depth + 1, seen));
        } catch (Throwable ignored) {
        }
      }
      if (obj.size() > 0) {
        return obj;
      }
    }

    JsonObject obj = new JsonObject();
    for (Field field : cls.getFields()) {
      if (!Modifier.isPublic(field.getModifiers())) {
        continue;
      }
      try {
        obj.add(field.getName(), serialize(field.get(value), depth + 1, seen));
      } catch (Throwable ignored) {
      }
    }

    if (obj.size() > 0) {
      return obj;
    }

    for (Method method : cls.getMethods()) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      if (method.getParameterCount() != 0) {
        continue;
      }
      if (method.getName().equals("getClass")) {
        continue;
      }
      String name = method.getName();
      String prop = null;
      if (name.startsWith("get") && name.length() > 3) {
        prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
      } else if (name.startsWith("is") && name.length() > 2) {
        prop = Character.toLowerCase(name.charAt(2)) + name.substring(3);
      }
      if (prop == null || prop.isBlank()) {
        continue;
      }
      try {
        obj.add(prop, serialize(method.invoke(value), depth + 1, seen));
      } catch (Throwable ignored) {
      }
    }

    if (obj.size() > 0) {
      return obj;
    }

    return new JsonPrimitive(String.valueOf(value));
  }
}
