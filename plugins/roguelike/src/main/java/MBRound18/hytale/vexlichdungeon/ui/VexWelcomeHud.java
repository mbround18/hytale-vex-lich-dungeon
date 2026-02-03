package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIPage;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexwelcomehudUi;
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

public final class VexWelcomeHud extends AbstractCustomUIHud<VexHudVexwelcomehudUi> {
  private VexWelcomeHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexwelcomehudUi.class, playerRef);
  }

  public static void open(@Nonnull PlayerRef playerRef, @Nonnull String bodyText) {
    VexWelcomeHud hud = new VexWelcomeHud(playerRef);
    Map<String, String> vars = new HashMap<>();
    VexHudVexwelcomehudUi ui = hud.getUiModel();
    vars.put(ui.vexContentVexWelcomeBody, bodyText);
    openHud(playerRef, hud, vars);
  }

  private static void openHud(@Nonnull PlayerRef playerRef, @Nonnull VexWelcomeHud hud,
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
