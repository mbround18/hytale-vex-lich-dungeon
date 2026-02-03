package MBRound18.hytale.shared.interfaces.huds.demo;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosHudsDemohudpartystatusUi;

public class DemoHudPartyStatusHud extends AbstractCustomUIHud<DemosHudsDemohudpartystatusUi> {

  public DemoHudPartyStatusHud(
      @Nonnull CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef) {
    super(DemosHudsDemohudpartystatusUi.class, store, ref, playerRef);
  }
}
