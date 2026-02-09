package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.events.PortalClosedEvent;
import MBRound18.ImmortalEngine.api.events.PortalCreatedEvent;
import MBRound18.hytale.shared.utilities.LoggingHelper;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VexLifecycleEventHandler {
  private final LoggingHelper log;

  public VexLifecycleEventHandler(@Nonnull LoggingHelper log) {
    this.log = Objects.requireNonNull(log, "log");
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void register(@Nonnull EventBus eventBus) {
    eventBus.register((Class) PortalCreatedEvent.class,
        (java.util.function.Consumer) (Object e) -> onPortalCreated((PortalCreatedEvent) e));
    eventBus.register((Class) PortalClosedEvent.class,
        (java.util.function.Consumer) (Object e) -> onPortalClosed((PortalClosedEvent) e));

    eventBus.register((Class) InstanceCreatedEvent.class,
        (java.util.function.Consumer) (Object e) -> onInstanceCreated((InstanceCreatedEvent) e));
    eventBus.register((Class) InstanceEnteredEvent.class,
        (java.util.function.Consumer) (Object e) -> onInstanceEntered((InstanceEnteredEvent) e));
    eventBus.register((Class) InstanceExitedEvent.class,
        (java.util.function.Consumer) (Object e) -> onInstanceExited((InstanceExitedEvent) e));

    eventBus.register((Class) RoomEnteredEvent.class,
        (java.util.function.Consumer) (Object e) -> onRoomEntered((RoomEnteredEvent) e));
    eventBus.register((Class) RoomGeneratedEvent.class,
        (java.util.function.Consumer) (Object e) -> onRoomGenerated((RoomGeneratedEvent) e));
    eventBus.register((Class) EntityEliminatedEvent.class,
        (java.util.function.Consumer) (Object e) -> onEntityEliminated((EntityEliminatedEvent) e));
    eventBus.register((Class) EntitySpawnedEvent.class,
        (java.util.function.Consumer) (Object e) -> onEntitySpawned((EntitySpawnedEvent) e));
    eventBus.register((Class) RoomClearedEvent.class,
        (java.util.function.Consumer) (Object e) -> onRoomCleared((RoomClearedEvent) e));
    eventBus.register((Class) SafeRoomNeededEvent.class,
        (java.util.function.Consumer) (Object e) -> onSafeRoomNeeded((SafeRoomNeededEvent) e));
    eventBus.register((Class) ReturnPortalSpawnedEvent.class,
        (java.util.function.Consumer) (Object e) -> onReturnPortalSpawned((ReturnPortalSpawnedEvent) e));
    eventBus.register((Class) ReturnPortalRemovedEvent.class,
        (java.util.function.Consumer) (Object e) -> onReturnPortalRemoved((ReturnPortalRemovedEvent) e));
    eventBus.register((Class) PortalEnteredEvent.class,
        (java.util.function.Consumer) (Object e) -> onPortalEntered((PortalEnteredEvent) e));
    eventBus.register((Class) LootRolledEvent.class,
        (java.util.function.Consumer) (Object e) -> onLootRolled((LootRolledEvent) e));
    eventBus.register((Class) ChestSpawnedEvent.class,
        (java.util.function.Consumer) (Object e) -> onChestSpawned((ChestSpawnedEvent) e));
    eventBus.register((Class) InstanceInitializedEvent.class,
        (java.util.function.Consumer) (Object e) -> onInstanceInitialized((InstanceInitializedEvent) e));
    eventBus.register((Class) InstanceTeardownStartedEvent.class,
        (java.util.function.Consumer) (Object e) -> onInstanceTeardownStarted((InstanceTeardownStartedEvent) e));
    eventBus.register((Class) InstanceTeardownCompletedEvent.class,
        (java.util.function.Consumer) (Object e) -> onInstanceTeardownCompleted((InstanceTeardownCompletedEvent) e));
    eventBus.register((Class) PlayerJoinedServerEvent.class,
        (java.util.function.Consumer) (Object e) -> onPlayerJoinedServer((PlayerJoinedServerEvent) e));
    eventBus.register((Class) VexChallengeCommandEvent.class,
        (java.util.function.Consumer) (Object e) -> onVexChallengeCommand((VexChallengeCommandEvent) e));
    eventBus.register((Class) PrefabEntitySpawnedEvent.class,
        (java.util.function.Consumer) (Object e) -> onPrefabEntitySpawned((PrefabEntitySpawnedEvent) e));
    eventBus.register((Class) InstanceCapacityReachedEvent.class,
        (java.util.function.Consumer) (Object e) -> onInstanceCapacityReached((InstanceCapacityReachedEvent) e));
    eventBus.register((Class) PortalCloseRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> onPortalCloseRequested((PortalCloseRequestedEvent) e));
    eventBus.register((Class) PortalOwnerRegisteredEvent.class,
        (java.util.function.Consumer) (Object e) -> onPortalOwnerRegistered((PortalOwnerRegisteredEvent) e));
    eventBus.register((Class) CountdownHudClearRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> onCountdownHudClearRequested((CountdownHudClearRequestedEvent) e));
    eventBus.register((Class) VexWelcomeHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> onVexWelcomeHudRequested((VexWelcomeHudRequestedEvent) e));
    eventBus.register((Class) VexScoreHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> onVexScoreHudRequested((VexScoreHudRequestedEvent) e));
    eventBus.register((Class) VexSummaryHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> onVexSummaryHudRequested((VexSummaryHudRequestedEvent) e));
    eventBus.register((Class) VexLeaderboardHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> onVexLeaderboardHudRequested((VexLeaderboardHudRequestedEvent) e));
    eventBus.register((Class) VexDemoHudRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> onVexDemoHudRequested((VexDemoHudRequestedEvent) e));
    eventBus.register((Class) PortalCountdownHudUpdateRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> onPortalCountdownHudUpdateRequested(
            (PortalCountdownHudUpdateRequestedEvent) e));
  }

  private void onPortalCreated(@Nullable PortalCreatedEvent event) {
    if (event == null) {
      return;
    }
    World world = event.getWorld();
    log.info("[EVENT] PortalCreated portal=%s world=%s at=(%d,%d,%d) expiresAt=%d",
        event.getPortalId(), world.getName(), event.getPlacement().x,
        event.getPlacement().y, event.getPlacement().z, event.getExpiresAt());
  }

  private void onPortalClosed(@Nullable PortalClosedEvent event) {
    if (event == null) {
      return;
    }
    World world = event.getWorld();
    log.info("[EVENT] PortalClosed portal=%s world=%s", event.getPortalId(),
        world != null ? world.getName() : "<null>");
  }

  private void onInstanceCreated(@Nullable InstanceCreatedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] InstanceCreated world=%s", event.getWorld().getName());
  }

  private void onInstanceEntered(@Nullable InstanceEnteredEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] InstanceEntered world=%s player=%s", event.getWorld().getName(),
        formatPlayer(event.getPlayerRef()));
  }

  private void onInstanceExited(@Nullable InstanceExitedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] InstanceExited world=%s player=%s", event.getWorld().getName(),
        formatPlayer(event.getPlayerRef()));
  }

  private void onRoomEntered(@Nullable RoomEnteredEvent event) {
    if (event == null) {
      return;
    }
    RoomCoordinate room = event.getRoom();
    RoomCoordinate prev = event.getPreviousRoom();
    log.info("[EVENT] RoomEntered world=%s player=%s room=(%d,%d) prev=(%s)",
        event.getWorld().getName(), formatPlayer(event.getPlayerRef()),
        room.getX(), room.getZ(), prev == null ? "none" : prev.getX() + "," + prev.getZ());
  }

  private void onRoomGenerated(@Nullable RoomGeneratedEvent event) {
    if (event == null) {
      return;
    }
    RoomCoordinate room = event.getRoom();
    log.info("[EVENT] RoomGenerated world=%s room=(%d,%d) prefab=%s",
        event.getWorld().getName(), room.getX(), room.getZ(), event.getPrefabPath());
  }

  private void onEntityEliminated(@Nullable EntityEliminatedEvent event) {
    if (event == null) {
      return;
    }
    RoomCoordinate room = event.getRoom();
    log.info("[EVENT] EntityEliminated world=%s entity=%s killer=%s points=%d room=(%d,%d)",
        event.getWorld().getName(), event.getEntityId(), event.getKillerId(), event.getPoints(),
        room.getX(), room.getZ());
  }

  private void onEntitySpawned(@Nullable EntitySpawnedEvent event) {
    if (event == null) {
      return;
    }
    RoomCoordinate room = event.getRoom();
    log.info("[EVENT] EntitySpawned world=%s entity=%s type=%s points=%d room=(%d,%d)",
        event.getWorld().getName(), event.getEntityId(), event.getEntityType(), event.getPoints(),
        room.getX(), room.getZ());
  }

  private void onRoomCleared(@Nullable RoomClearedEvent event) {
    if (event == null) {
      return;
    }
    RoomCoordinate room = event.getRoom();
    log.info("[EVENT] RoomCleared world=%s room=(%d,%d)", event.getWorld().getName(), room.getX(), room.getZ());
  }

  private void onSafeRoomNeeded(@Nullable SafeRoomNeededEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] SafeRoomNeeded world=%s roomsSinceEvent=%d interval=%d",
        event.getWorld().getName(), event.getRoomsSinceEvent(), event.getEventInterval());
  }

  private void onReturnPortalSpawned(@Nullable ReturnPortalSpawnedEvent event) {
    if (event == null) {
      return;
    }
    RoomCoordinate room = event.getRoom();
    log.info("[EVENT] ReturnPortalSpawned world=%s room=(%d,%d) pos=(%d,%d,%d)",
        event.getWorld().getName(), room.getX(), room.getZ(),
        event.getPosition().x, event.getPosition().y, event.getPosition().z);
  }

  private void onReturnPortalRemoved(@Nullable ReturnPortalRemovedEvent event) {
    if (event == null) {
      return;
    }
    RoomCoordinate room = event.getRoom();
    log.info("[EVENT] ReturnPortalRemoved world=%s room=(%d,%d) pos=(%d,%d,%d)",
        event.getWorld().getName(), room.getX(), room.getZ(),
        event.getPosition().x, event.getPosition().y, event.getPosition().z);
  }

  private void onPortalEntered(@Nullable PortalEnteredEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] PortalEntered world=%s player=%s portal=%s",
        event.getWorld().getName(), formatPlayer(event.getPlayerRef()), event.getPortalId());
  }

  private void onLootRolled(@Nullable LootRolledEvent event) {
    if (event == null) {
      return;
    }
    RoomCoordinate room = event.getRoom();
    log.info("[EVENT] LootRolled world=%s room=(%d,%d) item=%s x%d",
        event.getWorld().getName(), room.getX(), room.getZ(), event.getItemId(), event.getCount());
  }

  private void onChestSpawned(@Nullable ChestSpawnedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] ChestSpawned world=%s model=%s pos=(%.2f,%.2f,%.2f) prefab=%s",
        event.getWorld().getName(), event.getModelId(), event.getPosition().x,
        event.getPosition().y, event.getPosition().z, event.getPrefabPath());
  }

  private void onInstanceInitialized(@Nullable InstanceInitializedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] InstanceInitialized world=%s", event.getWorld().getName());
  }

  private void onInstanceTeardownStarted(@Nullable InstanceTeardownStartedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] InstanceTeardownStarted world=%s", event.getWorldName());
  }

  private void onInstanceTeardownCompleted(@Nullable InstanceTeardownCompletedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] InstanceTeardownCompleted world=%s", event.getWorldName());
  }

  private void onPlayerJoinedServer(@Nullable PlayerJoinedServerEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] PlayerJoinedServer world=%s player=%s",
        event.getWorld().getName(), formatPlayer(event.getPlayerRef()));
  }

  private void onVexChallengeCommand(@Nullable VexChallengeCommandEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] VexChallengeCommand player=%s world=%s countdown=%d prefab=%s",
        formatPlayer(event.getPlayerRef()),
        event.getWorldName() == null ? "<unknown>" : event.getWorldName(),
        event.getCountdownSeconds(),
        event.getPrefabPath());
  }

  private void onPrefabEntitySpawned(@Nullable PrefabEntitySpawnedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] PrefabEntitySpawned world=%s model=%s pos=(%.2f,%.2f,%.2f) prefab=%s",
        event.getWorld().getName(), event.getModelId(), event.getPosition().x,
        event.getPosition().y, event.getPosition().z, event.getPrefabPath());
  }

  private void onInstanceCapacityReached(@Nullable InstanceCapacityReachedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] InstanceCapacityReached template=%s world=%s players=%d/%d",
        event.getInstanceTemplate(), event.getWorldName(), event.getCurrentPlayers(),
        event.getMaxPlayers());
  }

  private void onPortalCloseRequested(@Nullable PortalCloseRequestedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] PortalCloseRequested portal=%s", event.getPortalId());
  }

  private void onPortalOwnerRegistered(@Nullable PortalOwnerRegisteredEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] PortalOwnerRegistered portal=%s owner=%s",
        event.getPortalId(), event.getOwnerId());
  }

  private void onCountdownHudClearRequested(@Nullable CountdownHudClearRequestedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] CountdownHudClearRequested player=%s", formatPlayer(event.getPlayerRef()));
  }

  private void onVexWelcomeHudRequested(@Nullable VexWelcomeHudRequestedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] WelcomeHudRequested player=%s", formatPlayer(event.getPlayerRef()));
  }

  private void onVexScoreHudRequested(@Nullable VexScoreHudRequestedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] ScoreHudRequested player=%s instanceScore=%d playerScore=%d delta=%d",
        formatPlayer(event.getPlayerRef()), event.getInstanceScore(), event.getPlayerScore(),
        event.getDelta());
  }

  private void onVexSummaryHudRequested(@Nullable VexSummaryHudRequestedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] SummaryHudRequested player=%s", formatPlayer(event.getPlayerRef()));
  }

  private void onVexLeaderboardHudRequested(@Nullable VexLeaderboardHudRequestedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] LeaderboardHudRequested player=%s", formatPlayer(event.getPlayerRef()));
  }

  private void onVexDemoHudRequested(@Nullable VexDemoHudRequestedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] DemoHudRequested player=%s", formatPlayer(event.getPlayerRef()));
  }

  private void onPortalCountdownHudUpdateRequested(@Nullable PortalCountdownHudUpdateRequestedEvent event) {
    if (event == null) {
      return;
    }
    log.info("[EVENT] PortalCountdownHudUpdateRequested player=%s portal=%s time=%s",
        formatPlayer(event.getPlayerRef()), event.getPortalId(), event.getTimeLeft());
  }

  private String formatPlayer(@Nullable PlayerRef playerRef) {
    if (playerRef == null) {
      return "<null>";
    }
    UUID uuid = playerRef.getUuid();
    String name = playerRef.getUsername();
    if (name == null || name.isBlank()) {
      return uuid != null ? uuid.toString() : "<unknown>";
    }
    return uuid == null ? name : name + " (" + uuid + ")";
  }
}
