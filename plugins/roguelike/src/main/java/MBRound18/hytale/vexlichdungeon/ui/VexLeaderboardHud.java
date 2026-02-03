package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIPage;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexleaderboardhudUi;
import MBRound18.hytale.shared.utilities.UiThread;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public final class VexLeaderboardHud extends AbstractCustomUIHud<VexHudVexleaderboardhudUi> {
  private VexLeaderboardHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexleaderboardhudUi.class, playerRef);
  }

  public static void open(@Nonnull PlayerRef playerRef, @Nonnull String leaderboardText) {
    VexLeaderboardHud hud = new VexLeaderboardHud(playerRef);
    Map<String, String> vars = new HashMap<>();
    VexHudVexleaderboardhudUi ui = hud.getUiModel();
    vars.put(ui.vexContentVexLeaderboardBody, leaderboardText);
    openHud(playerRef, hud, vars);
  }

  private static void openHud(@Nonnull PlayerRef playerRef, @Nonnull VexLeaderboardHud hud,
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
      if (hudManager == null) {
        return;
      }
      hudManager.setCustomHud(playerRef, hud);
      if (!vars.isEmpty()) {
        UICommandBuilder builder = new UICommandBuilder();
        AbstractCustomUIPage.applyInitialState(builder, vars);
        hud.update(false, builder);
      }
    });
  }
}
