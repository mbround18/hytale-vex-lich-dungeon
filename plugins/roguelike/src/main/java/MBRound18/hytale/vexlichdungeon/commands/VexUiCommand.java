package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.ImmortalEngine.api.i18n.EngineLang;
import MBRound18.ImmortalEngine.api.ui.HudRegistry;
import MBRound18.ImmortalEngine.api.ui.UiRegistry;
import MBRound18.ImmortalEngine.api.ui.UiTemplate;
import MBRound18.ImmortalEngine.api.ui.UiTemplateLoader;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.hytale.vexlichdungeon.ui.HudController;
import MBRound18.hytale.vexlichdungeon.ui.UIController;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VexUiCommand extends AbstractCommand {
  private static final String PERMISSION_LIST = "vex.ui.list";
  private static final String PERMISSION_SHOW = "vex.ui.show";
  private static final String PERMISSION_RELOAD = "vex.ui.reload";

  private final DataStore dataStore;

  public VexUiCommand(@Nonnull DataStore dataStore) {
    super("ui", "Debug Vex UI pages");
    this.dataStore = dataStore;
    setAllowsExtraArguments(true);
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    String[] tokens = tokenize(context.getInputString());
    int index = skipCommandTokens(tokens, "ui");

    if (index >= tokens.length) {
      sendUsage(context);
      return CompletableFuture.completedFuture(null);
    }

    String action = tokens[index].toLowerCase(Locale.ROOT);
    if ("list".equals(action)) {
      if (!context.sender().hasPermission(PERMISSION_LIST)) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.list.permission")));
        return CompletableFuture.completedFuture(null);
      }
      sendList(context);
      return CompletableFuture.completedFuture(null);
    }

    if ("reload".equals(action)) {
      if (!context.sender().hasPermission(PERMISSION_RELOAD)) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.reload.permission")));
        return CompletableFuture.completedFuture(null);
      }
      if (reloadTemplates(context)) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.reload.success")));
      } else {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.reload.failed")));
      }
      return CompletableFuture.completedFuture(null);
    }

    if ("show".equals(action)) {
      if (!context.sender().hasPermission(PERMISSION_SHOW)) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.show.permission")));
        return CompletableFuture.completedFuture(null);
      }
      if (!context.isPlayer()) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.show.onlyPlayers")));
        return CompletableFuture.completedFuture(null);
      }
      if (index + 1 >= tokens.length) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.show.usage")));
        return CompletableFuture.completedFuture(null);
      }

      String uiId = tokens[index + 1].toLowerCase(Locale.ROOT);
      UiTemplate template = UIController.getTemplate(uiId);
      UiTemplate hudTemplate = null;
      if (template == null) {
        hudTemplate = HudController.getTemplate(uiId);
      }
      if (template == null && hudTemplate == null) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.show.unknown", uiId)));
        sendList(context);
        return CompletableFuture.completedFuture(null);
      }

      Map<String, String> vars = parseVars(tokens, index + 2);
      PlayerRef playerRef = com.hypixel.hytale.server.core.universe.Universe.get()
          .getPlayer(context.sender().getUuid());
      boolean opened = template != null
          ? UIController.openTemplate(playerRef, template, vars)
          : HudController.openTemplate(playerRef, hudTemplate, vars);
      if (!opened) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.show.failed")));
      }
      return CompletableFuture.completedFuture(null);
    }

    sendUsage(context);
    return CompletableFuture.completedFuture(null);
  }

  private void sendUsage(@Nonnull CommandContext context) {
    context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.usage.list")));
    context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.usage.reload")));
    context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.usage.show")));
  }

  private boolean reloadTemplates(@Nonnull CommandContext context) {
    Path templatesPath = dataStore.getDataDirectory().resolve("ui-templates.json");
    try {
      if (Files.exists(templatesPath)) {
        UiTemplateLoader.loadFromPath(templatesPath);
        return true;
      }
      return UiTemplateLoader.loadFromResource(getClass().getClassLoader(), "ui-templates.json");
    } catch (Exception e) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.reload.error", e.getMessage())));
      return false;
    }
  }

  private void sendList(@Nonnull CommandContext context) {
    context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.list.header")));
    for (UiTemplate template : UiRegistry.getTemplates().values()) {
      String vars = template.getVars().isEmpty()
          ? EngineLang.t("command.vex.ui.list.noVars")
          : String.join(", ", template.getVars());
      context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.list.entry", template.getId(), vars)));
    }
    for (UiTemplate template : HudRegistry.getTemplates().values()) {
      String vars = template.getVars().isEmpty()
          ? EngineLang.t("command.vex.ui.list.noVars")
          : String.join(", ", template.getVars());
      context.sendMessage(Message.raw(EngineLang.t("command.vex.ui.list.entry", template.getId(), vars)));
    }
  }

  private static String[] tokenize(String input) {
    String trimmed = input == null ? "" : input.trim();
    if (trimmed.isEmpty()) {
      return new String[0];
    }
    return trimmed.split("\\s+");
  }

  private static int skipCommandTokens(String[] tokens, String commandName) {
    int index = 0;
    if (index < tokens.length && "vex".equalsIgnoreCase(tokens[index])) {
      index++;
    }
    if (index < tokens.length && commandName.equalsIgnoreCase(tokens[index])) {
      index++;
    }
    return index;
  }

  private static Map<String, String> parseVars(String[] tokens, int startIndex) {
    Map<String, String> vars = new LinkedHashMap<>();
    for (int i = startIndex; i < tokens.length; i++) {
      String token = tokens[i];
      if (!token.startsWith("--var-")) {
        continue;
      }
      String assignment = token.substring("--var-".length());
      int eq = assignment.indexOf('=');
      if (eq <= 0) {
        continue;
      }
      String key = assignment.substring(0, eq);
      String value = assignment.substring(eq + 1);
      vars.put(key, value);
    }
    return vars;
  }
}
