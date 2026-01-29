package MBRound18.ImmortalEngine.api.participants;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Tracks participants currently in an instance/world along with basic stats.
 * Intended for shared HUD/party displays across minigames.
 */
public final class ParticipantTracker {
  private static final ParticipantTracker INSTANCE = new ParticipantTracker();

  private final Map<String, Map<UUID, ParticipantSnapshot>> worlds = new ConcurrentHashMap<>();

  private ParticipantTracker() {
  }

  @Nonnull
  public static ParticipantTracker get() {
    return INSTANCE;
  }

  public void updateFromWorld(@Nullable World world) {
    if (world == null) {
      return;
    }
    String worldName = world.getName();
    Map<UUID, ParticipantSnapshot> participants = worlds.computeIfAbsent(worldName,
        name -> new ConcurrentHashMap<>());
    HashSet<UUID> seen = new HashSet<>();

    EntityStatsModule statsModule = EntityStatsModule.get();
    ComponentType<EntityStore, EntityStatMap> componentType = statsModule != null
        ? statsModule.getEntityStatMapComponentType()
        : EntityStatMap.getComponentType();
    Store<EntityStore> store = world.getEntityStore().getStore();
    long now = System.currentTimeMillis();

    for (PlayerRef playerRef : world.getPlayerRefs()) {
      UUID uuid = playerRef.getUuid();
      String name = resolveDisplayName(playerRef);
      Ref<EntityStore> ref = playerRef.getReference();
      EntityStatMap statMap = ref != null ? store.getComponent(ref, componentType) : null;
      StatSnapshot health = readStat(statMap, "Health");
      StatSnapshot stamina = readStat(statMap, "Stamina");
      participants.put(uuid, new ParticipantSnapshot(uuid, name, health.current, health.max,
          stamina.current, stamina.max, now));
      seen.add(uuid);
    }

    participants.keySet().removeIf(uuid -> !seen.contains(uuid));
  }

  @Nonnull
  public Collection<ParticipantSnapshot> getParticipants(@Nonnull String worldName) {
    Map<UUID, ParticipantSnapshot> participants = worlds.get(worldName);
    if (participants == null) {
      return List.of();
    }
    List<ParticipantSnapshot> list = new ArrayList<>(participants.values());
    list.sort(Comparator.comparing(ParticipantSnapshot::getName, String.CASE_INSENSITIVE_ORDER));
    return list;
  }

  @Nullable
  public ParticipantSnapshot getParticipant(@Nonnull String worldName, @Nonnull UUID uuid) {
    Map<UUID, ParticipantSnapshot> participants = worlds.get(worldName);
    return participants != null ? participants.get(uuid) : null;
  }

  public void clearWorld(@Nonnull String worldName) {
    worlds.remove(worldName);
  }

  private StatSnapshot readStat(@Nullable EntityStatMap statMap, @Nonnull String statId) {
    if (statMap == null) {
      return StatSnapshot.EMPTY;
    }
    EntityStatValue stat = statMap.get(statId);
    if (stat == null) {
      return StatSnapshot.EMPTY;
    }
    return new StatSnapshot(stat.get(), stat.getMax());
  }

  @Nonnull
  private String resolveDisplayName(@Nonnull PlayerRef playerRef) {
    String name = playerRef.getUsername();
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return name == null ? "" : name;
    }
    Store<EntityStore> store = ref.getStore();
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      return name == null ? "" : name;
    }
    String displayName = player.getDisplayName();
    return displayName == null || displayName.isBlank()
        ? (name == null ? "" : name)
        : displayName;
  }

  private static final class StatSnapshot {
    private static final StatSnapshot EMPTY = new StatSnapshot(-1f, -1f);
    private final float current;
    private final float max;

    private StatSnapshot(float current, float max) {
      this.current = current;
      this.max = max;
    }
  }
}
