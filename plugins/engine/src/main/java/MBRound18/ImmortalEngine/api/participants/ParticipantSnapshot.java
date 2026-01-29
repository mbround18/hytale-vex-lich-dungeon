package MBRound18.ImmortalEngine.api.participants;

import java.util.UUID;
import javax.annotation.Nonnull;

public final class ParticipantSnapshot {
  private final UUID uuid;
  private final String name;
  private final float health;
  private final float healthMax;
  private final float stamina;
  private final float staminaMax;
  private final long updatedAt;

  public ParticipantSnapshot(@Nonnull UUID uuid, @Nonnull String name, float health, float healthMax,
      float stamina, float staminaMax, long updatedAt) {
    this.uuid = uuid;
    this.name = name;
    this.health = health;
    this.healthMax = healthMax;
    this.stamina = stamina;
    this.staminaMax = staminaMax;
    this.updatedAt = updatedAt;
  }

  @Nonnull
  public UUID getUuid() {
    return uuid;
  }

  @Nonnull
  public String getName() {
    return name;
  }

  public float getHealth() {
    return health;
  }

  public float getHealthMax() {
    return healthMax;
  }

  public float getStamina() {
    return stamina;
  }

  public float getStaminaMax() {
    return staminaMax;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }
}
