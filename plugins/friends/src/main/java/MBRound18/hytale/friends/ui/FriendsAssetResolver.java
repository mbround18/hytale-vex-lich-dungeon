package MBRound18.hytale.friends.ui;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

public final class FriendsAssetResolver {
  private static volatile Path assetsZipPath;

  private FriendsAssetResolver() {
  }

  public static void setAssetsZipPath(@Nullable Path zipPath) {
    assetsZipPath = zipPath;
  }

  @Nullable
  public static String readInlineDocument(@Nullable String uiPath) {
    String normalized = normalizePath(uiPath);
    String resolved = resolvePath(normalized);
    String doc = readDocument(resolved != null ? resolved : normalized);
    if (doc == null) {
      return null;
    }
    if (doc.contains(".ui") || doc.contains("$") || doc.contains("@Import")) {
      return null;
    }
    return doc;
  }

  @Nullable
  public static String resolvePath(@Nullable String uiPath) {
    String normalized = normalizePath(uiPath);
    if (normalized == null || assetsZipPath == null) {
      return normalized;
    }
    List<String> candidates = buildCandidates(normalized);
    try (ZipFile zipFile = new ZipFile(assetsZipPath.toFile())) {
      for (String candidate : candidates) {
        ZipEntry entry = zipFile.getEntry(candidate);
        if (entry != null) {
          if (normalized.startsWith("/") && candidate.equals(normalized.substring(1))) {
            return normalized;
          }
          return candidate;
        }
      }
    } catch (Exception ignored) {
      return normalized;
    }
    return normalized;
  }

  @Nullable
  public static String readDocument(@Nullable String uiPath) {
    String normalized = normalizePath(uiPath);
    if (normalized == null || assetsZipPath == null) {
      return null;
    }
    List<String> candidates = buildCandidates(normalized);
    try (ZipFile zipFile = new ZipFile(assetsZipPath.toFile())) {
      for (String candidate : candidates) {
        ZipEntry entry = zipFile.getEntry(candidate);
        if (entry == null) {
          continue;
        }
        try (InputStream stream = zipFile.getInputStream(entry)) {
          return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
      }
    } catch (Exception ignored) {
      return null;
    }
    return null;
  }

  private static List<String> buildCandidates(String uiPath) {
    List<String> candidates = new ArrayList<>();
    candidates.add(uiPath);
    String trimmed = uiPath;
    // if (uiPath.startsWith("/")) {
    // trimmed = uiPath.substring(1);
    // candidates.add(trimmed);
    // }
    // if (trimmed.startsWith("Common/")) {
    // candidates.add(trimmed.substring("Common/".length()));
    // }
    // if (trimmed.startsWith("UI/")) {
    // candidates.add(trimmed.substring("UI/".length()));
    // candidates.add("Common/" + trimmed);
    // }
    // if (trimmed.startsWith("Custom/")) {
    // candidates.add("UI/" + trimmed);
    // candidates.add("Common/UI/" + trimmed);
    // candidates.add("Common/" + trimmed);
    // }
    // if (trimmed.startsWith("Common/UI/")) {
    // candidates.add(trimmed.substring("Common/".length()));
    // }
    return candidates;
  }

  @Nullable
  private static String normalizePath(@Nullable String uiPath) {
    if (uiPath == null) {
      return null;
    }
    String normalized = uiPath.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
