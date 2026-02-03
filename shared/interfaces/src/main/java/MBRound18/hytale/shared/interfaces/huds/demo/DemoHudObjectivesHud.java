package MBRound18.hytale.shared.interfaces.huds.demo;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

import MBRound18.hytale.shared.interfaces.abstracts.AbstractCustomUIHud;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosHudsDemohudobjectivesUi;

public class DemoHudObjectivesHud extends AbstractCustomUIHud {
  private static final String UI_PATH = DemosHudsDemohudobjectivesUi.UI_PATH;

  public DemoHudObjectivesHud(
      @Nonnull CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef playerRef) {
    super(UI_PATH, context, store, ref, playerRef);
  }
}
