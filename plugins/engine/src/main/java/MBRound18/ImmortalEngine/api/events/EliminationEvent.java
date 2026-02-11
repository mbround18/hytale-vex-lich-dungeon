package MBRound18.ImmortalEngine.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Event fired when an entity is eliminated (killed).
 * Contains information about who killed what, where, and with what weapon.
 */
public final class EliminationEvent extends DebugEvent {
  private final World world;
  private final Ref<EntityStore> victimRef;
  private final Ref<EntityStore> killerRef;
  private final Ref<EntityStore> projectileRef;
  private final String damageCause;
  private final float damageAmount;
  private final String deathIcon;

  public EliminationEvent(
      @Nonnull World world,
      @Nonnull Ref<EntityStore> victimRef,
      @Nullable Ref<EntityStore> killerRef,
      @Nullable Ref<EntityStore> projectileRef,
      @Nullable String damageCause,
      float damageAmount,
      @Nullable String deathIcon) {
    this.world = world;
    this.victimRef = victimRef;
    this.killerRef = killerRef;
    this.projectileRef = projectileRef;
    this.damageCause = damageCause;
    this.damageAmount = damageAmount;
    this.deathIcon = deathIcon;
  }

  @Override
  public Object toPayload() {
    try {
      // Prefer immediate payload to avoid serialization timeouts.
      return withCorrelation(buildPayload());
    } catch (Throwable ignored) {
      return withCorrelation(onWorldThread(world, this::buildPayload));
    }
  }

  private Object buildPayload() {
    Map<String, Object> payload = new LinkedHashMap<>();

    // World information
    payload.put("world", worldMeta(world));

    // Victim information
    Map<String, Object> victim = extractEntityInfo(victimRef);
    if (victim != null) {
      payload.put("victim", victim);
    }

    // Killer information
    if (killerRef != null) {
      Map<String, Object> killer = extractEntityInfo(killerRef);
      if (killer != null) {
        payload.put("killer", killer);
      }
    }

    // Projectile/weapon information
    if (projectileRef != null) {
      Map<String, Object> projectile = extractEntityInfo(projectileRef);
      if (projectile != null) {
        payload.put("weapon", projectile);
      }
    }

    // Damage information
    Map<String, Object> damage = new LinkedHashMap<>();
    if (damageCause != null) {
      damage.put("cause", damageCause);
    }
    damage.put("amount", damageAmount);
    if (deathIcon != null) {
      damage.put("icon", deathIcon);
    }
    if (killerRef != null) {
      Map<String, Object> heldItem = extractHeldItem(killerRef);
      if (heldItem != null && !heldItem.isEmpty()) {
        damage.put("item", heldItem);
      }
    }
    payload.put("damage", damage);

    return payload;
  }

  private Map<String, Object> extractEntityInfo(Ref<EntityStore> entityRef) {
    if (entityRef == null) {
      return null;
    }

    Map<String, Object> info = new LinkedHashMap<>();

    try {
      // Store-level access (more reliable than reflection on ref)
      Store<EntityStore> store = null;
      try {
        store = entityRef.getStore();
      } catch (Exception ignored) {
      }

      if (store != null) {
        try {
          UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
          if (uuidComponent != null && uuidComponent.getUuid() != null) {
            info.put("id", uuidComponent.getUuid().toString());
          }
        } catch (Exception ignored) {
        }

        try {
          DisplayNameComponent displayName = store.getComponent(entityRef, DisplayNameComponent.getComponentType());
          if (displayName != null) {
            Message message = displayName.getDisplayName();
            if (message != null) {
              info.put("name", message.getRawText());
            }
          }
        } catch (Exception ignored) {
        }

        try {
          ModelComponent modelComponent = store.getComponent(entityRef, ModelComponent.getComponentType());
          if (modelComponent != null && modelComponent.getModel() != null) {
            String modelId = modelComponent.getModel().getModelAssetId();
            if (modelId == null || modelId.isBlank()) {
              modelId = modelComponent.getModel().getModel();
            }
            if (modelId != null && !modelId.isBlank()) {
              info.put("type", modelId);
            }
          }
        } catch (Exception ignored) {
        }

        try {
          TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
          if (transform != null) {
            Vector3d position = transform.getPosition();
            if (position != null) {
              Map<String, Object> coords = new LinkedHashMap<>();
              coords.put("x", formatCoordinate(position.getX()));
              coords.put("y", formatCoordinate(position.getY()));
              coords.put("z", formatCoordinate(position.getZ()));
              info.put("location", coords);
            }
          }
        } catch (Exception ignored) {
        }

        try {
          ItemComponent itemComponent = store.getComponent(entityRef, ItemComponent.getComponentType());
          if (itemComponent != null) {
            ItemStack itemStack = itemComponent.getItemStack();
            if (itemStack != null && itemStack.getItemId() != null && !itemStack.getItemId().isBlank()) {
              info.put("itemId", itemStack.getItemId());
              info.put("itemCount", itemStack.getQuantity());
              info.put("itemDurability", itemStack.getDurability());
              info.put("itemMaxDurability", itemStack.getMaxDurability());
            }
          }
        } catch (Exception ignored) {
        }
      }

      // Get entity ID/UUID
      if (!info.containsKey("id")) {
        Object entityId = invokeAny(entityRef, "getUuid", "getId");
        if (entityId != null) {
          info.put("id", entityId.toString());
        }
      }

      // Get entity type/name
      if (!info.containsKey("type")) {
        Object typeName = invokeAny(entityRef, "getTypeName", "getType", "getName");
        if (typeName != null) {
          info.put("type", typeName.toString());
        }
      }

      // Try to get position/location
      if (!info.containsKey("location")) {
        Object position = invokeAny(entityRef, "getPosition", "getLocation");
        if (position != null) {
          Map<String, Object> coords = new LinkedHashMap<>();
          Object x = invokeAny(position, "getX", "x");
          Object y = invokeAny(position, "getY", "y");
          Object z = invokeAny(position, "getZ", "z");

          if (x != null)
            coords.put("x", formatCoordinate(x));
          if (y != null)
            coords.put("y", formatCoordinate(y));
          if (z != null)
            coords.put("z", formatCoordinate(z));

          if (!coords.isEmpty()) {
            info.put("location", coords);
          }
        }
      }

      // Try to get player name if it's a player
      if (!info.containsKey("name")) {
        Object username = invokeAny(entityRef, "getUsername", "getName", "getDisplayName");
        if (username != null) {
          info.put("name", username.toString());
        }
      }

    } catch (Exception e) {
      // Fallback: just include the reference string
      info.put("ref", entityRef.toString());
    }

    return info.isEmpty() ? null : info;
  }

  private Map<String, Object> extractHeldItem(Ref<EntityStore> entityRef) {
    if (entityRef == null) {
      return null;
    }
    try {
      Store<EntityStore> store = entityRef.getStore();
      if (store == null) {
        return null;
      }
      Player player = store.getComponent(entityRef, Player.getComponentType());
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
      Map<String, Object> data = new LinkedHashMap<>();
      String itemId = item.getItemId();
      if (itemId != null && !itemId.isBlank()) {
        data.put("id", itemId);
      }
      data.put("count", item.getQuantity());
      data.put("durability", item.getDurability());
      data.put("maxDurability", item.getMaxDurability());
      return data;
    } catch (Exception ignored) {
      return null;
    }
  }

  private static Object formatCoordinate(Object coord) {
    if (coord instanceof Number) {
      double value = ((Number) coord).doubleValue();
      return Math.round(value * 100.0) / 100.0;
    }
    return coord;
  }

  @Nonnull
  public World getWorld() {
    return world;
  }

  @Nonnull
  public Ref<EntityStore> getVictimRef() {
    return victimRef;
  }

  @Nullable
  public Ref<EntityStore> getKillerRef() {
    return killerRef;
  }

  @Nullable
  public Ref<EntityStore> getProjectileRef() {
    return projectileRef;
  }

  @Nullable
  public String getDamageCause() {
    return damageCause;
  }

  public float getDamageAmount() {
    return damageAmount;
  }

  @Nullable
  public String getDeathIcon() {
    return deathIcon;
  }
}
