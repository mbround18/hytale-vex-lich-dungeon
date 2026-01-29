package MBRound18.hytale.friends.party;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import MBRound18.ImmortalEngine.api.participants.ParticipantSnapshot;
import MBRound18.ImmortalEngine.api.participants.ParticipantTracker;
import MBRound18.ImmortalEngine.api.social.PartyMemberSnapshot;
import MBRound18.ImmortalEngine.api.social.PartyService;
import MBRound18.ImmortalEngine.api.social.PartySnapshot;
import MBRound18.hytale.friends.ui.FriendsHudController;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PartyHudUpdater implements Runnable {
  private static final long UPDATE_INTERVAL_MS = 1000L;

  private final PartyService partyService;
  private final EngineLog log;
  private ScheduledFuture<?> task;

  public PartyHudUpdater(PartyService partyService, EngineLog log) {
    this.partyService = partyService;
    this.log = log;
  }

  public void start() {
    stop();
    task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this, 250L, UPDATE_INTERVAL_MS,
        TimeUnit.MILLISECONDS);
  }

  public void stop() {
    if (task != null) {
      task.cancel(false);
      task = null;
    }
  }

  @Override
  public void run() {
    try {
      Map<UUID, PlayerContext> online = new HashMap<>();
      for (World world : Universe.get().getWorlds().values()) {
        ParticipantTracker.get().updateFromWorld(world);
        for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
          online.put(player.getUuid(), new PlayerContext(player, world));
        }
      }

      Set<UUID> partyMembers = new HashSet<>();
      for (PartySnapshot party : partyService.getParties()) {
        String listText = buildPartyList(party, online);
        for (PartyMemberSnapshot member : party.getMembers()) {
          partyMembers.add(member.getUuid());
          PlayerContext context = online.get(member.getUuid());
          if (context != null) {
            FriendsHudController.openPartyHud(context.player.getPlayerRef(), listText);
          }
        }
      }

      for (PlayerContext context : online.values()) {
        if (!partyMembers.contains(context.player.getUuid())) {
          FriendsHudController.clearHud(context.player.getPlayerRef());
        }
      }
    } catch (Exception e) {
      log.warn("[FRIENDS] Party HUD updater failed: %s", e.getMessage());
    }
  }

  private String buildPartyList(PartySnapshot party, Map<UUID, PlayerContext> online) {
    StringBuilder builder = new StringBuilder();
    for (PartyMemberSnapshot member : party.getMembers()) {
      if (builder.length() > 0) {
        builder.append("\n");
      }
      String name = member.getName();
      PlayerContext context = online.get(member.getUuid());
      ParticipantSnapshot stats = null;
      if (context != null) {
        stats = ParticipantTracker.get().getParticipant(context.world.getName(), member.getUuid());
      }
      String health = formatStat(stats == null ? -1f : stats.getHealth(),
          stats == null ? -1f : stats.getHealthMax());
      String stamina = formatStat(stats == null ? -1f : stats.getStamina(),
          stats == null ? -1f : stats.getStaminaMax());
      builder.append(name);
      if (member.isLeader()) {
        builder.append(" [LEAD]");
      }
      builder.append(" - HP ")
          .append(health)
          .append(" | ST ")
          .append(stamina);
    }
    return builder.toString();
  }

  private String formatStat(float currentValue, float maxValue) {
    if (currentValue < 0f) {
      return "?";
    }
    int current = Math.max(0, Math.round(currentValue));
    int max = Math.max(0, Math.round(maxValue));
    if (max <= 0 && current > 0) {
      return String.valueOf(current);
    }
    if (max <= 0) {
      return "?";
    }
    return current + "/" + max;
  }

  private static final class PlayerContext {
    private final com.hypixel.hytale.server.core.entity.entities.Player player;
    private final World world;

    private PlayerContext(com.hypixel.hytale.server.core.entity.entities.Player player, World world) {
      this.player = player;
      this.world = world;
    }
  }
}
