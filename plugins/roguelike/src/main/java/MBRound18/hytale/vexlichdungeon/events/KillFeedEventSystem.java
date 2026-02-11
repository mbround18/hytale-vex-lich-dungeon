package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.hytale.vexlichdungeon.dungeon.RoguelikeDungeonController;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import MBRound18.hytale.shared.interfaces.util.UiMessage;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class KillFeedEventSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
  private static final String VEX_WORLD_SUBSTRING = "Vex_The_Lich_Dungeon";
  private final RoguelikeDungeonController roguelikeController;

  public KillFeedEventSystem(@Nonnull RoguelikeDungeonController roguelikeController) {
    super(KillFeedEvent.KillerMessage.class);
    this.roguelikeController = roguelikeController;
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.any();
  }

  @Override
  public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
      @Nonnull KillFeedEvent.KillerMessage event) {
    EntityStore entityStore = store.getExternalData();
    World world = entityStore.getWorld();

    Ref<EntityStore> victimRef = event.getTargetRef();
    if (victimRef == null || !victimRef.isValid()) {
      return;
    }

    UUIDComponent uuidComponent = store.getComponent(victimRef, UUIDComponent.getComponentType());
    if (uuidComponent == null || uuidComponent.getUuid() == null) {
      return;
    }
    UUID victimUuid = uuidComponent.getUuid();

    if (world != null && world.getName() != null && world.getName().contains(VEX_WORLD_SUBSTRING)) {
      RoguelikeDungeonController.EnemyKillInfo info = roguelikeController.getEnemyKillInfo(world, victimUuid);
      String victimName = sanitizeLabel(resolveVictimName(store, victimRef, info != null ? info.getType() : null));
      String killerName = sanitizeLabel(resolveKillerName(store, event.getDamage()));
      String weaponName = sanitizeLabel(resolveWeaponName(store, event.getDamage()));
      Integer points = info != null ? info.getPoints() : null;

      StringBuilder message = new StringBuilder();
      if (killerName != null && !killerName.isBlank()) {
        message.append(killerName).append(" ");
      }
      message.append("killed ").append(victimName != null ? victimName : "Enemy");
      if (weaponName != null && !weaponName.isBlank()) {
        message.append(" with ").append(weaponName);
      }
      if (points != null && points > 0) {
        message.append(" (points ").append(points).append(")");
      }
      event.setMessage(UiMessage.raw(message.toString()));
    }

    UUID killerUuid = resolveKillerUuid(store, event.getDamage());
    Vector3d position = resolvePosition(store, victimRef);

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

  @Nullable
  private String resolveVictimName(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
      @Nullable String fallback) {
    DisplayNameComponent displayName = store.getComponent(ref, DisplayNameComponent.getComponentType());
    if (displayName != null && displayName.getDisplayName() != null) {
      String value = String.valueOf(displayName.getDisplayName());
      if (!value.isBlank()) {
        return value;
      }
    }
    ModelComponent model = store.getComponent(ref, ModelComponent.getComponentType());
    if (model != null && model.getModel() != null) {
      String value = model.getModel().getModelAssetId();
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return fallback;
  }

  @Nullable
  private String resolveKillerName(@Nonnull Store<EntityStore> store, @Nullable Damage damage) {
    if (damage == null) {
      return null;
    }
    Damage.Source source = damage.getSource();
    if (!(source instanceof Damage.EntitySource entitySource)) {
      return null;
    }
    Ref<EntityStore> sourceRef = entitySource.getRef();
    if (sourceRef == null || !sourceRef.isValid()) {
      return null;
    }
    UUIDComponent uuidComponent = store.getComponent(sourceRef, UUIDComponent.getComponentType());
    if (uuidComponent != null && uuidComponent.getUuid() != null) {
      PlayerRef playerRef = Universe.get().getPlayer(uuidComponent.getUuid());
      if (playerRef != null && playerRef.getUsername() != null) {
        return playerRef.getUsername();
      }
    }
    DisplayNameComponent displayName = store.getComponent(sourceRef, DisplayNameComponent.getComponentType());
    if (displayName != null && displayName.getDisplayName() != null) {
      String value = String.valueOf(displayName.getDisplayName());
      if (!value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  @Nullable
  private String resolveWeaponName(@Nonnull Store<EntityStore> store, @Nullable Damage damage) {
    if (damage == null) {
      return null;
    }
    Damage.Source source = damage.getSource();
    if (!(source instanceof Damage.EntitySource entitySource)) {
      return null;
    }
    Ref<EntityStore> sourceRef = entitySource.getRef();
    if (sourceRef == null || !sourceRef.isValid()) {
      return null;
    }
    Player player = store.getComponent(sourceRef, Player.getComponentType());
    if (player == null) {
      return null;
    }
    Inventory inventory = player.getInventory();
    if (inventory == null) {
      return null;
    }
    ItemStack item = inventory.getItemInHand();
    if (item == null || item.isEmpty()) {
      item = inventory.getActiveHotbarItem();
    }
    if (item == null || item.isEmpty()) {
      item = inventory.getActiveToolItem();
    }
    if (item == null || item.isEmpty()) {
      item = inventory.getToolsItem();
    }
    if (item == null || item.isEmpty()) {
      item = inventory.getUtilityItem();
    }
    if (item == null || item.isEmpty()) {
      return null;
    }
    String itemId = item.getItemId();
    return itemId == null || itemId.isBlank() ? null : itemId;
  }

  @Nullable
  private String sanitizeLabel(@Nullable String value) {
    if (value == null) {
      return null;
    }
    return value.replace("/", " ").replace("=", " ");
  }
}
