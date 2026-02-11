package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexscorehudUi;
import MBRound18.hytale.shared.utilities.UiThread;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import MBRound18.hytale.shared.interfaces.util.UiMessage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VexScoreHud extends AbstractCustomUIHud<VexHudVexscorehudUi> {

  private enum DebugMode {
    SINGLE_FIELD,
    ALL
  }

  private static final DebugMode MODE = DebugMode.ALL;

  public VexScoreHud(@Nonnull PlayerRef playerRef) {
    super(VexHudVexscorehudUi.class, playerRef);
  }

  /**
   * Opens or updates the HUD.
   * Restored Store and Ref parameters to fix compilation error in
   * RoguelikeDungeonController.
   */
  public static void open(
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef,
      int instanceScore,
      int playerScore,
      int delta,
      @Nullable String partyList) {
    update(playerRef, instanceScore, playerScore, delta, partyList);
  }

  public static VexScoreHud open(@Nonnull PlayerRef playerRef) {
    return ensureActive(playerRef, VexScoreHud.class);
  }

  public static void update(@Nonnull PlayerRef playerRef, int instanceScore, int playerScore,
      int delta, @Nullable String partyList) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      VexScoreHud hud = VexScoreHud.open(playerRef);
      if (hud == null)
        return;

      if (MODE == DebugMode.ALL) {
        hud.setInstanceScore(playerRef, instanceScore);
        hud.setPlayerScore(playerRef, playerScore);
        hud.setDelta(playerRef, delta);
        hud.setPartyList(playerRef, partyList);
      } else {
        hud.setInstanceScore(playerRef, instanceScore);
      }
    });
  }

  public void setInstanceScore(@Nonnull PlayerRef playerRef, int instanceScore) {
    VexHudVexscorehudUi ui = getUiModel();
    if (ui == null)
      return;
    String total = HudTextSanitizer.formatLabeledValue("Total Score:", instanceScore);
    set(playerRef, ui.vexHudInstanceScore, UiMessage.raw(total));
  }

  public void setPlayerScore(@Nonnull PlayerRef playerRef, int playerScore) {
    VexHudVexscorehudUi ui = getUiModel();
    if (ui == null)
      return;
    String score = HudTextSanitizer.formatLabeledValue("Player Score:", playerScore);
    set(playerRef, ui.vexHudPlayerScore, UiMessage.raw(score));
  }

  public void setDelta(@Nonnull PlayerRef playerRef, int delta) {
    VexHudVexscorehudUi ui = getUiModel();
    if (ui == null)
      return;
    String value = HudTextSanitizer.formatDelta(delta);
    set(playerRef, ui.vexHudDelta, UiMessage.raw(value));
  }

  public void setPartyList(@Nonnull PlayerRef playerRef, @Nullable String partyList) {
    VexHudVexscorehudUi ui = getUiModel();
    if (ui == null)
      return;
    String listValue = HudTextSanitizer.sanitize(partyList);
    String list = HudTextSanitizer.sanitize("Party: %s".formatted(listValue));
    set(playerRef, ui.vexHudPartyList, UiMessage.raw(list));
  }

  public static void closeFor(@Nonnull PlayerRef playerRef) {
    UiThread.runOnPlayerWorld(playerRef, () -> {
      AbstractCustomUIHud.closeHud(playerRef, VexScoreHud.class, hud -> {
        // Cleanup logic if needed
      });
    });
  }
}
