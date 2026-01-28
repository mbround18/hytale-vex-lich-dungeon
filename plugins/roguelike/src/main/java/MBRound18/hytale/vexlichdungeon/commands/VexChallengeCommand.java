package MBRound18.hytale.vexlichdungeon.commands;

import MBRound18.PortalEngine.api.logging.EngineLog;
import MBRound18.PortalEngine.api.i18n.EngineLang;
import MBRound18.hytale.vexlichdungeon.data.DataStore;
import MBRound18.PortalEngine.api.portal.PortalBlockResolver;
import MBRound18.PortalEngine.api.portal.PortalPlacementConfig;
import MBRound18.PortalEngine.api.portal.PortalPlacementFailure;
import MBRound18.PortalEngine.api.portal.PortalPlacementRegistry;
import MBRound18.PortalEngine.api.portal.PortalPlacementResult;
import MBRound18.PortalEngine.api.portal.PortalPlacementService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Vector3i;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command to create and join a Vex Lich Dungeon challenge instance.
 * Usage: /vex challenge
 * 
 * This command:
 * 1. Checks if the player has permission (same as /inst command -
 * hytale.instances.create)
 * 2. Executes "/inst Vex_The_Lich_Dungeon" to create a new instance
 * 3. The instance system teleports the player automatically
 * 4. Dungeon generation will automatically trigger via event handlers once
 * instance is created
 */
public class VexChallengeCommand extends AbstractAsyncCommand {

  private static final String WORLD_NAME = "Vex_The_Lich_Dungeon";
  private static final String[] PORTAL_BLOCK_CANDIDATES = new String[] {
      "Vex_Dungeon_Challenge_Enter",
      "Items/Portal/Vex/Vex_Dungeon_Challenge_Enter",
      "Portal_Device",
      "PortalDevice",
      "Portal_Device_Block",
      "Portal_Device_Base",
      "Portal_Device_Off",
      "Portal_Device_On"
  };
  private static final long PORTAL_TTL_MS = 30_000L;

  private final EngineLog log;
  private final DataStore dataStore;

  public VexChallengeCommand(@Nonnull EngineLog log, @Nonnull DataStore dataStore) {
    super("challenge", "Create and join a Vex Lich Dungeon challenge");
    this.log = log;
    this.dataStore = dataStore;
  }

  @Override
  protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
    CommandSender sender = context.sender();

    // Ensure command is executed by a player
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.onlyPlayers")));
      return CompletableFuture.completedFuture(null);
    }

    try {
      log.info("[COMMAND] Player %s executed /vex challenge", sender.getDisplayName());

      PlayerContext playerContext = findPlayerContext(sender.getUuid());
      if (playerContext == null) {
        context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.playerMissing")));
        return CompletableFuture.completedFuture(null);
      }

      playerContext.world.execute(() -> {
        PortalPlacementService placementService = new PortalPlacementService(
            PortalPlacementConfig.defaults(),
            new PortalBlockResolver(List.of(PORTAL_BLOCK_CANDIDATES)));
        PortalPlacementResult placement = placementService.placePortal(
            playerContext.world,
            playerContext.transform.getPosition(),
            playerContext.transform.getRotation(),
            log);
        if (!placement.isPlaced()) {
          log.warn("[PORTAL] Challenge portal placement failed: %s (%s)",
              placement.getFailure(),
              placement.getDetail() != null ? placement.getDetail() : "no detail");
          if (placement.getFailure() == PortalPlacementFailure.NO_SAFE_SPOT) {
            context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.noSpace")));
          } else {
            context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.placeFailed")));
          }
          return;
        }
        Vector3i pos = placement.getPosition();
        if (pos == null) {
          log.warn("[PORTAL] Challenge portal placement returned null position despite success.");
          context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.placeFailed")));
          return;
        }
        int maxPlayers = dataStore.getConfig().getMaxPlayersPerInstance();
        long expiresAt = System.currentTimeMillis() + PORTAL_TTL_MS;
        PortalPlacementRegistry.register(WORLD_NAME, playerContext.world.getName(), pos, expiresAt, maxPlayers);
        context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.portalPlaced")));
        context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.portalHint", WORLD_NAME)));
      });

      log.info("[COMMAND] Completed /vex challenge for player %s", sender.getDisplayName());

    } catch (Exception e) {
      log.error("Failed to execute vex command: %s", e.getMessage());
      e.printStackTrace();
      context.sendMessage(Message.raw(EngineLang.t("command.vex.challenge.failed", e.getMessage())));
    }

    return CompletableFuture.completedFuture(null);
  }

  @Nullable
  @SuppressWarnings("removal")
  private PlayerContext findPlayerContext(@Nonnull UUID uuid) {
    for (World world : Universe.get().getWorlds().values()) {
      for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
        if (!uuid.equals(player.getUuid())) {
          continue;
        }
        TransformComponent transform = player.getTransformComponent();
        if (transform == null) {
          return null;
        }
        return new PlayerContext(world, transform);
      }
    }
    return null;
  }

  private static final class PlayerContext {
    private final World world;
    private final TransformComponent transform;

    private PlayerContext(World world, TransformComponent transform) {
      this.world = world;
      this.transform = transform;
    }
  }
}
