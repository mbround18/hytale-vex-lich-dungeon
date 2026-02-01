package MBRound18.hytale.friends.ui;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import MBRound18.hytale.shared.interfaces.ui.UiPath;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;
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
    LinkedHashSet<String> candidates = new LinkedHashSet<>();
    addCandidate(candidates, uiPath);
    String trimmed = uiPath;
    if (uiPath.startsWith("/")) {
      trimmed = uiPath.substring(1);
      addCandidate(candidates, trimmed);
    }
    if (trimmed.startsWith("Common/")) {
      addCandidate(candidates, trimmed.substring("Common/".length()));
    }
    if (trimmed.startsWith("UI/")) {
      addCandidate(candidates, "Common/" + trimmed);
    }
    if (trimmed.startsWith("Custom/")) {
      addCandidate(candidates, "UI/" + trimmed);
      addCandidate(candidates, "Common/UI/" + trimmed);
      addCandidate(candidates, "Common/" + trimmed);
    }
    if (trimmed.startsWith("Common/UI/")) {
      addCandidate(candidates, trimmed.substring("Common/".length()));
    }
    String clientPath = UiPath.normalizeForClient(trimmed);
    if (clientPath != null) {
      addCandidate(candidates, clientPath);
      addCandidate(candidates, "Custom/" + clientPath);
      addCandidate(candidates, "UI/Custom/" + clientPath);
      addCandidate(candidates, "Common/UI/Custom/" + clientPath);
      addCandidate(candidates, "Common/UI/" + clientPath);
      addCandidate(candidates, "Common/" + clientPath);
    }
    return new ArrayList<>(candidates);
  }

  private static void addCandidate(@Nonnull LinkedHashSet<String> candidates, @Nullable String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    candidates.add(value);
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
