package MBRound18.hytale.dungeonmaster.handlers;

import MBRound18.hytale.dungeonmaster.helpers.WebContext;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public final class OpenApiHandler {
  private final @Nonnull Gson gson;

  public OpenApiHandler(@Nonnull Gson gson) {
    this.gson = java.util.Objects.requireNonNull(gson, "gson");
  }

  public void handle(@Nonnull HttpExchange exchange) throws IOException {
    Map<String, Object> spec = new LinkedHashMap<>();
    spec.put("openapi", "3.0.3");
    spec.put("info", Map.of(
        "title", "DungeonMaster Debug API",
        "version", "0.1.0",
        "description", "Developer debug endpoints for the Hytale Vex Lich Dungeon stack."));
    spec.put("servers", List.of(Map.of("url", "/")));

    Map<String, Object> schemas = new LinkedHashMap<>();
    schemas.put("EventEnvelope", objectSchema(Map.of(
        "id", Map.of("type", "integer", "format", "int64"),
        "timestamp", Map.of("type", "integer", "format", "int64"),
        "type", Map.of("type", "string"),
        "payload", Map.of("type", "object"))));
    schemas.put("EventsPollResponse", objectSchema(Map.of(
        "events", arraySchema(schemaRef("EventEnvelope")),
        "nextSince", Map.of("type", "integer", "format", "int64"))));
    schemas.put("EventTypes", arraySchema(Map.of("type", "string")));
    schemas.put("PlayerPosition", objectSchema(Map.of(
        "x", Map.of("type", "number", "format", "double"),
        "y", Map.of("type", "number", "format", "double"),
        "z", Map.of("type", "number", "format", "double"))));
    schemas.put("Player", objectSchema(Map.of(
        "uuid", Map.of("type", "string"),
        "name", Map.of("type", "string"),
        "world", Map.of("type", "string"),
        "position", Map.of("allOf", List.of(schemaRef("PlayerPosition")), "nullable", true)),
        List.of("uuid", "name", "world")));
    schemas.put("Players", arraySchema(schemaRef("Player")));
    schemas.put("WorldMetadata", objectSchema(Map.of(
        "name", Map.of("type", "string"),
        "playerCount", Map.of("type", "integer", "format", "int32"))));
    schemas.put("Worlds", arraySchema(schemaRef("WorldMetadata")));
    schemas.put("PrefabBounds", objectSchema(Map.of(
        "minX", Map.of("type", "integer", "format", "int32"),
        "maxX", Map.of("type", "integer", "format", "int32"),
        "minY", Map.of("type", "integer", "format", "int32"),
        "maxY", Map.of("type", "integer", "format", "int32"),
        "minZ", Map.of("type", "integer", "format", "int32"),
        "maxZ", Map.of("type", "integer", "format", "int32"))));
    schemas.put("PrefabRoomSize", objectSchema(Map.of(
        "w", Map.of("type", "integer", "format", "int32"),
        "h", Map.of("type", "integer", "format", "int32"))));
    schemas.put("PrefabMetadata", objectSchema(Map.of(
        "prefabId", Map.of("type", "string"),
        "width", Map.of("type", "integer", "format", "int32"),
        "depth", Map.of("type", "integer", "format", "int32"),
        "roomSize", schemaRef("PrefabRoomSize"),
        "bounds", schemaRef("PrefabBounds"))));
    schemas.put("Stats", objectSchema(Map.of(
        "system", objectSchema(Map.of(
            "uptime_ms", Map.of("type", "integer", "format", "int64"),
            "threads_active", Map.of("type", "integer", "format", "int32"),
            "memory_free", Map.of("type", "integer", "format", "int64"),
            "memory_total", Map.of("type", "integer", "format", "int64"),
            "memory_max", Map.of("type", "integer", "format", "int64"))),
        "worlds", arraySchema(objectSchema(Map.of(
            "name", Map.of("type", "string"),
            "players", Map.of("type", "integer", "format", "int32"),
            "loaded_chunks", Map.of("type", "integer", "format", "int32")))),
        "events", objectSchema(Map.of(
            "registered_types", Map.of("type", "integer", "format", "int32"),
            "clients_connected", Map.of("type", "integer", "format", "int32"),
            "buffer_size", Map.of("type", "integer", "format", "int32"))))));

    spec.put("components", Map.of("schemas", schemas));

    Map<String, Object> paths = new LinkedHashMap<>();
    paths.put("/api/health", Map.of("get", Map.of(
        "summary", "Health check",
        "responses", Map.of("200", textResponse("ok")))));
    paths.put("/api/events", Map.of("get", Map.of(
        "summary", "SSE event stream",
        "parameters", List.of(
            Map.of("name", "types", "in", "query", "description", "Comma-separated event class names",
                "schema", Map.of("type", "string"))),
        "responses", Map.of("200", Map.of(
            "description", "Server-Sent Events stream",
            "content", Map.of("text/event-stream", Map.of(
                "schema", Map.of("type", "string"),
                "example", "event: MBRound18.hytale.events.WorldCreated\\ndata: {\\\"id\\\":1}\\n\\n")))))));
    paths.put("/api/events/poll", Map.of("get", Map.of(
        "summary", "Poll events",
        "parameters", List.of(
            Map.of("name", "since", "in", "query", "schema", Map.of("type", "integer", "format", "int64")),
            Map.of("name", "limit", "in", "query", "schema", Map.of("type", "integer", "format", "int32"))),
        "responses", Map.of("200", jsonResponse("Event snapshot", schemaRef("EventsPollResponse"))))));
    paths.put("/api/events/types", Map.of("get", Map.of(
        "summary", "List event types",
        "responses", Map.of("200", jsonResponse("Registered event type names", schemaRef("EventTypes"))))));
    paths.put("/api/stats", Map.of("get", Map.of(
        "summary", "Server stats",
        "responses", Map.of("200", jsonResponse("Runtime stats", schemaRef("Stats"))))));
    paths.put("/api/metadata/players", Map.of("get", Map.of(
        "summary", "Player metadata",
        "responses", Map.of("200", jsonResponse("Players list", schemaRef("Players"))))));
    paths.put("/api/metadata/worlds", Map.of("get", Map.of(
        "summary", "World metadata",
        "responses", Map.of("200", jsonResponse("World list", schemaRef("Worlds"))))));
    paths.put("/api/metadata/prefab/{prefabId}", Map.of("get", Map.of(
        "summary", "Prefab metadata",
        "parameters", List.of(
            Map.of("name", "prefabId", "in", "path", "required", true, "schema", Map.of("type", "string"))),
        "responses", Map.of(
            "200", jsonResponse("Prefab metadata", schemaRef("PrefabMetadata")),
            "400", textResponse("Missing prefab id"),
            "404", textResponse("Prefab not found"),
            "500", textResponse("Internal Server Error")))));
    spec.put("paths", paths);

    new WebContext(exchange, gson).json(spec);
  }

  private static Map<String, Object> schemaRef(String name) {
    return Map.of("$ref", "#/components/schemas/" + name);
  }

  private static Map<String, Object> arraySchema(Map<String, Object> items) {
    return Map.of("type", "array", "items", items);
  }

  private static Map<String, Object> objectSchema(Map<String, Object> properties) {
    return objectSchema(properties, null);
  }

  private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    if (required != null && !required.isEmpty()) {
      schema.put("required", required);
    }
    return schema;
  }

  private static Map<String, Object> jsonResponse(String description, Map<String, Object> schema) {
    return Map.of(
        "description", description,
        "content", Map.of("application/json", Map.of("schema", schema)));
  }

  private static Map<String, Object> textResponse(String description) {
    return Map.of(
        "description", description,
        "content", Map.of("text/plain", Map.of("schema", Map.of("type", "string"))));
  }
}
