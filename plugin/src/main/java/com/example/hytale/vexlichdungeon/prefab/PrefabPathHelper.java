package com.example.hytale.vexlichdungeon.prefab;

import javax.annotation.Nonnull;

/**
 * Helper utility for converting asset-relative prefab paths to mod-relative
 * paths
 * that the Hytale prefab system recognizes.
 * 
 * <p>
 * Example:
 * 
 * <pre>
 * // Asset path: assets/Server/Prefabs/Base/Vex_Courtyard_Base.prefab.json
 * // Mod path: Mods/VexLichDungeon/Base/Vex_Courtyard_Base.prefab.json
 * String modPath = PrefabPathHelper.toModPath("Base/Vex_Courtyard_Base.prefab.json");
 * </pre>
 */
public final class PrefabPathHelper {

  // No mod root prefix needed - paths are relative to Server/Prefabs/ in asset pack
  private static final String MOD_ROOT = "";
  private static final String PREFAB_EXTENSION = ".prefab.json";

  private PrefabPathHelper() {
    // Utility class - prevent instantiation
  }

  /**
   * Converts an asset-relative prefab path to a mod-relative path.
   * 
   * @param assetRelativePath Path relative to assets/Server/Prefabs/
   *                          (e.g., "Base/Vex_Courtyard_Base.prefab.json")
   * @return Full mod-relative path (e.g.,
   *         "Mods/VexLichDungeon/Base/Vex_Courtyard_Base.prefab.json")
   */
  @Nonnull
  public static String toModPath(@Nonnull String assetRelativePath) {
    // Strip leading slash if present
    String cleaned = assetRelativePath.startsWith("/")
        ? assetRelativePath.substring(1)
        : assetRelativePath;

    // Ensure .prefab.json extension
    if (!cleaned.endsWith(PREFAB_EXTENSION)) {
      cleaned += PREFAB_EXTENSION;
    }

    return MOD_ROOT + cleaned;
  }

  /**
   * Builds a mod path for a base prefab.
   * 
   * @param prefabName Name of the prefab file (with or without extension)
   * @return Full mod path to the base prefab
   */
  @Nonnull
  public static String toBasePath(@Nonnull String prefabName) {
    return toModPath("Base/" + stripExtension(prefabName));
  }

  /**
   * Builds a mod path for a gate prefab.
   * 
   * @param prefabName Name of the prefab file (with or without extension)
   * @return Full mod path to the gate prefab
   */
  @Nonnull
  public static String toGatePath(@Nonnull String prefabName) {
    return toModPath("Gates/" + stripExtension(prefabName));
  }

  /**
   * Builds a mod path for a room prefab.
   * 
   * @param prefabName Name of the prefab file (with or without extension)
   * @return Full mod path to the room prefab
   */
  @Nonnull
  public static String toRoomPath(@Nonnull String prefabName) {
    return toModPath("Rooms/" + stripExtension(prefabName));
  }

  /**
   * Builds a mod path for a hallway prefab.
   * 
   * @param prefabName Name of the prefab file (with or without extension)
   * @return Full mod path to the hallway prefab
   */
  @Nonnull
  public static String toHallwayPath(@Nonnull String prefabName) {
    return toModPath("Hallways/" + stripExtension(prefabName));
  }

  /**
   * Builds a mod path for a decoration prefab.
   * 
   * @param prefabName Name of the prefab file (with or without extension)
   * @return Full mod path to the decoration prefab
   */
  @Nonnull
  public static String toDecorationPath(@Nonnull String prefabName) {
    return toModPath("Decoration/" + stripExtension(prefabName));
  }

  /**
   * Gets the path to the blocked gate prefab (used for outer edges).
   * 
   * @return Full mod path to the blocked gate
   */
  @Nonnull
  public static String getBlockedGatePath() {
    return toGatePath("Vex_Seperator_Gate_Blocked");
  }

  /**
   * Gets the path to the base courtyard prefab (dungeon start).
   * 
   * @return Full mod path to the courtyard base
   */
  @Nonnull
  public static String getCourtYardBasePath() {
    return toBasePath("Vex_Courtyard_Base");
  }

  /**
   * Strips the .prefab.json extension from a filename if present.
   * 
   * @param filename Filename with or without extension
   * @return Filename without extension
   */
  @Nonnull
  private static String stripExtension(@Nonnull String filename) {
    if (filename.endsWith(PREFAB_EXTENSION)) {
      return filename.substring(0, filename.length() - PREFAB_EXTENSION.length());
    }
    return filename;
  }

  /**
   * Extracts just the prefab name from a full mod path.
   * 
   * @param modPath Full mod path (e.g.,
   *                "Mods/VexLichDungeon/Base/Vex_Courtyard_Base.prefab.json")
   * @return Just the filename (e.g., "Vex_Courtyard_Base")
   */
  @Nonnull
  public static String extractPrefabName(@Nonnull String modPath) {
    int lastSlash = modPath.lastIndexOf('/');
    String filename = (lastSlash >= 0) ? modPath.substring(lastSlash + 1) : modPath;
    return stripExtension(filename);
  }
}
