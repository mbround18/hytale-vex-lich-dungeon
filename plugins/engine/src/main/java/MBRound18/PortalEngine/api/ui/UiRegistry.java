package MBRound18.PortalEngine.api.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class UiRegistry {
  private static final Map<String, UiTemplate> TEMPLATES = new LinkedHashMap<>();

  private UiRegistry() {
  }

  public static void register(@Nonnull UiTemplate template) {
    TEMPLATES.put(template.getId(), template);
  }

  public static void registerAll(@Nonnull Map<String, UiTemplate> templates) {
    TEMPLATES.putAll(templates);
  }

  @Nullable
  public static UiTemplate getTemplate(@Nonnull String id) {
    return TEMPLATES.get(id);
  }

  public static Map<String, UiTemplate> getTemplates() {
    return Collections.unmodifiableMap(TEMPLATES);
  }

  public static void clear() {
    TEMPLATES.clear();
  }
}
