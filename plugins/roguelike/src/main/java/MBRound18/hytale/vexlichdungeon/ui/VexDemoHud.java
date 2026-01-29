package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.vexlichdungeon.ui.core.AbstractCustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class VexDemoHud extends AbstractCustomUIHud {
  // IMPORTANT: this should be a client-loadable UI path (usually under ui/)
  private static final String HUD_PATH = "Vex/Hud/VexDemoHud.ui";

  private int score;
  private int timeRemaining;

  public VexDemoHud(@Nonnull PlayerRef playerRef, int score, int timeRemaining) {
    super(HUD_PATH, playerRef);
    this.score = score;
    this.timeRemaining = timeRemaining;
  }

  public void updateScore(int score) {
    this.score = score;
    set("#DemoScore.TextSpans", "Score: " + score);
  }

  public int getScore() {
    return score;
  }

  public void updateTimeRemaining(int seconds) {
    this.timeRemaining = seconds;
    set("#DemoTimer.TextSpans", "Time: " + formatTime(seconds));
  }

  public int getTimeRemaining() {
    return timeRemaining;
  }
}
