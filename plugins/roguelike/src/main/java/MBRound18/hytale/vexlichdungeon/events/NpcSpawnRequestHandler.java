package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.shared.utilities.LoggingHelper;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.npc.NPCPlugin;
import it.unimi.dsi.fastutil.Pair;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class NpcSpawnRequestHandler {
  private final LoggingHelper log = new LoggingHelper(NpcSpawnRequestHandler.class);

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void register(@Nonnull EventBus eventBus) {
    eventBus.register(
        (Class) NpcSpawnRequestedEvent.class,
        (java.util.function.Consumer) (Object e) -> {
          if (e instanceof NpcSpawnRequestedEvent event) {
            onNpcSpawnRequested(event);
          }
        });
  }

  private void onNpcSpawnRequested(@Nonnull NpcSpawnRequestedEvent event) {
    NPCPlugin npcPlugin = NPCPlugin.get();
    if (npcPlugin == null) {
      event.getResult().complete(NpcSpawnResult.failure("NPCPlugin unavailable"));
      return;
    }
    String roleName = event.getRoleName();
    String modelId = event.getModelId();
    if (!npcPlugin.hasRoleName(roleName)) {
      event.getResult().complete(NpcSpawnResult.failure("Unknown NPC role: " + roleName));
      return;
    }
    World world = event.getWorld();
    Store<EntityStore> store = world.getEntityStore().getStore();
    if (store == null) {
      event.getResult().complete(NpcSpawnResult.failure("Missing entity store"));
      return;
    }

    Vector3d position = event.getPosition();
    Vector3f rotation = event.getRotation();
    Object spawnResult;
    try {
      spawnResult = npcPlugin.spawnNPC(store, roleName, modelId, position,
          Objects.requireNonNull(rotation, "rotation"));
    } catch (Exception e) {
      log.warn("[NPC-SPAWN] Failed to spawn %s: %s", roleName, e.getMessage());
      event.getResult().complete(NpcSpawnResult.failure(e.getMessage()));
      return;
    }

    UUID uuid = extractUuid(spawnResult);
    if (uuid == null) {
      event.getResult().complete(NpcSpawnResult.failure("NPC spawn returned no UUID"));
      return;
    }
    event.getResult().complete(NpcSpawnResult.success(uuid));
  }

  @Nullable
  private UUID extractUuid(Object spawnResult) {
    if (spawnResult == null) {
      return null;
    }
    Object npc = null;
    if (spawnResult instanceof Pair<?, ?> pair) {
      npc = pair.right();
    } else {
      try {
        if (spawnResult.getClass().getSimpleName().contains("Pair")) {
          java.lang.reflect.Method rightMethod = spawnResult.getClass().getMethod("right");
          npc = rightMethod.invoke(spawnResult);
        }
      } catch (Exception e) {
        // ignore
      }
    }

    UUID reflected = invokeUuid(npc);
    if (reflected != null) {
      return reflected;
    }
    if (npc instanceof INonPlayerCharacter inpc) {
      try {
        java.lang.reflect.Method getUuidMethod = inpc.getClass().getMethod("getUuid");
        Object uuidObj = getUuidMethod.invoke(inpc);
        if (uuidObj instanceof UUID id) {
          return id;
        }
      } catch (Exception e) {
        // ignore
      }
    }
    return null;
  }

  @Nullable
  private UUID invokeUuid(@Nullable Object npc) {
    if (npc == null) {
      return null;
    }
    try {
      java.lang.reflect.Method getUuidMethod = npc.getClass().getMethod("getUuid");
      Object uuidObj = getUuidMethod.invoke(npc);
      if (uuidObj instanceof UUID id) {
        return id;
      }
    } catch (Exception e) {
      // ignore
    }
    return null;
  }
}
