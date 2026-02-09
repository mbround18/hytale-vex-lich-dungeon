# Event Debug Serialization Plan

## Goals

- Standardize per-event debug serialization without unsafe reflection on JDK internals.
- Allow event classes to own payload shape, including optional async resolution via `CompletableFuture`.
- Provide a consistent default path when no custom payload is supplied.

## Proposed Base Class: `DebugEvent`

Add optional helpers and defaults directly on `DebugEvent` so event classes can opt in.

### New Methods on `DebugEvent`

- `Object toPayload()`
  - Default: `null` (falls back to generic serialization).
  - Override to return a POJO/Map/Json-like payload.
  - May return `CompletableFuture<?>` to allow game-thread resolution.

- `JsonElement toJsonPayload()`
  - Calls a shared safe serializer against `toPayload()`.
  - If `toPayload()` is `null`, uses a safe reflection fallback that only reads public getters/fields.

- `Map<String, Object> debugFields()` (optional helper)
  - Convenience builder for simple metadata.
  - Returns a mutable map that will be safely serialized.

- `static CompletableFuture<Object> completedPayload(Object payload)`
  - Already present; keep for convenience.

### Optional Static Utilities (Engine)

- `DebugEvent.safeSerialize(Object src)`
  - Minimal dependency on Gson (or a tiny abstraction).
  - Uses:
    - Known adapters (World/Player/Entity) for metadata only.
    - Safe reflection on public getters/fields.
    - Exclusion rules for Java internals.

## Event Class Usage Patterns

### 1) Simple Synchronous Payload

```java
public final class BossDefeatedEvent extends DebugEvent {
  // fields...
  @Override
  public Object toPayload() {
    return Map.of(
      "world", DebugEvent.worldMeta(getWorld()),
      "entityId", getEntityId().toString(),
      "room", getRoom()
    );
  }
}
```

### 2) Async Payload via Game Thread

```java
@Override
public Object toPayload() {
  return world.execute(() -> Map.of(
    "world", DebugEvent.worldMeta(world),
    "playerCount", world.getPlayerCount()
  ));
}
```

### 3) Getter-Based Default (No override)

If a class does nothing, the serializer falls back to public getters (`get*`, `is*`) and public fields.

## How Debug Server Would Use It

1. When serializing an `IEvent`:

- If it’s a `DebugEvent`, call `toJsonPayload()`.
- Else, try generic safe serialization (current helper fallback).

1. If the payload is a `CompletableFuture`, wait up to a short timeout (e.g. 50ms). If it times out, emit a minimal placeholder.

## Suggested Engine Helpers (Optional)

- `DebugEvent.worldMeta(World world)` → `{ name, id, dimension, playerCount }`
- `DebugEvent.playerMeta(PlayerRef ref)` → `{ uuid, name }`
- `DebugEvent.entityMeta(Entity entity)` → `{ uuid/id, type }`

## Open Questions

- Should `DebugEvent` depend directly on Gson, or should it return a `Map<String, Object>` and let the server serialize it?
- What timeout should async payloads use by default (50ms vs configurable)?
- Should we allow a per-event timeout override?

## Next Steps (If Approved)

- Implement the helper methods on `DebugEvent`.
- Update event serialization to prefer `DebugEvent` pathways.
- Optionally add default adapters for World/Player/Entity metadata.
