package MBRound18.hytale.vexlichdungeon.portal;

import MBRound18.ImmortalEngine.api.events.WorldEnteredEvent;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import MBRound18.hytale.shared.utilities.PlayerPoller;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class PortalEntryEventHandler {
  private static final LoggingHelper log = new LoggingHelper(PortalEntryEventHandler.class);

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    Objects.requireNonNull(eventBus, "eventBus").register(
        (Class) WorldEnteredEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof WorldEnteredEvent event) {
            World world = event.getWorld();
            var playerRef = event.getPlayerRef();
            if (world == null || playerRef == null) {
              return;
            }
            final String targetWorldName = world.getName();
            final PlayerPoller poller = new PlayerPoller();
            final int maxAttempts = 25;
            final int[] attempts = new int[] { 0 };
            poller.start(playerRef, 200L, () -> {
              attempts[0]++;
              if (attempts[0] >= maxAttempts) {
                log.warn("Portal entry timed out for player %s in %s",
                    playerRef.getUsername(), targetWorldName);
                poller.stop();
                return;
              }
              if (!playerRef.isValid()) {
                poller.stop();
                return;
              }
              UUID worldId = playerRef.getWorldUuid();
              if (worldId == null) {
                return;
              }
              World currentWorld = Universe.get().getWorld(worldId);
              if (currentWorld == null || currentWorld.getName() == null) {
                return;
              }
              if (!currentWorld.getName().equals(targetWorldName)) {
                return;
              }
              Ref<EntityStore> ref = playerRef.getReference();
              if (ref == null || !ref.isValid()) {
                return;
              }
              Store<EntityStore> store = ref.getStore();
              if (store == null) {
                return;
              }
              Player player = store.getComponent(ref, Player.getComponentType());
              if (player == null) {
                return;
              }
              PortalManagerSystem.handlePortalEntry(playerRef, currentWorld);
              poller.stop();
            });
          }
        });
  }
}
