package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexscorehudUi;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.shared.utilities.UiThread;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class VexScoreHud extends AbstractCustomUIHud<VexHudVexscorehudUi> {
  private VexScoreHud(
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef

  ) {
    super(VexHudVexscorehudUi.class, store, ref, playerRef);
  }

  public static void open(
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef,
      int instanceScore, int playerScore,
      int delta, @Nonnull String partyList) {
    VexScoreHud hud = new VexScoreHud(store, ref, playerRef);
    Map<String, String> vars = new HashMap<>();
    VexHudVexscorehudUi ui = hud.getUiModel();
    vars.put(ui.vexHudInstanceScore, "Instance: " + instanceScore);
    vars.put(ui.vexHudPlayerScore, "Player: " + playerScore);
    vars.put(ui.vexHudDelta, (delta >= 0 ? "+" : "") + delta);
    vars.put(ui.vexHudPartyList, Objects.requireNonNull(partyList, "partyList"));
    openHud(playerRef, hud, vars);
  }

  private static void openHud(@Nonnull PlayerRef playerRef, @Nonnull VexScoreHud hud,
      @Nonnull Map<String, String> vars) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      Ref<EntityStore> ref = playerRef.getReference();
      if (ref == null || !ref.isValid()) {
        return;
      }
      Store<EntityStore> store = ref.getStore();
      Player player = store.getComponent(ref, Player.getComponentType());
      if (player == null) {
        return;
      }
      HudManager hudManager = player.getHudManager();
      if (!hud.isActiveHud(playerRef)) {
        hudManager.setCustomHud(playerRef, hud);
      }
      if (!vars.isEmpty()) {
        for (Map.Entry<String, String> entry : vars.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          LoggingHelper logger = new LoggingHelper(VexScoreHud.class);

          logger.info("Setting VexScoreHud key '{}' to value '{}'", key, value);

          if (key == null || value == null) {
            continue;
          }

          hud.set(playerRef, key, Message.raw(value));

        }
      }
    });
  }
}
