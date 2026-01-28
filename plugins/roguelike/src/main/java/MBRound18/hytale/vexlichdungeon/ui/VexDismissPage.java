package MBRound18.hytale.vexlichdungeon.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

/**
 * Invisible dismiss-only page to listen for ESC and cancel HUD sequences.
 */
public final class VexDismissPage extends BasicCustomUIPage {
  private final Runnable onDismiss;
  private final AtomicBoolean closing = new AtomicBoolean(false);

  public VexDismissPage(@Nonnull PlayerRef playerRef, @Nonnull Runnable onDismiss) {
    super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
    this.onDismiss = onDismiss;
  }

  @Override
  public void build(UICommandBuilder commands) {
    // Intentionally empty; HUD UI is handled separately.
  }

  @Override
  public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
    if (closing.get()) {
      return;
    }
    if (onDismiss != null) {
      onDismiss.run();
    }
  }

  public void requestClose() {
    if (closing.compareAndSet(false, true)) {
      close();
    }
  }
}
