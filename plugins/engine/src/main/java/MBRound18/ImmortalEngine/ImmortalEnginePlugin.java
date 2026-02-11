package MBRound18.ImmortalEngine;

import MBRound18.ImmortalEngine.api.events.AssetPacksLoadedEvent;
import MBRound18.ImmortalEngine.api.events.EliminationEvent;
import MBRound18.ImmortalEngine.api.events.EventDispatcher;
import MBRound18.ImmortalEngine.api.events.PortalClosedEvent;
import MBRound18.ImmortalEngine.api.events.PortalCreatedEvent;
import MBRound18.ImmortalEngine.api.events.WorldCreatedEvent;
import MBRound18.ImmortalEngine.api.events.WorldEnteredEvent;
import MBRound18.ImmortalEngine.api.events.WorldExitedEvent;
import MBRound18.ImmortalEngine.api.events.WorldPlayerSpawnedEvent;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.AssetPackRegisterEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

/**
 * Headless microgame engine plugin entrypoint.
 * 
 * Event Producer Mapping (HytaleServer → Engine Events):
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ HytaleServer Events → ImmortalEngine Events │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ AssetPackRegisterEvent → AssetPacksLoadedEvent ✓ │
 * │ StartWorldEvent → WorldCreatedEvent ✓ │
 * │ AddPlayerToWorldEvent → WorldEnteredEvent ✓ │
 * │ DrainPlayerFromWorldEvent → WorldExitedEvent ✓ │
 * │ KillFeedEvent.KillerMessage → EliminationEvent ✓ │
 * │ │
 * │ Portal Events → PortalCreatedEvent ✗ (TODO) │
 * │ Portal Events → PortalClosedEvent ✗ (TODO) │
 * └─────────────────────────────────────────────────────────────────┘
 * 
 * Notes on Portal Events:
 * - Portal events are currently created by the roguelike adapter and consumed
 * but not produced by HytaleServer core events
 * - Need to hook into world portal management system to capture
 * creation/closure
 * - Consider listening to portal-related ECS events or component lifecycle
 */
public class ImmortalEnginePlugin extends JavaPlugin {
  private enum KillSystemRegistration {
    REGISTERED,
    ALREADY_REGISTERED,
    DEFERRED
  }

  private static final class KillSystemRegistrationStats {
    private int registered;
    private int deferred;
  }
  private final AtomicBoolean assetsLoaded = new AtomicBoolean(false);
  private final Set<Integer> registeredEntityStores = ConcurrentHashMap.newKeySet();
  private final Set<String> spawnedPlayers = ConcurrentHashMap.newKeySet();
  private final Set<String> scheduledPlayerSpawnChecks = ConcurrentHashMap.newKeySet();
  private HytaleLogger log;

  public ImmortalEnginePlugin(@Nonnull JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    this.log = getLogger().getSubLogger("ImmortalEngine");
    // Engine runtime is bootstrapped by adapters in future revisions.
  }

  @Override
  protected void start() {
    EventBus eventBus = HytaleServer.get().getEventBus();
    if (eventBus == null) {
      log.at(Level.SEVERE).log("EventBus not available, cannot register kill event handlers");
      return;
    }
    EventRegistry eventRegistry = getEventRegistry();
    if (eventRegistry == null) {
      log.at(Level.SEVERE).log("EventRegistry not available, cannot register world/player event handlers");
    }

    log.at(Level.INFO).log("[STARTUP] Registering ImmortalEngine event listeners");

    // Listen for asset pack registration and fire AssetPacksLoadedEvent
    Consumer<AssetPackRegisterEvent> assetListener = e -> {
      if (assetsLoaded.compareAndSet(false, true)) {
        log.at(Level.FINE).log("[ASSETS] Asset packs loaded, dispatching AssetPacksLoadedEvent");
        EventDispatcher.dispatch(eventBus, new AssetPacksLoadedEvent());
      }
    };

    eventBus.register(AssetPackRegisterEvent.class, assetListener);
    log.at(Level.INFO).log("[STARTUP] Registered AssetPackRegisterEvent listener");

    // Listen for new worlds starting and register kill feed systems for them
    // This is the primary way we register kill event systems, as worlds have fully
    // initialized at this point
    Consumer<StartWorldEvent> worldStartListener = event -> {
      World world = event.getWorld();
      if (world != null) {
        log.at(Level.INFO).log("[WORLD] StartWorldEvent fired for world: " + world.getName());
        registerKillEventSystem(eventBus, world);
        // Fire WorldCreatedEvent for the engine
        log.at(Level.INFO).log("[EVENT-PRODUCER] Dispatching WorldCreatedEvent for world: " + world.getName());
        EventDispatcher.dispatch(eventBus, new WorldCreatedEvent(world));
      }
    };
    eventBus.registerGlobal(StartWorldEvent.class, worldStartListener);
    log.at(Level.INFO).log("[STARTUP] Registered StartWorldEvent listener");

    // Listen for player additions to worlds and fire WorldEnteredEvent
    Consumer<AddPlayerToWorldEvent> playerAddListener = event -> {
      World world = event.getWorld();
      Holder<EntityStore> holder = event.getHolder();
      PlayerRef playerRef = holder != null ? holder.getComponent(PlayerRef.getComponentType()) : null;
      if (world != null && playerRef != null) {
        log.at(Level.INFO).log("[EVENT-PRODUCER] Dispatching WorldEnteredEvent for player " + playerRef.getUuid()
            + " in world: " + world.getName());
        EventDispatcher.dispatch(eventBus, new WorldEnteredEvent(world, playerRef));
      } else if (world != null && holder != null) {
        log.at(Level.INFO).log("[EVENT-PRODUCER] Deferring WorldEnteredEvent until PlayerRef is available for holder "
            + System.identityHashCode(holder) + " in world: " + world.getName());
      }
      if (world != null && holder != null) {
        scheduleWorldPlayerSpawned(eventBus, world, holder);
      }
    };
    if (eventRegistry != null) {
      eventRegistry.registerGlobal(AddPlayerToWorldEvent.class, playerAddListener);
      log.at(Level.INFO).log("[STARTUP] Registered AddPlayerToWorldEvent listener (EventRegistry)");
    }

    // Listen for player removal from worlds and fire WorldExitedEvent
    Consumer<DrainPlayerFromWorldEvent> playerDrainListener = event -> {
      World world = event.getWorld();
      PlayerRef playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
      if (world != null && playerRef != null) {
        log.at(Level.INFO).log("[EVENT-PRODUCER] Dispatching WorldExitedEvent for player " + playerRef.getUuid()
            + " from world: " + world.getName());
        EventDispatcher.dispatch(eventBus, new WorldExitedEvent(world, playerRef));
      }
    };
    if (eventRegistry != null) {
      eventRegistry.registerGlobal(DrainPlayerFromWorldEvent.class, playerDrainListener);
      log.at(Level.INFO).log("[STARTUP] Registered DrainPlayerFromWorldEvent listener (EventRegistry)");
    }

    // Listen for PlayerReadyEvent to register kill systems per-world when universe
    // is authenticated
    // This fires after universe is fully ready and players begin joining
    Consumer<PlayerReadyEvent> playerReadyListener = event -> {
      log.at(Level.INFO).log("[UNIVERSE] PlayerReadyEvent fired - registering kill systems for all ready worlds");
      registerKillEventSystemsForAllWorlds(eventBus);
    };
    if (eventRegistry != null) {
      eventRegistry.registerGlobal(PlayerReadyEvent.class, playerReadyListener);
      log.at(Level.INFO).log("[STARTUP] Registered PlayerReadyEvent listener (EventRegistry)");
    }

    // Start a background retry loop to register kill systems once EntityStore
    // becomes available.
    startKillSystemRetryThread(eventBus);
  }

  /**
   * Register ECS event systems for KillFeedEvent to capture elimination data.
   * Called when all worlds are ready (via PlayerReadyEvent).
   */
  private KillSystemRegistrationStats registerKillEventSystemsForAllWorlds(EventBus eventBus) {
    Universe universe = Universe.get();
    if (universe == null) {
      log.at(Level.WARNING).log("[KILL-SYSTEM] Universe not available for kill event system registration");
      return new KillSystemRegistrationStats();
    }
    int worldCount = universe.getWorlds().size();
    log.at(Level.INFO).log("[KILL-SYSTEM] Registering kill systems for " + worldCount + " world(s)");
    KillSystemRegistrationStats stats = new KillSystemRegistrationStats();
    for (World world : universe.getWorlds().values()) {
      KillSystemRegistration status = registerKillEventSystem(eventBus, world);
      if (status == KillSystemRegistration.REGISTERED) {
        stats.registered++;
      } else if (status == KillSystemRegistration.DEFERRED) {
        stats.deferred++;
      }
    }
    return stats;
  }

  private KillSystemRegistration registerKillEventSystem(EventBus eventBus, World world) {
    if (world == null) {
      log.at(Level.WARNING).log("[KILL-SYSTEM] Attempted to register kill system for null world");
      return KillSystemRegistration.DEFERRED;
    }

    try {
      Store<EntityStore> store = world.getEntityStore().getStore();
      if (store == null) {
        log.at(Level.INFO).log(
            "[KILL-SYSTEM] EntityStore.Store not available for world: " + world.getName() + ", deferring registration");
        return KillSystemRegistration.DEFERRED;
      }
      int key = System.identityHashCode(store);
      if (!registeredEntityStores.add(key)) {
        log.at(Level.INFO).log("[KILL-SYSTEM] Kill system already registered for world: " + world.getName());
        return KillSystemRegistration.ALREADY_REGISTERED;
      }

      try {
        store.getRegistry().registerSystem(new KillFeedKillerEventSystem(eventBus, this));
        log.at(Level.INFO)
            .log("[KILL-SYSTEM] Registered KillFeedEvent.KillerMessage ECS system for world: " + world.getName());
        return KillSystemRegistration.REGISTERED;
      } catch (IllegalArgumentException e) {
        log.at(Level.INFO).log("[KILL-SYSTEM] KillFeedEvent system already registered for world: " + world.getName());
        return KillSystemRegistration.ALREADY_REGISTERED;
      }
    } catch (NullPointerException e) {
      log.at(Level.INFO).log("[KILL-SYSTEM] EntityStore initialization incomplete for world: " + world.getName()
          + ", deferring registration");
      return KillSystemRegistration.DEFERRED;
    }
  }

  private void startKillSystemRetryThread(EventBus eventBus) {
    Thread retryThread = new Thread(() -> {
      final int maxAttempts = 30;
      final long sleepMillis = 2000;
      for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        KillSystemRegistrationStats stats = registerKillEventSystemsForAllWorlds(eventBus);
        if (stats.deferred == 0) {
          if (stats.registered > 0) {
            log.at(Level.INFO).log("[KILL-SYSTEM] Registered kill system(s) after " + attempt + " attempt(s)");
          } else {
            log.at(Level.INFO).log("[KILL-SYSTEM] Kill systems already registered; stopping retry loop");
          }
          return;
        }
        try {
          Thread.sleep(sleepMillis);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
      log.at(Level.WARNING).log("[KILL-SYSTEM] Retry window elapsed without registering kill systems");
    }, "ImmortalEngine-KillSystemRetry");
    retryThread.setDaemon(true);
    retryThread.start();
  }

  private void scheduleWorldPlayerSpawned(EventBus eventBus, World world, Holder<EntityStore> holder) {
    if (world == null || holder == null) {
      return;
    }
    String pendingKey = world.getName() + ":" + System.identityHashCode(holder);
    if (!scheduledPlayerSpawnChecks.add(pendingKey)) {
      return;
    }

    final int maxAttempts = 25;
    final long delayMs = 200L;
    final int[] attempts = new int[] { 0 };
    final ScheduledFuture<?>[] handle = new ScheduledFuture<?>[1];

    Runnable check = () -> {
      attempts[0]++;
      try {
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) {
          if (attempts[0] == 1) {
            log.at(Level.INFO).log("[EVENT-PRODUCER] Waiting for PlayerRef on holder "
                + System.identityHashCode(holder) + " in world: " + world.getName());
          }
        } else if (isPlayerSpawnedInWorld(world, playerRef)) {
          String spawnKey = world.getName() + ":" + playerRef.getUuid();
          if (spawnedPlayers.add(spawnKey)) {
            log.at(Level.INFO).log(
                "[EVENT-PRODUCER] Dispatching WorldPlayerSpawnedEvent for player " + playerRef.getUuid()
                    + " in world: " + world.getName());
            boolean dispatched = EventDispatcher.dispatch(eventBus, new WorldPlayerSpawnedEvent(world, playerRef));
            if (!dispatched) {
              log.at(Level.WARNING).log("[EVENT-PRODUCER] WorldPlayerSpawnedEvent dispatch failed for player "
                  + playerRef.getUuid() + " in world: " + world.getName());
            }
          }
          if (handle[0] != null) {
            handle[0].cancel(false);
          }
          return;
        }
      } catch (Exception ignored) {
      }
      if (attempts[0] >= maxAttempts && handle[0] != null) {
        log.at(Level.WARNING).log(
            "[EVENT-PRODUCER] WorldPlayerSpawnedEvent timed out for holder " + System.identityHashCode(holder)
                + " in world: " + world.getName());
        handle[0].cancel(false);
      }
    };

    log.at(Level.INFO).log("[EVENT-PRODUCER] Scheduling WorldPlayerSpawnedEvent checks for holder "
        + System.identityHashCode(holder) + " in world: " + world.getName());
    handle[0] = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(check, 0, delayMs, TimeUnit.MILLISECONDS);
  }

  private boolean isPlayerSpawnedInWorld(World world, PlayerRef playerRef) {
    if (world == null || playerRef == null || !playerRef.isValid()) {
      return false;
    }
    if (playerRef.getWorldUuid() == null) {
      return false;
    }
    World currentWorld = Universe.get().getWorld(playerRef.getWorldUuid());
    if (currentWorld == null || !currentWorld.getName().equals(world.getName())) {
      return false;
    }
    try {
      Ref<EntityStore> ref = playerRef.getReference();
      if (ref == null || !ref.isValid()) {
        return false;
      }
      Store<EntityStore> store = ref.getStore();
      if (store == null) {
        return false;
      }
      Player player = store.getComponent(ref, Player.getComponentType());
      boolean ready = player != null;
      if (ready) {
        return true;
      }
      return false;
    } catch (Exception ignored) {
      return false;
    }
  }

  /**
   * Handle KillFeedEvent.KillerMessage and extract complete elimination
   * information.
   */
  private void handleKillerMessage(EventBus eventBus, KillFeedEvent.KillerMessage event, World world) {
    try {
      // Extract damage information
      Damage damage = event.getDamage();
      if (damage == null) {
        log.at(Level.WARNING).log("[KILL-EVENT] Received KillFeedEvent with null damage");
        return;
      }

      // Extract victim reference (targetRef)
      Ref<EntityStore> victimRef = event.getTargetRef();

      if (victimRef == null) {
        log.at(Level.WARNING).log("[KILL-EVENT] Received KillFeedEvent with null victim reference");
        return;
      }

      log.at(Level.INFO).log("[KILL-EVENT] Processing KillFeedEvent for victim");

      if (world == null) {
        log.at(Level.WARNING).log("[KILL-EVENT] Could not extract world from elimination event");
        return;
      }

      // Extract killer information from damage source
      Ref<EntityStore> killerRef = null;
      Ref<EntityStore> projectileRef = null;

      Damage.Source source = damage.getSource();
      if (source != null) {
        if (source instanceof Damage.EntitySource) {
          Damage.EntitySource entitySource = (Damage.EntitySource) source;
          killerRef = entitySource.getRef();

          // Check if it's a projectile source
          if (source instanceof Damage.ProjectileSource) {
            Damage.ProjectileSource projectileSource = (Damage.ProjectileSource) source;
            projectileRef = projectileSource.getProjectile();
          }
        }
      }

      // Extract damage cause
      String damageCauseName = null;
      try {
        DamageCause cause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (cause != null) {
          damageCauseName = cause.getId();
        }
      } catch (Exception ignored) {
      }

      // Extract death icon from damage metadata
      String deathIcon = null;
      try {
        deathIcon = damage.getIfPresentMetaObject(Damage.DEATH_ICON);
      } catch (Exception ignored) {
      }

      // Get damage amount
      float damageAmount = damage.getAmount();

      // Create and dispatch EliminationEvent
      EliminationEvent eliminationEvent = new EliminationEvent(
          world,
          victimRef,
          killerRef,
          projectileRef,
          damageCauseName,
          damageAmount,
          deathIcon);

      EventDispatcher.dispatch(eventBus, eliminationEvent);

      // Log the kill event
      String killerType = killerRef != null ? "Entity" : "Environmental";
      String projectileType = projectileRef != null ? " (projectile)" : "";
      String cause = damageCauseName != null ? damageCauseName : "unknown";

      log.at(Level.INFO).log("[KILL] " + killerType + projectileType + " killed entity in world '"
          + world.getName() + "' via " + cause + " (" + damageAmount + " dmg)");

    } catch (Exception e) {
      log.at(Level.SEVERE).log("[KILL-EVENT] Failed to process KillFeedEvent.KillerMessage: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static final class KillFeedKillerEventSystem
      extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
    private final EventBus eventBus;
    private final ImmortalEnginePlugin plugin;

    private KillFeedKillerEventSystem(EventBus eventBus, ImmortalEnginePlugin plugin) {
      super(KillFeedEvent.KillerMessage.class);
      this.eventBus = eventBus;
      this.plugin = plugin;
    }

    @Override
    public void handle(
        int entityIndex,
        com.hypixel.hytale.component.ArchetypeChunk<EntityStore> chunk,
        Store<EntityStore> store,
        com.hypixel.hytale.component.CommandBuffer<EntityStore> commandBuffer,
        KillFeedEvent.KillerMessage event) {
      plugin.log.at(Level.INFO)
          .log("[KILL-SYSTEM] KillFeedEvent.KillerMessage received on entity index: " + entityIndex);
      World world = null;
      if (store != null) {
        try {
          EntityStore entityStore = store.getExternalData();
          if (entityStore != null) {
            world = entityStore.getWorld();
          }
        } catch (Exception ignored) {
        }
      }
      plugin.handleKillerMessage(eventBus, event, world);
    }

    @Override
    public com.hypixel.hytale.component.query.Query<EntityStore> getQuery() {
      return Archetype.empty();
    }
  }

  /**
   * Extract world from an entity reference.
   */
  private World extractWorldFromEntityRef(Ref<EntityStore> entityRef) {
    if (entityRef == null) {
      return null;
    }

    try {
      // Try to get world from the entity reference
      Object world = entityRef.getClass().getMethod("getWorld").invoke(entityRef);
      if (world instanceof World) {
        return (World) world;
      }

      // Alternative: try getComponentAccessor and then getWorld
      Object accessor = entityRef.getClass().getMethod("getComponentAccessor").invoke(entityRef);
      if (accessor != null) {
        Object worldFromAccessor = accessor.getClass().getMethod("getWorld").invoke(accessor);
        if (worldFromAccessor instanceof World) {
          return (World) worldFromAccessor;
        }
      }
    } catch (Exception e) {
      // Debug logging - could not extract world from entity ref
    }

    return null;
  }

  @Override
  protected void shutdown() {
    // No-op for now.
  }
}
