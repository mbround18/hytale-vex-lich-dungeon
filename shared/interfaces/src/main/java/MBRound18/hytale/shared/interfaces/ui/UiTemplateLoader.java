package MBRound18.hytale.shared.interfaces.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class UiTemplateLoader {
  private UiTemplateLoader() {
  }

  public static boolean loadFromPath(@Nonnull Path path) throws IOException {
    try (InputStream stream = Files.newInputStream(path)) {
      return loadFromStream(Objects.requireNonNull(stream, "stream"));
    }
  }

  public static boolean loadFromResource(@Nonnull ClassLoader loader, @Nonnull String resourcePath)
      throws IOException {
    try (InputStream stream = loader.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        return false;
      }
      return loadFromStream(Objects.requireNonNull(stream, "stream"));
    }
  }

  private static boolean loadFromStream(@Nonnull InputStream stream) throws IOException {
    JsonElement root = JsonParser.parseReader(new InputStreamReader(stream));
    if (!root.isJsonObject()) {
      return false;
    }
    UiRegistry.clear();
    HudRegistry.clear();
    JsonObject obj = root.getAsJsonObject();
    registerFromArray(obj.getAsJsonArray("ui"), UiRegistry::register);
    registerFromArray(obj.getAsJsonArray("hud"), HudRegistry::register);
    return true;
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
      if (!obj.has("id") || !obj.has("path")) {
        continue;
      }
      String id = obj.get("id").getAsString();
      String path = obj.get("path").getAsString();
      List<String> vars = new ArrayList<>();
      if (obj.has("vars") && obj.get("vars").isJsonArray()) {
        for (JsonElement var : obj.get("vars").getAsJsonArray()) {
          if (var.isJsonPrimitive()) {
            vars.add(var.getAsString());
          }
        }
      }
      registrar.accept(new UiTemplate(
          Objects.requireNonNull(id, "id"),
          Objects.requireNonNull(path, "path"),
          Objects.requireNonNull(vars, "vars")));
    }
  }
}
