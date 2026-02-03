package MBRound18.hytale.shared.interfaces.commands;

import MBRound18.hytale.shared.interfaces.ui.UiTemplate;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// Shared debug command for demo UI/HUD interfaces (e.g., /dui, /dhud).
public class UiCommand extends AbstractCommand {
  public enum Mode {
    UI,
    HUD,
    BOTH;

    public boolean supportsUi() {
      return this == UI || this == BOTH;
    }

    public boolean supportsHud() {
      return this == HUD || this == BOTH;
    }
  }

  private static final String SUB_HELP = "help";
  private static final String SUB_LIST = "list";
  private static final String SUB_SHOW = "show";
  private static final String SUB_CLEAR = "clear";
  private static final String SUB_RELOAD = "reload";
  private static final String SUB_DEMO = "demo";
  private static final String SUB_TEST = "test";

  private final Mode mode;
  private final UiCommandHandler handler;
  private final @Nullable String rootCommand;
  private final @Nonnull String usagePrefix;
  private final @Nullable String permissionList;
  private final @Nullable String permissionShow;
  private final @Nullable String permissionReload;
  private final @Nonnull String commandToken;

  public UiCommand(@Nonnull String name, @Nonnull String description, @Nonnull Mode mode,
      @Nonnull UiCommandHandler handler) {
    this(name, description, mode, handler, null, null, null, null);
  }

  public UiCommand(@Nonnull String name, @Nonnull String description, @Nonnull Mode mode,
      @Nonnull UiCommandHandler handler, @Nullable String rootCommand,
      @Nullable String permissionList, @Nullable String permissionShow, @Nullable String permissionReload) {
    super(name, description);
    setAllowsExtraArguments(true);
    this.mode = Objects.requireNonNull(mode, "mode");
    this.handler = Objects.requireNonNull(handler, "handler");
    this.rootCommand = rootCommand;
    this.permissionList = permissionList;
    this.permissionShow = permissionShow;
    this.permissionReload = permissionReload;
    this.commandToken = Objects.requireNonNull(name, "name");
    if (rootCommand == null || rootCommand.isBlank()) {
      this.usagePrefix = "/" + name;
    } else {
      this.usagePrefix = "/" + rootCommand + " " + name;
    }
    registerSubCommands();
  }

  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    @Nonnull
    String[] tokens = tokenize(context.getInputString());
    int index = skipCommandTokens(tokens);
    if (index >= tokens.length) {
      sendHelp(context);
      return CompletableFuture.completedFuture(null);
    }

    if (!checkPermission(context, permissionShow)) {
      return CompletableFuture.completedFuture(null);
    }
    showFromTokens(context, tokens, index);
    return CompletableFuture.completedFuture(null);
  }

  private void registerSubCommands() {
    addSubCommand(new HelpSubCommand());
    addSubCommand(new ListSubCommand());
    addSubCommand(new ShowSubCommand());
    addSubCommand(new ClearSubCommand());
    addSubCommand(new ReloadSubCommand());
    addSubCommand(new DemoSubCommand());
    addSubCommand(new TestSubCommand());
  }

  private void showFromTokens(@Nonnull CommandContext context, @Nonnull String[] tokens, int index) {
    if (index >= tokens.length) {
      sendHelp(context);
      return;
    }
    PlayerRef playerRef = requirePlayer(context);
    if (playerRef == null) {
      return;
    }
    @Nonnull
    String id = Objects.requireNonNull(tokens[index], "id");
    @Nonnull
    Map<String, String> vars = parseVars(tokens, index + 1);

    if (handler.handleCustomShow(context, playerRef, id, vars)) {
      return;
    }

    UiTemplate template = null;
    boolean isHud = false;
    if (mode.supportsUi()) {
      template = findTemplate(handler.getUiTemplates(), id);
    }
    if (template == null && mode.supportsHud()) {
      template = findTemplate(handler.getHudTemplates(), id);
      isHud = template != null;
    }

    if (template == null && looksLikePath(id)) {
      UiTemplate adhoc = new UiTemplate(id, id,
          Objects.requireNonNull(java.util.List.<String>of(), "vars"));
      if (mode == Mode.HUD) {
        isHud = true;
      } else if (mode == Mode.UI) {
        isHud = false;
      } else {
        String lower = id.toLowerCase(Locale.ROOT);
        isHud = lower.contains("/hud") || lower.contains("/huds/");
      }
      template = adhoc;
    }

    if (template == null) {
      context.sendMessage(Message.raw("Unknown UI id '" + id + "'."));
      sendList(context);
      return;
    }

    boolean opened = isHud
        ? handler.openHud(playerRef, template, vars)
        : handler.openUi(playerRef, template, vars);
    if (!opened) {
      context.sendMessage(Message.raw("Failed to open UI '" + id + "'."));
    }
  }

  private void sendHelp(@Nonnull CommandContext context) {
    String label;
    if (mode == Mode.UI) {
      label = "UI";
    } else if (mode == Mode.HUD) {
      label = "HUD";
    } else {
      label = "UI/HUD";
    }
    context.sendMessage(Message.raw("Usage: " + usagePrefix + " <subcommand> [args]"));
    context.sendMessage(Message.raw("Subcommands:"));
    context.sendMessage(Message.raw("  " + SUB_HELP + " - Show command help. (alias: ?)"));
    context.sendMessage(Message.raw("  " + SUB_LIST + " - List available " + label + " templates. (alias: ls)"));
    context.sendMessage(Message.raw("  " + SUB_SHOW
        + " <id> [--var-KEY=VALUE] - Open a " + label + " template or .ui path."));
    if (mode.supportsHud()) {
      context.sendMessage(Message.raw("  " + SUB_CLEAR
          + " - Clear the active HUD. (alias: reset)"));
    }
    if (handler.supportsReload()) {
      context.sendMessage(Message.raw("  " + SUB_RELOAD
          + " - Reload UI templates."));
    }
    if (handler.supportsDemo()) {
      context.sendMessage(Message.raw("  " + SUB_DEMO
          + " - Start demo mode."));
    }
    if (handler.supportsTest()) {
      context.sendMessage(Message.raw("  " + SUB_TEST
          + " [seconds] - Start test mode. (1-30s)"));
    }
    context.sendMessage(Message.raw("Tip: " + usagePrefix + " <id> is shorthand for "
        + usagePrefix + " show <id>."));
  }

  private void sendList(@Nonnull CommandContext context) {
    if (mode.supportsUi()) {
      context.sendMessage(Message.raw("UI templates: " + joinTemplateIds(handler.getUiTemplates())));
    }
    if (mode.supportsHud()) {
      context.sendMessage(Message.raw("HUD templates: " + joinTemplateIds(handler.getHudTemplates())));
    }
  }

  private static String joinTemplateIds(@Nonnull Map<String, UiTemplate> templates) {
    if (templates.isEmpty()) {
      return "(none)";
    }
    return String.join(", ", templates.keySet());
  }

  private int skipCommandTokens(@Nonnull String[] tokens) {
    int index = 0;
    if (rootCommand != null && index < tokens.length
        && tokenEquals(Objects.requireNonNull(rootCommand, "rootCommand"), tokens[index])) {
      index++;
    }
    if (index < tokens.length && tokenEquals(commandToken, tokens[index])) {
      index++;
    }
    return index;
  }

  private boolean checkPermission(@Nonnull CommandContext context, @Nullable String permission) {
    if (permission == null || permission.isBlank()) {
      return true;
    }
    if (context.sender().hasPermission(permission)) {
      return true;
    }
    context.sendMessage(Message.raw("Missing permission: " + permission));
    return false;
  }

  @Nullable
  private PlayerRef requirePlayer(@Nonnull CommandContext context) {
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("This command can only be used by players."));
      return null;
    }
    PlayerRef playerRef = Universe.get().getPlayer(
        Objects.requireNonNull(context.sender().getUuid(), "sender uuid"));
    if (playerRef == null || !playerRef.isValid()) {
      context.sendMessage(Message.raw("Failed to resolve player."));
      return null;
    }
    return playerRef;
  }

  @Nullable
  private static UiTemplate findTemplate(@Nonnull Map<String, UiTemplate> templates, @Nonnull String id) {
    UiTemplate template = templates.get(id);
    if (template != null) {
      return template;
    }
    String lower = id.toLowerCase(Locale.ROOT);
    return templates.get(lower);
  }

  private static boolean looksLikePath(@Nonnull String value) {
    return value.contains("/") || value.contains("\\") || value.endsWith(".ui");
  }

  @Nullable
  private static String normalizeToken(@Nullable String token) {
    if (token == null) {
      return null;
    }
    String trimmed = token.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.startsWith("/")) {
      return trimmed.substring(1);
    }
    return trimmed;
  }

  private static boolean tokenEquals(@Nonnull String expected, @Nullable String token) {
    String normalized = normalizeToken(token);
    return normalized != null && expected.equalsIgnoreCase(normalized);
  }

  @Nonnull
  private static String[] tokenize(@Nullable String input) {
    String trimmed = input == null ? "" : input.trim();
    if (trimmed.isEmpty()) {
      return new String[0];
    }
    return Objects.requireNonNull(trimmed.split("\\s+"), "tokens");
  }

  @Nonnull
  private static Map<String, String> parseVars(@Nonnull String[] tokens, int startIndex) {
    Map<String, String> vars = new LinkedHashMap<>();
    for (int i = startIndex; i < tokens.length; i++) {
      String token = tokens[i];
      String assignment;
      if (token.startsWith("--var-")) {
        assignment = token.substring("--var-".length());
      } else {
        if (!token.contains("=")) {
          continue;
        }
        assignment = token.startsWith("--") ? token.substring(2) : token;
      }
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

  private static int parseDelaySeconds(@Nonnull String[] tokens, int index) {
    if (index >= tokens.length) {
      return 3;
    }
    try {
      int value = Integer.parseInt(tokens[index]);
      if (value < 1) {
        return 1;
      }
      if (value > 30) {
        return 30;
      }
      return value;
    } catch (NumberFormatException e) {
      return 3;
    }
  }

  private final class HelpSubCommand extends AbstractCommand {
    private HelpSubCommand() {
      super(SUB_HELP, "Show command help");
      addAliases("?");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      sendHelp(context);
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class ListSubCommand extends AbstractCommand {
    private ListSubCommand() {
      super(SUB_LIST, "List available templates");
      addAliases("ls");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!checkPermission(context, permissionList)) {
        return CompletableFuture.completedFuture(null);
      }
      sendList(context);
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class ShowSubCommand extends AbstractCommand {
    private ShowSubCommand() {
      super(SUB_SHOW, "Open a template by id or path");
      setAllowsExtraArguments(true);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!checkPermission(context, permissionShow)) {
        return CompletableFuture.completedFuture(null);
      }
      String[] tokens = tokenize(context.getInputString());
      int index = skipCommandTokens(tokens) + 1;
      showFromTokens(context, tokens, index);
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class ClearSubCommand extends AbstractCommand {
    private ClearSubCommand() {
      super(SUB_CLEAR, "Clear the active HUD");
      addAliases("reset");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!checkPermission(context, permissionShow)) {
        return CompletableFuture.completedFuture(null);
      }
      if (!mode.supportsHud()) {
        context.sendMessage(Message.raw("HUD reset is not available for this command."));
        return CompletableFuture.completedFuture(null);
      }
      PlayerRef playerRef = requirePlayer(context);
      if (playerRef == null) {
        return CompletableFuture.completedFuture(null);
      }
      boolean cleared = handler.clearHud(playerRef);
      context.sendMessage(Message.raw(cleared ? "HUD reset." : "No HUD to reset."));
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class ReloadSubCommand extends AbstractCommand {
    private ReloadSubCommand() {
      super(SUB_RELOAD, "Reload UI templates");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!checkPermission(context, permissionReload)) {
        return CompletableFuture.completedFuture(null);
      }
      if (!handler.supportsReload()) {
        context.sendMessage(Message.raw("Reload is not supported for this command."));
        return CompletableFuture.completedFuture(null);
      }
      boolean ok = handler.reloadTemplates(context);
      context.sendMessage(Message.raw(ok ? "UI templates reloaded." : "Failed to reload UI templates."));
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class DemoSubCommand extends AbstractCommand {
    private DemoSubCommand() {
      super(SUB_DEMO, "Start demo mode");
      setAllowsExtraArguments(true);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!checkPermission(context, permissionShow)) {
        return CompletableFuture.completedFuture(null);
      }
      if (!handler.supportsDemo()) {
        context.sendMessage(Message.raw("Demo mode is not supported for this command."));
        return CompletableFuture.completedFuture(null);
      }
      PlayerRef playerRef = requirePlayer(context);
      if (playerRef == null) {
        return CompletableFuture.completedFuture(null);
      }
      String[] tokens = tokenize(context.getInputString());
      int index = skipCommandTokens(tokens) + 1;
      boolean ok = handler.startDemo(context, playerRef, parseVars(tokens, index));
      if (!ok) {
        context.sendMessage(Message.raw("Failed to start demo."));
      }
      return CompletableFuture.completedFuture(null);
    }
  }

  private final class TestSubCommand extends AbstractCommand {
    private TestSubCommand() {
      super(SUB_TEST, "Start test mode");
      setAllowsExtraArguments(true);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!checkPermission(context, permissionShow)) {
        return CompletableFuture.completedFuture(null);
      }
      if (!handler.supportsTest()) {
        context.sendMessage(Message.raw("Test mode is not supported for this command."));
        return CompletableFuture.completedFuture(null);
      }
      PlayerRef playerRef = requirePlayer(context);
      if (playerRef == null) {
        return CompletableFuture.completedFuture(null);
      }
      String[] tokens = tokenize(context.getInputString());
      int index = skipCommandTokens(tokens) + 1;
      int delaySeconds = parseDelaySeconds(tokens, index);
      boolean ok = handler.startTest(context, playerRef, delaySeconds);
      if (!ok) {
        context.sendMessage(Message.raw("Failed to start test."));
      }
      return CompletableFuture.completedFuture(null);
    }
  }
}
