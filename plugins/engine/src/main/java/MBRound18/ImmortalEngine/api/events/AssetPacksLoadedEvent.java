package MBRound18.ImmortalEngine.api.events;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Event fired when asset packs have finished loading and are ready for use.
 * Plugins can listen to this event to trigger index rebuilds and other
 * asset-dependent initialization.
 */
public final class AssetPacksLoadedEvent extends DebugEvent {

  public AssetPacksLoadedEvent() {
  }

  @Override
  public Object toPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("event", "AssetPacksLoaded");
    return withCorrelation(data);
  }
}
