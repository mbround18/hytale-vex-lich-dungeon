package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexscorehudUi;
import MBRound18.hytale.shared.utilities.UiThread;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class VexScoreHud extends AbstractCustomUIHud<VexHudVexscorehudUi> {
  public VexScoreHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexscorehudUi.class, playerRef);
  }

  public static void open(
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef,
      int instanceScore, int playerScore,
      int delta, @Nonnull String partyList) {
    update(playerRef, instanceScore, playerScore, delta, partyList);
  }

  public static VexScoreHud open(@Nonnull PlayerRef playerRef) {
    return ensureActive(playerRef, VexScoreHud.class);
  }

  public static void update(@Nonnull PlayerRef playerRef, int instanceScore, int playerScore,
      int delta, @Nonnull String partyList) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      VexScoreHud hud = VexScoreHud.open(playerRef);
      if (hud == null) {
        return;
      }
      hud.setInstanceScore(playerRef, instanceScore);
      hud.setPlayerScore(playerRef, playerScore);
      hud.setDelta(playerRef, delta);
      hud.setPartyList(playerRef, partyList);
    });
  }

  public void setInstanceScore(@Nonnull PlayerRef playerRef, int instanceScore) {
    VexHudVexscorehudUi ui = getUiModel();
    if (ui == null) {
      return;
    }
    String value = "Instance: " + instanceScore;
    set(playerRef, ui.vexHudInstanceScore, Message.raw(value));
  }

  public void setPlayerScore(@Nonnull PlayerRef playerRef, int playerScore) {
    VexHudVexscorehudUi ui = getUiModel();
    if (ui == null) {
      return;
    }
    String value = "Player: " + playerScore;
    set(playerRef, ui.vexHudPlayerScore, Message.raw(value));
  }

  public void setDelta(@Nonnull PlayerRef playerRef, int delta) {
    VexHudVexscorehudUi ui = getUiModel();
    if (ui == null) {
      return;
    }
    String value = (delta >= 0 ? "+" : "") + delta;
    set(playerRef, ui.vexHudDelta, Message.raw(value));
  }

  public void setPartyList(@Nonnull PlayerRef playerRef, @Nonnull String partyList) {
    VexHudVexscorehudUi ui = getUiModel();
    if (ui == null) {
      return;
    }
    String value = partyList != null ? partyList : "---";
    if (value.isBlank()) {
      value = "---";
    }
    set(playerRef, ui.vexHudPartyList, Message.raw(value));
  }

  public static void closeFor(@Nonnull PlayerRef playerRef) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      AbstractCustomUIHud.closeHud(playerRef, VexScoreHud.class, hud -> {
        // Score HUD closed safely
      });
    });
  }
}
