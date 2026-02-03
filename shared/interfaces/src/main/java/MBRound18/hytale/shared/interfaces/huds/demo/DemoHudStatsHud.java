package MBRound18.hytale.shared.interfaces.huds.demo;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ComponentType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.UiThread;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosHudsDemohudstatsUi;
import MBRound18.hytale.shared.interfaces.ui.generated.ServerLang;
import MBRound18.hytale.shared.utilities.LoggingHelper;

public class DemoHudStatsHud extends AbstractCustomUIHud {
  private static final DemosHudsDemohudstatsUi UI = new DemosHudsDemohudstatsUi();

  private static final String UI_PATH = DemosHudsDemohudstatsUi.UI_PATH;
  private static final ScheduledExecutorService STATS_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "demo-hud-stats");
    thread.setDaemon(true);
    return thread;
  });
  private final LoggingHelper log;
  private final PlayerRef currentPlayerRef;
  private ScheduledFuture<?> statsTask;

  public DemoHudStatsHud(
      @Nonnull CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef) {
    super(UI_PATH, context, store, ref, playerRef);
    this.currentPlayerRef = playerRef;
    this.log = new LoggingHelper(DemoHudStatsHud.class);
  }

  public void updateStats(
      @Nonnull PlayerRef playerRef,
      String health,
      String defensePercent,
      String stamina) {
    List<Map.Entry<String, String>> entries = List.of(
        Map.entry(UI.demoHudStatsStatsRowHealthHealthValue, health),
        Map.entry(UI.demoHudStatsStatsRowArmorArmorValue, defensePercent),
        Map.entry(UI.demoHudStatsStatsRowManaManaValue, stamina));
    for (Map.Entry<String, String> entry : entries) {
      set(playerRef, entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void run() {
    applyLabelRefs();
    startPolling();
  }

  private void applyLabelRefs() {
    @Nonnull
    PlayerRef playerRef = Objects.requireNonNull(this.currentPlayerRef, "currentPlayerRef");

    List<Map.Entry<String, Message>> entries = List.of(
        Map.entry(UI.demoHudStatsHeaderHeaderText,
            ServerLang.customUIDemoHudStatsHealthLabel),
        Map.entry(UI.demoHudStatsStatsRowHealthHealthLabel,
            ServerLang.customUIDemoHudStatsHealthLabel),
        Map.entry(UI.demoHudStatsStatsRowArmorArmorLabel, ServerLang.customUIDemoHudStatsDefenseLabel),
        Map.entry(UI.demoHudStatsStatsRowManaManaLabel, ServerLang.customUIDemoHudStatsStaminaLabel),
        Map.entry(UI.demoHudStatsZoneInfoLabel, ServerLang.customUIDemoHudStatsZoneLabel),
        Map.entry(UI.demoHudStatsZoneInfoValue, ServerLang.customUIDemoHudStatsZoneValue));

    log.info("Applying label refs to demo stats HUD");

    for (Map.Entry<String, Message> entry : entries) {
      set(playerRef,
          Objects.requireNonNull(entry.getKey(), "entry key"),
          Objects.requireNonNull(entry.getValue(), "entry value"));
    }
  }

  private synchronized void startPolling() {
    if (statsTask != null && !statsTask.isCancelled()) {
      return;
    }
    statsTask = STATS_SCHEDULER.scheduleAtFixedRate(() -> {
      if (!currentPlayerRef.isValid()) {
        stopPolling();
        return;
      }
      watchPlayerStats();
    }, 0L, 10L, TimeUnit.MILLISECONDS);
  }

  private synchronized void stopPolling() {
    if (statsTask != null) {
      statsTask.cancel(false);
      statsTask = null;
    }
  }

  public void watchPlayerStats() {
    PlayerRef playerRef = this.currentPlayerRef;
    UiThread.runOnPlayerWorld(playerRef, () -> {
      if (!playerRef.isValid()) {
        stopPolling();
        return;
      }
      Ref<EntityStore> ref = playerRef.getReference();
      if (ref == null || !ref.isValid()) {
        stopPolling();
        return;

      }
      Ref<EntityStore> safeRef = Objects.requireNonNull(ref, "ref");
      Store<EntityStore> store = safeRef.getStore();
      var player = store.getComponent(safeRef,
          com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
      if (player == null) {
        stopPolling();
        return;
      }
      var hudManager = player.getHudManager();
      if (hudManager == null || hudManager.getCustomHud() != this) {
        stopPolling();
        return;
      }
      EntityStatsModule statsModule = EntityStatsModule.get();
      ComponentType<EntityStore, EntityStatMap> componentType = statsModule != null
          ? statsModule.getEntityStatMapComponentType()
          : EntityStatMap.getComponentType();
      componentType = Objects.requireNonNull(componentType, "componentType");
      EntityStatMap statMap = store.getComponent(safeRef, componentType);
      if (statMap == null) {
        return;
      }
      EntityStatMap safeStatMap = Objects.requireNonNull(statMap, "statMap");
      int health = Math.round(getHealth(safeStatMap));
      int stamina = Math.round(readStat(safeStatMap, DefaultEntityStatTypes.getStamina()));
      String defensePercent = "--";

      log.debug("Updating stats HUD (health=" + health
          + ", stamina=" + stamina + ", defense=" + defensePercent + ")");
      updateStats(
          Objects.requireNonNull(playerRef, "playerRef"),
          Objects.requireNonNull(String.valueOf(health), "health"),
          Objects.requireNonNull(defensePercent, "defensePercent"),
          Objects.requireNonNull(String.valueOf(stamina), "stamina"));

    });
  }

  private float getHealth(@Nonnull EntityStatMap statMap) {
    return readStat(statMap, DefaultEntityStatTypes.getHealth());
  }

  private float readStat(@Nonnull EntityStatMap statMap, int statId) {
    EntityStatValue stat = statMap.get(statId);
    return stat != null ? stat.get() : -1f;
  }
}
