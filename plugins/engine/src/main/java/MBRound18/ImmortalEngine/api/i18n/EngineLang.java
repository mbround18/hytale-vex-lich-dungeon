package MBRound18.ImmortalEngine.api.i18n;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

/**
 * Lightweight language loader for server-side messages.
 * <p>
 * This helper reads {@code Server/Languages/<locale>/server.lang} from a bundled
 * assets ZIP and resolves the best available locale using the JVM default. If no
 * matching locale is found, it falls back to {@code en-US}. When a key is missing,
 * the key itself is returned.
 */
public final class EngineLang {
  private static final String DEFAULT_LANG_TAG = "en-US";
  private static final Map<String, String> STRINGS = new ConcurrentHashMap<>();
  private static volatile boolean loaded = false;
  private static volatile Path assetsZipPath;
  private static volatile String langPath = toLangPath(DEFAULT_LANG_TAG);

  private EngineLang() {
  }

  /**
   * Sets the assets ZIP path and clears any cached translations.
   *
   * @param zipPath Path to the assets ZIP containing {@code Server/Languages/*}
   */
  public static void setAssetsZipPath(@Nullable Path zipPath) {
    assetsZipPath = zipPath;
    STRINGS.clear();
    loaded = false;
    resolveLangPath(Locale.getDefault());
  }

  /**
   * Resolves a localized string by key.
   *
   * @param key  Translation key (e.g., {@code server.items.foo.name})
   * @param args Optional {@link String#format(Locale, String, Object...)} args
   * @return Resolved string or key if missing
   */
  public static String t(String key, Object... args) {
    ensureLoaded();
    String value = STRINGS.getOrDefault(key, key);
    if (args == null || args.length == 0) {
      return value;
    }
    return String.format(Locale.ROOT, value, args);
  }

  private static void ensureLoaded() {
    if (loaded) {
      return;
    }
    synchronized (EngineLang.class) {
      if (loaded) {
        return;
      }
      loadFromAssetsZip();
      loaded = true;
    }
  }

  private static void loadFromAssetsZip() {
    if (assetsZipPath == null || !Files.exists(assetsZipPath)) {
      return;
    }
    try (ZipFile zipFile = new ZipFile(assetsZipPath.toFile())) {
      ZipEntry entry = zipFile.getEntry(langPath);
      if (entry == null && !langPath.equals(toLangPath(DEFAULT_LANG_TAG))) {
        entry = zipFile.getEntry(toLangPath(DEFAULT_LANG_TAG));
      }
      if (entry == null) {
        return;
      }
      try (InputStream stream = zipFile.getInputStream(entry);
          BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          String trimmed = line.trim();
          if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
            continue;
          }
          int eq = trimmed.indexOf('=');
          if (eq <= 0) {
            continue;
          }
          String key = trimmed.substring(0, eq).trim();
          String value = trimmed.substring(eq + 1).trim();
          if (!key.isEmpty()) {
            STRINGS.put(key, value);
          }
        }
      }
    } catch (Exception ignored) {
      // Fail silently; fallback to key name.
    }
  }

  private static void resolveLangPath(Locale locale) {
    if (assetsZipPath == null || !Files.exists(assetsZipPath)) {
      langPath = toLangPath(DEFAULT_LANG_TAG);
      return;
    }
    String[] candidates = buildCandidates(locale);
    try (ZipFile zipFile = new ZipFile(assetsZipPath.toFile())) {
      for (String candidate : candidates) {
        String path = toLangPath(candidate);
        if (zipFile.getEntry(path) != null) {
          langPath = path;
          return;
        }
      }
    } catch (Exception ignored) {
      // fallback below
    }
    langPath = toLangPath(DEFAULT_LANG_TAG);
  }

  private static String[] buildCandidates(Locale locale) {
    String lang = locale.getLanguage();
    String country = locale.getCountry();
    if (lang == null || lang.isBlank()) {
      return new String[] { DEFAULT_LANG_TAG };
    }
    lang = lang.toLowerCase(Locale.ROOT);
    String langCountry = country == null || country.isBlank()
        ? null
        : lang + "-" + country.toUpperCase(Locale.ROOT);
    if (langCountry != null) {
      return new String[] { langCountry, lang, DEFAULT_LANG_TAG };
    }
    return new String[] { lang, DEFAULT_LANG_TAG };
  }

  private static String toLangPath(String langTag) {
    return "Server/Languages/" + langTag + "/server.lang";
  }
}
