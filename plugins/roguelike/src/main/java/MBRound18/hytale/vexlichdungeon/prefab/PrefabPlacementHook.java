package MBRound18.hytale.vexlichdungeon.prefab;

import com.hypixel.hytale.math.vector.Vector3i;

public class PrefabPlacementHook implements PrefabHook {
  @Override
  public void afterPlace(PrefabPlaceContext context) {
    if (context == null) {
      return;
    }
    String worldName = context.getWorld().getName();
    String prefabPath = context.getPrefabPath();
    Vector3i origin = context.getOrigin();
    PrefabPlacement placement = new PrefabPlacement(worldName, prefabPath, origin,
        context.getRotationDegrees(), context.isGate(), System.currentTimeMillis());
    PrefabPlacementRegistry.add(placement);
  }
}
