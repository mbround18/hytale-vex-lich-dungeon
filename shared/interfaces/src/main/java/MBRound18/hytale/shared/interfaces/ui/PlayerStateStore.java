package MBRound18.hytale.shared.interfaces.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerStateStore<S, A> {
  public interface Reducer<S, A> {
    @Nonnull
    S reduce(@Nonnull UUID uuid, @Nonnull Ref<EntityStore> ref, @Nonnull S previous, @Nonnull A action);
  }

  public interface StateSerializer<S> {
    @Nonnull
    JsonElement toJson(@Nonnull S state);

    @Nonnull
    S fromJson(@Nonnull JsonElement json);
  }

  private final @Nullable Path storageDir;
  private final Reducer<S, A> reducer;
  private final Supplier<S> initialState;
  private final StateSerializer<S> serializer;
  private final Logger logger;
  private final boolean logErrors;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final ConcurrentHashMap<UUID, S> state = new ConcurrentHashMap<>();

  public PlayerStateStore(@Nullable Path storageDir, @Nonnull Reducer<S, A> reducer,
      @Nonnull Supplier<S> initialState, @Nonnull StateSerializer<S> serializer,
      @Nonnull Logger logger, boolean logErrors) {
    this.storageDir = storageDir;
    this.reducer = Objects.requireNonNull(reducer, "reducer");
    this.initialState = Objects.requireNonNull(initialState, "initialState");
    this.serializer = Objects.requireNonNull(serializer, "serializer");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.logErrors = logErrors;
  }

  public void loadAll() {
    if (storageDir == null) {
      return;
    }
    try {
      Files.createDirectories(storageDir);
    } catch (IOException e) {
      logError("Failed to create player state store directory " + storageDir, e);
      return;
    }
    try {
      Files.list(storageDir).filter(path -> path.toString().endsWith(".json")).forEach(path -> {
        Path safePath = Objects.requireNonNull(path, "path");
        UUID uuid = pathToUuid(safePath);
        if (uuid == null) {
          return;
        }
        S loaded = readState(safePath);
        if (loaded != null) {
          state.put(uuid, loaded);
        }
      });
    } catch (IOException e) {
      logError("Failed to list player state store directory " + storageDir, e);
    }
  }

  public void dispatch(@Nonnull PlayerRef playerRef, @Nonnull A action) {
    if (playerRef == null || !playerRef.isValid()) {
      return;
    }
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return;
    }
    dispatch(playerRef.getUuid(), ref, action);
  }

  public void dispatch(@Nonnull UUID uuid, @Nonnull A action) {
    PlayerRef playerRef = Universe.get().getPlayer(uuid);
    if (playerRef == null || !playerRef.isValid()) {
      return;
    }
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      return;
    }
    dispatch(uuid, ref, action);
  }

  public void dispatch(@Nonnull UUID uuid, @Nonnull Ref<EntityStore> ref, @Nonnull A action) {
    S updated = state.compute(uuid, (key, previous) -> {
      S current = previous == null
          ? Objects.requireNonNull(initialState.get(), "initialState")
          : previous;
      return Objects.requireNonNull(reducer.reduce(uuid, ref, current, action), "state");
    });
    writeState(uuid, Objects.requireNonNull(updated, "state"));
  }

  @Nonnull
  @SuppressWarnings("null")
  public S getOrDefault(@Nonnull UUID uuid) {
    return state.computeIfAbsent(uuid,
        key -> Objects.requireNonNull(initialState.get(), "initialState"));
  }

  public void remove(@Nonnull UUID uuid) {
    state.remove(uuid);
    deleteState(uuid);
  }

  private void writeState(@Nonnull UUID uuid, @Nonnull S state) {
    if (storageDir == null) {
      return;
    }
    try {
      Files.createDirectories(storageDir);
    } catch (IOException e) {
      logError("Failed to create player state store directory " + storageDir, e);
      return;
    }
    Path dir = Objects.requireNonNull(storageDir, "storageDir");
    Path path = dir.resolve(uuid.toString() + ".json");
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      gson.toJson(serializer.toJson(state), writer);
    } catch (IOException e) {
      logError("Failed to write player state store " + path, e);
    }
  }

  private void deleteState(@Nonnull UUID uuid) {
    if (storageDir == null) {
      return;
    }
    Path dir = Objects.requireNonNull(storageDir, "storageDir");
    Path path = dir.resolve(uuid.toString() + ".json");
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      logError("Failed to delete player state store " + path, e);
    }
  }

  @Nullable
  private S readState(@Nonnull Path path) {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      JsonElement root = Objects.requireNonNull(JsonParser.parseReader(reader), "json");
      return serializer.fromJson(root);
    } catch (Exception e) {
      logError("Failed to read player state store " + path, e);
      return null;
    }
  }

  @Nullable
  private static UUID pathToUuid(@Nonnull Path path) {
    String file = path.getFileName().toString();
    if (!file.endsWith(".json")) {
      return null;
    }
    String uuid = file.substring(0, file.length() - 5);
    try {
      return UUID.fromString(uuid);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private void logError(@Nonnull String message, @Nonnull Exception e) {
    if (!logErrors) {
      return;
    }
    logger.log(Level.WARNING, message, e);
  }
}
