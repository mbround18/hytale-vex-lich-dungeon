package com.hypixel.hytale.server.core.universe;

import java.util.Objects;
import java.util.UUID;

public class PlayerRef {
  private final String username;
  private final UUID uuid;
  private boolean valid;

  public PlayerRef(String username) {
    this(username, UUID.randomUUID());
  }

  public PlayerRef(String username, UUID uuid) {
    this.username = Objects.requireNonNull(username, "username");
    this.uuid = Objects.requireNonNull(uuid, "uuid");
    this.valid = true;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public String getUsername() {
    return username;
  }

  public UUID getUuid() {
    return uuid;
  }
}
