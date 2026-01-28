package MBRound18.PortalEngine.api.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public final class UiTemplateLoader {
  private UiTemplateLoader() {
  }

  public static void loadFromReader(@Nonnull Reader reader) {
    UiRegistry.clear();
    HudRegistry.clear();

    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
    registerFromArray(root.getAsJsonArray("ui"), UiRegistry::register);
    registerFromArray(root.getAsJsonArray("hud"), HudRegistry::register);
  }

  public static void loadFromPath(@Nonnull Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      loadFromReader(reader);
    }
  }

  public static boolean loadFromResource(@Nonnull ClassLoader classLoader, @Nonnull String resourcePath)
      throws IOException {
    try (InputStream stream = classLoader.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        return false;
      }
      try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        loadFromReader(reader);
        return true;
      }
    }
  }

  private static void registerFromArray(JsonArray array, java.util.function.Consumer<UiTemplate> registrar) {
    if (array == null) {
      return;
    }
    for (JsonElement element : array) {
      if (!element.isJsonObject()) {
        continue;
      }
      JsonObject obj = element.getAsJsonObject();
      String id = getString(obj, "id");
      String path = getString(obj, "path");
      List<String> vars = getStringList(obj.getAsJsonArray("vars"));
      if (id == null || path == null) {
        continue;
      }
      registrar.accept(new UiTemplate(id, path, vars));
    }
  }

  private static String getString(JsonObject obj, String key) {
    if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) {
      return null;
    }
    return obj.get(key).getAsString();
  }

  private static List<String> getStringList(JsonArray array) {
    List<String> values = new ArrayList<>();
    if (array == null) {
      return values;
    }
    for (JsonElement element : array) {
      if (element.isJsonPrimitive()) {
        values.add(element.getAsString());
      }
    }
    return values;
  }
}
