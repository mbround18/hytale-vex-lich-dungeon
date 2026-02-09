package MBRound18.hytale.dungeonmaster.handlers;

import MBRound18.ImmortalEngine.api.prefab.PrefabInspector;
import MBRound18.hytale.dungeonmaster.helpers.WebContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import javax.annotation.Nonnull;

public final class PrefabMetadataHandler {
  private final @Nonnull Gson gson;
  private final @Nonnull PrefabInspector prefabInspector;

  public PrefabMetadataHandler(@Nonnull Gson gson, @Nonnull PrefabInspector prefabInspector) {
    this.gson = java.util.Objects.requireNonNull(gson, "gson");
    this.prefabInspector = java.util.Objects.requireNonNull(prefabInspector, "prefabInspector");
  }

  public void handle(@Nonnull HttpExchange exchange) throws IOException {
    WebContext ctx = new WebContext(exchange, gson);
    String prefabId = ctx.pathParam("/api/metadata/prefab/");

    if (prefabId == null || prefabId.isBlank()) {
      ctx.text(400, "Missing prefab id");
      return;
    }

    try {
      PrefabInspector.PrefabDimensions dims = prefabInspector.getPrefabDimensions(prefabId);
      if (dims == null) {
        ctx.text(404, "Prefab not found");
        return;
      }

      JsonObject payload = new JsonObject();
      payload.addProperty("prefabId", prefabId);
      payload.addProperty("width", dims.width);
      payload.addProperty("depth", dims.depth);

      JsonObject roomSize = new JsonObject();
      roomSize.addProperty("w", dims.width);
      roomSize.addProperty("h", dims.depth);
      payload.add("roomSize", roomSize);

      JsonObject bounds = new JsonObject();
      bounds.addProperty("minX", dims.minX);
      bounds.addProperty("maxX", dims.maxX);
      bounds.addProperty("minY", dims.minY);
      bounds.addProperty("maxY", dims.maxY);
      bounds.addProperty("minZ", dims.minZ);
      bounds.addProperty("maxZ", dims.maxZ);
      payload.add("bounds", bounds);

      ctx.json(payload);
    } catch (Exception e) {
      ctx.text(500, "Internal Server Error");
    }
  }
}
