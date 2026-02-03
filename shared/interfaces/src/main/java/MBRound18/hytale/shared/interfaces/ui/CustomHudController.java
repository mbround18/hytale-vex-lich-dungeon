package MBRound18.hytale.shared.interfaces.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public class CustomHudController extends AbstractCustomUIHud<Object> {
  public CustomHudController(@Nonnull String hudPath, @Nonnull PlayerRef playerRef) {
    super(hudPath, playerRef);
  }
}
