package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.ImmortalEngine.api.ui.UiPath;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class VexDebugHudPage extends CustomUIHud {
  private static final String HUD_ROOT_SELECTOR = "#hud-root";
  private static final Pattern ID_PATTERN = Pattern.compile("#([A-Za-z0-9_]+)");
  private static final Logger LOGGER = Logger.getLogger(VexDebugHudPage.class.getName());
  private static final boolean FORCE_EMPTY_BUILD = false;
  private static final boolean FORCE_INLINE_DEBUG = false;
  private static final boolean FORCE_RAW_PATH = false;
  private static final String INLINE_DEBUG_DOC = "Group { }";

  private final String uiPath;
  private final Map<String, String> vars;
  private String resolvedPath;
  private Set<String> knownIds;

  public VexDebugHudPage(@Nonnull PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    super(playerRef);
    this.uiPath = uiPath;
    this.vars = vars;
  }

  public boolean matchesPath(@Nonnull String otherPath) {
    String current = normalizePath(resolvePath());
    String other = normalizePath(UiAssetResolver.resolvePath(otherPath));
    if (current == null || other == null) {
      return Objects.equals(current, other);
    }
    return current.equals(other);
  }

  String getUiPath() {
    return uiPath;
  }

  Map<String, String> getVars() {
    return vars;
  }

  @Override
  public void show() {
    // Test: send CustomHud updates without clearing to see if clear flag crashes the client.
    UICommandBuilder commands = new UICommandBuilder();
    build(commands);
    CustomUICommand[] built = commands.getCommands();
    if (built == null || built.length == 0) {
      return;
    }
    update(false, commands);
  }

  @Override
  protected void build(UICommandBuilder commands) {
    if (FORCE_EMPTY_BUILD) {
      logCommands("Build", commands.getCommands());
      return;
    }
    if (FORCE_INLINE_DEBUG) {
      commands.appendInline(HUD_ROOT_SELECTOR, INLINE_DEBUG_DOC);
      logCommands("Build", commands.getCommands());
      return;
    }
    String resolved = resolvePath();
    String rawPath = uiPath;
    String clientPath = UiPath.normalizeForClient(rawPath != null ? rawPath : resolved);
    if (FORCE_RAW_PATH && rawPath != null) {
      commands.append(HUD_ROOT_SELECTOR, rawPath);
    } else if ((resolved != null || (uiPath != null && uiPath.endsWith(".ui"))) && clientPath != null) {
      commands.append(HUD_ROOT_SELECTOR, clientPath);
    } else {
      String inline = UiAssetResolver.readInlineDocument(uiPath);
      if (inline != null) {
        commands.appendInline(HUD_ROOT_SELECTOR, inline);
      } else {
        commands.append(HUD_ROOT_SELECTOR, clientPath != null ? clientPath : uiPath);
      }
    }
    logCommands("Build", commands.getCommands());
  }

  void appendVarCommands(@Nonnull UICommandBuilder commands, @Nonnull Map<String, String> vars) {
    Set<String> ids = getKnownIds();
    for (Map.Entry<String, String> entry : vars.entrySet()) {
      String id = entry.getKey();
      String baseId = stripSelectorBase(id);
      if (!ids.isEmpty() && baseId != null && !ids.contains(baseId)) {
        continue;
      }
      if (id != null && id.contains(".")) {
        String selector = id.startsWith("#") ? id : "#" + id;
        commands.set(selector, Message.raw(entry.getValue()));
        continue;
      }
      String selectorBase = baseId;
      if (selectorBase == null || selectorBase.isBlank()) {
        continue;
      }
      String selector = selectorBase.startsWith("#") ? selectorBase : "#" + selectorBase;
      String selectorText = selector + ".TextSpans";
      commands.set(selectorText, Message.raw(entry.getValue()));
    }
  }

  private String resolvePath() {
    if (resolvedPath == null) {
      resolvedPath = UiAssetResolver.resolvePath(uiPath);
    }
    return resolvedPath;
  }

  private Set<String> getKnownIds() {
    if (knownIds != null) {
      return knownIds;
    }
    String inline = UiAssetResolver.readInlineDocument(uiPath);
    String doc = inline != null ? inline : UiAssetResolver.readDocument(resolvePath() != null ? resolvePath() : uiPath);
    knownIds = extractIds(doc);
    return knownIds;
  }

  private static String normalizePath(String path) {
    if (path == null) {
      return null;
    }
    String normalized = UiPath.normalizeForClient(path);
    return normalized != null ? normalized : path;
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

  private static void logCommands(String phase, CustomUICommand[] commands) {
    if (commands == null) {
      return;
    }
    for (CustomUICommand command : commands) {
      if (command == null) {
        continue;
      }
      LOGGER.info(String.format("[HUD-BUILD] %s selector=%s data=%s text=%s",
          phase,
          command.selector,
          command.data,
          command.text));
    }
  }
}
