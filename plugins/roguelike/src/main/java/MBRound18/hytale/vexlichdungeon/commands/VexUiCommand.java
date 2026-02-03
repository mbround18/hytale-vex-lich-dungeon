package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIPage;
import MBRound18.hytale.shared.interfaces.ui.CustomHudController;
import MBRound18.hytale.shared.interfaces.ui.DebugUiPage;
import MBRound18.hytale.shared.utilities.UiThread;
import MBRound18.hytale.vexlichdungeon.ui.VexUiCatalog;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VexUiCommand extends AbstractCommand {
  public VexUiCommand() {
    super("ui", "Vex UI utilities");
    addSubCommand(new ListSubCommand());
    addSubCommand(new TestSubCommand());
  }

  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    context.sendMessage(Message.raw("Usage: /vex ui <list|test>"));
    return CompletableFuture.completedFuture(null);
  }

  private static final class ListSubCommand extends AbstractCommand {
    private ListSubCommand() {
      super("list", "List Vex UI and HUD templates");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      VexUiCatalog.registerDefaults();
      context.sendMessage(Message.raw("Vex UI pages: " + String.join(", ", VexUiCatalog.listUiIds())));
      context.sendMessage(Message.raw("Vex HUDs: " + String.join(", ", VexUiCatalog.listHudIds())));
      return CompletableFuture.completedFuture(null);
    }
  }

  private static final class TestSubCommand extends AbstractCommand {
    private TestSubCommand() {
      super("test", "Open a Vex UI or HUD by name");
      setAllowsExtraArguments(true);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
      if (!context.isPlayer()) {
        context.sendMessage(Message.raw("This command can only be used by players."));
        return CompletableFuture.completedFuture(null);
      }
      @Nonnull
      String[] tokens = tokenize(context.getInputString());
      int index = skipCommandTokens(tokens, "vex", "ui", "test");
      if (index >= tokens.length) {
        context.sendMessage(Message.raw("Usage: /vex ui test <name>"));
        context.sendMessage(Message.raw("Use /vex ui list to see available templates."));
        return CompletableFuture.completedFuture(null);
      }

      String name = tokens[index];
      VexUiCatalog.ResolvedTemplate resolved = VexUiCatalog.resolve(name);
      if (resolved == null) {
        context.sendMessage(Message.raw("Unknown Vex UI '" + name + "'. Use /vex ui list."));
        return CompletableFuture.completedFuture(null);
      }

      PlayerRef playerRef = requirePlayer(context);
      if (playerRef == null) {
        return CompletableFuture.completedFuture(null);
      }

      Map<String, String> vars = VexUiCatalog.defaultVars(resolved.getPrimaryId());
      boolean scheduled = UiThread.runOnPlayerWorld(playerRef, () -> {
        if (resolved.isHud()) {
          openHud(playerRef, resolved.getPath(), vars);
        } else {
          openUi(playerRef, resolved.getPath(), vars);
        }
      });

      if (!scheduled) {
        context.sendMessage(Message.raw("Unable to open Vex UI."));
      }
      return CompletableFuture.completedFuture(null);
    }

    private static void openUi(@Nonnull PlayerRef playerRef, @Nonnull String uiPath,
        @Nonnull Map<String, String> vars) {
      Ref<EntityStore> ref = playerRef.getReference();
      if (ref == null || !ref.isValid()) {
        return;
      }
      Store<EntityStore> store = ref.getStore();
      Player player = store.getComponent(ref, Player.getComponentType());
      if (player == null) {
        return;
      }
      DebugUiPage page = new DebugUiPage(playerRef, uiPath, vars);
      player.getPageManager().openCustomPage(ref, store, page);
      page.applyInitialState();
    }

    private static void openHud(@Nonnull PlayerRef playerRef, @Nonnull String hudPath,
        @Nonnull Map<String, String> vars) {
      Ref<EntityStore> ref = playerRef.getReference();
      if (ref == null || !ref.isValid()) {
        return;
      }
      Store<EntityStore> store = ref.getStore();
      Player player = store.getComponent(ref, Player.getComponentType());
      if (player == null) {
        return;
      }
      HudManager hudManager = Objects.requireNonNull(player.getHudManager(), "hudManager");
      CustomHudController hud = new CustomHudController(hudPath, playerRef);
      if (!hud.isActiveHud(playerRef)) {
        hudManager.setCustomHud(playerRef, hud);
      }
      if (!vars.isEmpty()) {
        for (Map.Entry<String, String> entry : vars.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          if (key == null || value == null) {
            continue;
          }
          hud.set(playerRef, key, Message.raw(value));
        }
      }

    }

    @Nonnull
    private static String[] tokenize(@Nonnull String input) {
      String trimmed = input == null ? "" : input.trim();
      if (trimmed.isEmpty()) {
        return new String[0];
      }
      return trimmed.split("\\s+");
    }

    private static int skipCommandTokens(@Nonnull String[] tokens, @Nonnull String... commandPath) {
      int index = 0;
      for (String command : commandPath) {
        if (index < tokens.length && command.equalsIgnoreCase(tokens[index])) {
          index++;
        }
      }
      return index;
    }

    @Nullable
    private static PlayerRef requirePlayer(@Nonnull CommandContext context) {
      if (!context.isPlayer()) {
        context.sendMessage(Message.raw("This command can only be used by players."));
        return null;
      }
      java.util.UUID uuid = context.sender().getUuid();
      if (uuid == null) {
        context.sendMessage(Message.raw("Unable to resolve player."));
        return null;
      }
      PlayerRef ref = Universe.get().getPlayer(uuid);
      if (ref == null || !ref.isValid()) {
        context.sendMessage(Message.raw("Unable to resolve player."));
        return null;
      }
      return ref;
    }
  }
}
