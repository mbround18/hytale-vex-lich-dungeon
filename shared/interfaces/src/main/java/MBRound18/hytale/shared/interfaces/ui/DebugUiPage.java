package MBRound18.hytale.shared.interfaces.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import javax.annotation.Nonnull;

public final class DebugUiPage extends AbstractCustomUIPage {
  public DebugUiPage(@Nonnull PlayerRef playerRef, @Nonnull String uiPath,
      @Nonnull Map<String, String> vars) {
    super(playerRef, CustomPageLifetime.CanDismiss, uiPath, vars, null);
  }
}
