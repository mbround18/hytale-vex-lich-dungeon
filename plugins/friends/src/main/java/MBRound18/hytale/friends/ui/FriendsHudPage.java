package MBRound18.hytale.friends.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.UiPath;
import MBRound18.hytale.shared.interfaces.ui.UiVars;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

public class FriendsHudPage extends AbstractCustomUIHud {
  private static final Pattern ID_PATTERN = Pattern.compile("#([A-Za-z0-9_]+)");

  private final String uiPath;
  private final Map<String, String> vars;
  private Set<String> knownIds;

  public FriendsHudPage(@Nonnull PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    super(uiPath, playerRef);
    this.uiPath = uiPath;
    this.vars = vars;
  }

  @Override
  protected void build(UICommandBuilder commands) {
    String resolvedPath = FriendsAssetResolver.resolvePath(uiPath);
    String inline = FriendsAssetResolver.readInlineDocument(uiPath);
    String doc = inline != null ? inline
        : FriendsAssetResolver.readDocument(resolvedPath != null ? resolvedPath : uiPath);
    knownIds = extractIds(doc);
    if (inline != null) {
      commands.appendInline(null, inline);
    } else {
      String clientPath = UiPath.normalizeForClient(resolvedPath != null ? resolvedPath : uiPath);
      commands.append(clientPath != null ? clientPath : uiPath);
    }
  }

  void appendVarCommands(@Nonnull UICommandBuilder commands, @Nonnull Map<String, String> vars) {
    Set<String> ids = getKnownIds();
    for (Map.Entry<String, String> entry : vars.entrySet()) {
      String id = entry.getKey();
      if (!ids.isEmpty()) {
        String baseId = stripSelectorBase(id);
        if (baseId != null && !ids.contains(baseId)) {
          continue;
        }
      }
      if (id != null && !id.contains(".")) {
        id = UiVars.textSpansId(id);
      }
      if (id == null) {
        continue;
      }
      if (!id.startsWith("#")) {
        id = "#" + id;
      }
      String value = entry.getValue();
      String safeValue = value == null ? "" : value;
      commands.set(id, Message.raw(Objects.requireNonNull(safeValue, "value")));
    }
  }

  Set<String> getKnownIds() {
    return knownIds == null ? java.util.Collections.emptySet() : knownIds;
  }

  Map<String, String> getVars() {
    return vars;
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
