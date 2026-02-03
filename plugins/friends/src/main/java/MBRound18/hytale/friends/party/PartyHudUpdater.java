package MBRound18.hytale.friends.party;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.ImmortalEngine.api.participants.ParticipantSnapshot;
import MBRound18.ImmortalEngine.api.participants.ParticipantTracker;
import MBRound18.ImmortalEngine.api.social.PartyMemberSnapshot;
import MBRound18.ImmortalEngine.api.social.PartyService;
import MBRound18.ImmortalEngine.api.social.PartySnapshot;
import MBRound18.hytale.friends.ui.FriendsHudController;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
  private static final int MAX_MEMBER_SLOTS = 4;

  private final PartyService partyService;
  private final LoggingHelper log;
  private ScheduledFuture<?> task;

  public PartyHudUpdater(PartyService partyService, LoggingHelper log) {
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
      for (World world : Universe.get().getWorlds().values()) {
        world.execute(() -> updateWorld(world));
      }
    } catch (Exception e) {
      log.warn("[FRIENDS] Party HUD updater failed: %s", e.getMessage());
    }
  }

  private void updateWorld(World world) {
    Map<UUID, PlayerContext> online = new HashMap<>();
    ParticipantTracker.get().updateFromWorld(world);
    for (PlayerRef playerRef : world.getPlayerRefs()) {
      online.put(playerRef.getUuid(), new PlayerContext(playerRef, world));
    }

    Set<UUID> partyMembers = new HashSet<>();
    for (PartySnapshot party : partyService.getParties()) {
      Map<String, String> vars = buildPartyVars(party, online);
      for (PartyMemberSnapshot member : party.getMembers()) {
        partyMembers.add(member.getUuid());
        PlayerContext context = online.get(member.getUuid());
        if (context != null) {
          FriendsHudController.openPartyHud(context.playerRef, vars);
        }
      }
    }

    for (PlayerContext context : online.values()) {
      if (!partyMembers.contains(context.playerRef.getUuid())) {
        FriendsHudController.clearHud(context.playerRef);
      }
    }
  }

  private Map<String, String> buildPartyVars(PartySnapshot party, Map<UUID, PlayerContext> online) {
    Map<String, String> vars = new HashMap<>();
    String summary = buildPartyList(party, online);
    vars.put("FriendsPartyList", summary);

    int slot = 1;
    for (PartyMemberSnapshot member : party.getMembers()) {
      if (slot > MAX_MEMBER_SLOTS) {
        break;
      }
      String name = member.getName();
      PlayerContext context = online.get(member.getUuid());
      ParticipantSnapshot stats = null;
      if (context != null) {
        stats = ParticipantTracker.get().getParticipant(context.world.getName(), member.getUuid());
      }
      float health = stats == null ? -1f : stats.getHealth();
      float healthMax = stats == null ? -1f : stats.getHealthMax();
      float stamina = stats == null ? -1f : stats.getStamina();
      float staminaMax = stats == null ? -1f : stats.getStaminaMax();

      vars.put("Member" + slot + "Name", name);
      vars.put("Member" + slot + "Leader", member.isLeader() ? "LEAD" : "");
      vars.put("Member" + slot + "HpBar", buildBar("HP", health, healthMax));
      vars.put("Member" + slot + "StamBar", buildBar("ST", stamina, staminaMax));
      vars.put("Member" + slot + "Item", "");
      slot++;
    }

    for (int i = slot; i <= MAX_MEMBER_SLOTS; i++) {
      vars.put("Member" + i + "Name", "");
      vars.put("Member" + i + "Leader", "");
      vars.put("Member" + i + "HpBar", "HP [----------]");
      vars.put("Member" + i + "StamBar", "ST [----------]");
      vars.put("Member" + i + "Item", "");
    }

    return vars;
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

  private String buildBar(String label, float currentValue, float maxValue) {
    if (currentValue < 0f || maxValue <= 0f) {
      return label + " [??????????]";
    }
    float ratio = currentValue / maxValue;
    if (ratio < 0f) {
      ratio = 0f;
    } else if (ratio > 1f) {
      ratio = 1f;
    }
    int segments = 10;
    int filled = Math.round(ratio * segments);
    StringBuilder bar = new StringBuilder();
    for (int i = 0; i < segments; i++) {
      bar.append(i < filled ? "#" : "-");
    }
    return label + " [" + bar + "]";
  }

  private static final class PlayerContext {
    private final PlayerRef playerRef;
    private final World world;

    private PlayerContext(PlayerRef playerRef, World world) {
      this.playerRef = playerRef;
      this.world = world;
    }
  }
}
