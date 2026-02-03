package MBRound18.hytale.shared.interfaces.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class DebugHud extends AbstractCustomUIHud {
  public DebugHud(
      @Nonnull CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef,
      @Nonnull String uiPath) {
    super(uiPath, context, store, ref, playerRef);
  }
}
