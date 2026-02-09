package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class KillFeedEventSystem extends EntityEventSystem<EntityStore, KillFeedEvent.DecedentMessage> {
  private final RoguelikeDungeonController roguelikeController;

  public KillFeedEventSystem(@Nonnull RoguelikeDungeonController roguelikeController) {
    super(KillFeedEvent.DecedentMessage.class);
    this.roguelikeController = roguelikeController;
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.any();
  }

  @Override
  public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
      @Nonnull KillFeedEvent.DecedentMessage event) {
    EntityStore entityStore = store.getExternalData();
    World world = entityStore.getWorld();

    Ref<EntityStore> ref = chunk.getReferenceTo(index);
    if (ref == null || !ref.isValid()) {
      return;
    }

    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    if (uuidComponent == null || uuidComponent.getUuid() == null) {
      return;
    }
    UUID victimUuid = uuidComponent.getUuid();

    UUID killerUuid = resolveKillerUuid(store, event.getDamage());
    Vector3d position = resolvePosition(store, ref);

    world.execute(() -> roguelikeController.handleEntityEliminated(world, victimUuid, killerUuid, position));
  }

  @Nullable
  private Vector3d resolvePosition(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    return transform == null ? null : transform.getPosition();
  }

  @Nullable
  private UUID resolveKillerUuid(@Nonnull Store<EntityStore> store, @Nullable Damage damage) {
    if (damage == null) {
      return null;
    }

    Damage.Source source = damage.getSource();
    if (source instanceof Damage.EntitySource entitySource) {
      Ref<EntityStore> sourceRef = entitySource.getRef();
      if (sourceRef != null && sourceRef.isValid()) {
        UUIDComponent sourceUuid = store.getComponent(sourceRef, UUIDComponent.getComponentType());
        if (sourceUuid != null) {
          return sourceUuid.getUuid();
        }
      }
    }

    return null;
  }
}
