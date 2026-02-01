package MBRound18.hytale.friends.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

public class FriendsUiPage extends AbstractCustomUIPage {
  private static final Pattern ID_PATTERN = Pattern.compile("#([A-Za-z0-9_]+)");
  private final String uiPath;
  private final Map<String, String> vars;

  public FriendsUiPage(@Nonnull PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    super(playerRef, CustomPageLifetime.CanDismiss, uiPath, vars, new UiDocumentResolver() {
      @Override
      public String resolvePath(@Nonnull String path) {
        return FriendsAssetResolver.resolvePath(path);
      }

      @Override
      public String readInlineDocument(@Nonnull String path) {
        return FriendsAssetResolver.readInlineDocument(path);
      }
    });
    this.uiPath = uiPath;
    this.vars = vars;
  }

  @Override
  public void init(@Nonnull UICommandBuilder commands) {
    String resolvedPath = FriendsAssetResolver.resolvePath(uiPath);
    String inline = FriendsAssetResolver.readInlineDocument(uiPath);
    String doc = inline != null ? inline : FriendsAssetResolver.readDocument(resolvedPath != null ? resolvedPath : uiPath);
    Set<String> ids = extractIds(doc);
    for (Map.Entry<String, String> entry : vars.entrySet()) {
      String id = entry.getKey();
      if (!ids.isEmpty()) {
        String baseId = stripSelectorBase(id);
        if (baseId != null && !ids.contains(baseId)) {
          continue;
        }
      }
      if (id != null && !id.startsWith("#")) {
        id = "#" + id;
      }
      if (id != null && !id.contains(".")) {
        id = id + ".Text";
      }
      if (id != null) {
        commands.set(id, entry.getValue());
      }
    }
  }

  private static Set<String> extractIds(String doc) {
    if (doc == null || doc.isBlank()) {
      return java.util.Collections.emptySet();
    }
    Matcher matcher = ID_PATTERN.matcher(doc);
    Set<String> ids = new HashSet<>();
    while (matcher.find()) {
      ids.add(matcher.group(1));
    }
    return ids;
  }

  private static String stripSelectorBase(String selector) {
    if (selector == null) {
      return null;
    }
    String trimmed = selector.trim();
    if (trimmed.startsWith("#")) {
      trimmed = trimmed.substring(1);
    }
    int dot = trimmed.indexOf('.');
    return dot >= 0 ? trimmed.substring(0, dot) : trimmed;
  }
}
