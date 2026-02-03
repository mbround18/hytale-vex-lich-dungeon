package MBRound18.hytale.shared.interfaces.ui;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemoinputsUi;

public final class DebugHud extends AbstractCustomUIHud<DemosPagesDemoinputsUi> {
  public DebugHud(
      @Nonnull CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef) {
    super(DemosPagesDemoinputsUi.class, store, ref, playerRef);
  }
}
