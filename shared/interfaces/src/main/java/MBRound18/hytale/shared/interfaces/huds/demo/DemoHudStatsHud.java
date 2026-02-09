package MBRound18.hytale.shared.interfaces.huds.demo;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
import javax.annotation.Nonnull;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosHudsDemohudstatsUi;
import MBRound18.hytale.shared.interfaces.ui.generated.ServerLang;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.shared.utilities.PlayerPoller;

public class DemoHudStatsHud extends AbstractCustomUIHud<DemosHudsDemohudstatsUi> {
  private final PlayerPoller playerPoller = new PlayerPoller();
  private final LoggingHelper log;
  private final PlayerRef currentPlayerRef;
  // private ScheduledFuture<?> statsTask;

  public DemoHudStatsHud(
      @Nonnull CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef) {
    super(DemosHudsDemohudstatsUi.class, store, ref, playerRef);
    this.currentPlayerRef = playerRef;
    this.log = new LoggingHelper(DemoHudStatsHud.class);
  }

  public void updateStats(
      @Nonnull PlayerRef playerRef,
      String health,
      String defensePercent,
      String stamina) {
    DemosHudsDemohudstatsUi UI = getUiModel();
    if (UI == null) {
      return;
    }
    // set(playerRef, UI.customUIDemoHudStatsStaminaLabel, stamina);
    set(playerRef, UI.demoHudStatsStatsRowHealthHealthValue, Message.raw(health != null ? health : "--"));
    set(playerRef, UI.demoHudStatsStatsRowArmorArmorValue, Message.raw(defensePercent != null ? defensePercent : "--"));
    set(playerRef, UI.demoHudStatsStatsRowManaManaValue, Message.raw(stamina != null ? stamina : "--"));

  }

  public void run() {
    applyLabelRefs();
    playerPoller.start(currentPlayerRef, 200L, this::watchPlayerStats);
  }

  private void applyLabelRefs() {
    @Nonnull
    PlayerRef playerRef = Objects.requireNonNull(this.currentPlayerRef, "currentPlayerRef");
    if (!isActiveHud(playerRef)) {
      return;
    }
    DemosHudsDemohudstatsUi UI = getUiModel();
    if (UI == null) {
      return;
    }
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

  public void watchPlayerStats() {
    PlayerRef playerRef = Objects.requireNonNull(this.currentPlayerRef, "currentPlayerRef");
    Player player = validateAndGetPlayer();
    if (player == null) {
      playerPoller.stop();
      return;
    }
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      playerPoller.stop();
      return;
    }
    Store<EntityStore> store = ref.getStore();
    EntityStatsModule statsModule = EntityStatsModule.get();
    ComponentType<EntityStore, EntityStatMap> componentType = statsModule != null
        ? statsModule.getEntityStatMapComponentType()
        : EntityStatMap.getComponentType();
    if (componentType == null) {
      // Just skip this update frame instead of throwing/crashing
      // The stats module might not be loaded yet.
      return;
    }
    EntityStatMap statMap = store.getComponent(ref, componentType);
    if (statMap == null) {
      // Just skip this update frame instead of throwing/crashing
      // The player might be respawning or loading.
      return;
    }
    EntityStatMap safeStatMap = Objects.requireNonNull(statMap, "statMap");
    int health = Math.round(getHealth(safeStatMap));
    int stamina = Math.round(readStat(safeStatMap, DefaultEntityStatTypes.getStamina()));
    String defensePercent = "--";
    try {
      updateStats(
          Objects.requireNonNull(playerRef, "playerRef"),
          String.valueOf(health),
          defensePercent,
          String.valueOf(stamina));
    } catch (Exception e) {
      log.error("Failed to update UI. Check your .ui file IDs! " + e.getMessage());
      playerPoller.stop(); // Stop to prevent spamming errors
    }
  }

  public void onClear() {
    playerPoller.stop();
  }

  private float getHealth(@Nonnull EntityStatMap statMap) {
    return readStat(statMap, DefaultEntityStatTypes.getHealth());
  }

  private float readStat(@Nonnull EntityStatMap statMap, int statId) {
    EntityStatValue stat = statMap.get(statId);
    return stat != null ? stat.get() : -1f;
  }
}
