package MBRound18.hytale.vexlichdungeon.ui;

import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexdemohudUi;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexleaderboardhudUi;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexportalcountdownhudMinUi;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexportalcountdownhudUi;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexscorehudUi;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexsummaryhudUi;
import MBRound18.hytale.shared.interfaces.ui.generated.VexHudVexwelcomehudUi;
import MBRound18.hytale.shared.interfaces.ui.generated.VexPagesVexdungeonsummaryUi;
import MBRound18.hytale.shared.interfaces.ui.generated.VexPagesVexscoreboardUi;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VexUiCatalog {
  private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
  private static final Map<String, String> UI_PATHS = new LinkedHashMap<>();
  private static final Map<String, String> HUD_PATHS = new LinkedHashMap<>();
  private static final Map<String, String> UI_ALIAS = new LinkedHashMap<>();
  private static final Map<String, String> HUD_ALIAS = new LinkedHashMap<>();
  private static final Set<String> UI_PRIMARY = new LinkedHashSet<>();
  private static final Set<String> HUD_PRIMARY = new LinkedHashSet<>();
  private static final Map<String, Map<String, String>> DEFAULTS = new LinkedHashMap<>();
  private static final VexHudVexscorehudUi VEX_SCORE_HUD = new VexHudVexscorehudUi();
  private static final VexHudVexwelcomehudUi VEX_WELCOME_HUD = new VexHudVexwelcomehudUi();
  private static final VexHudVexsummaryhudUi VEX_SUMMARY_HUD = new VexHudVexsummaryhudUi();
  private static final VexHudVexleaderboardhudUi VEX_LEADERBOARD_HUD = new VexHudVexleaderboardhudUi();
  private static final VexHudVexportalcountdownhudUi VEX_PORTAL_HUD = new VexHudVexportalcountdownhudUi();
  private static final VexHudVexportalcountdownhudMinUi VEX_PORTAL_HUD_MIN = new VexHudVexportalcountdownhudMinUi();
  private static final VexHudVexdemohudUi VEX_DEMO_HUD = new VexHudVexdemohudUi();
  private static final VexPagesVexscoreboardUi VEX_SCOREBOARD_PAGE = new VexPagesVexscoreboardUi();
  private static final VexPagesVexdungeonsummaryUi VEX_SUMMARY_PAGE = new VexPagesVexdungeonsummaryUi();

  private VexUiCatalog() {
  }

  public static void registerDefaults() {
    if (REGISTERED.getAndSet(true)) {
      return;
    }

    registerUi("scoreboard", VexPagesVexscoreboardUi.UI_PATH, "scores");
    registerUi("dungeon-summary", VexPagesVexdungeonsummaryUi.UI_PATH, "summary");

    registerHud("score-hud", VexHudVexscorehudUi.UI_PATH, "score");
    registerHud("welcome-hud", VexHudVexwelcomehudUi.UI_PATH, "welcome");
    registerHud("summary-hud", VexHudVexsummaryhudUi.UI_PATH, "summary-hud");
    registerHud("leaderboard-hud", VexHudVexleaderboardhudUi.UI_PATH, "leaderboard");
    registerHud("portal-countdown-hud", VexHudVexportalcountdownhudUi.UI_PATH,
        "portal");
    registerHud("portal-countdown-hud-min", VexHudVexportalcountdownhudMinUi.UI_PATH,
        "portal-min");
    registerHud("demo-hud", VexHudVexdemohudUi.UI_PATH, "demo");

    Map<String, String> defaults = new LinkedHashMap<>();
    defaults.put(VEX_SCOREBOARD_PAGE.vexContentScoreBody, "1. Alice - 1250\n2. Bob - 980\n3. Chen - 760");
    DEFAULTS.put("scoreboard", defaults);

    defaults = new LinkedHashMap<>();
    defaults.put(VEX_SUMMARY_PAGE.vexContentSummaryStats, "Total Score: 1250 | Time: 12:34");
    defaults.put(VEX_SUMMARY_PAGE.vexContentSummaryBody, "Cleared 5 rooms, defeated the Vex Lich.");
    DEFAULTS.put("dungeon-summary", defaults);

    defaults = new LinkedHashMap<>();
    defaults.put(VEX_SCORE_HUD.vexHudInstanceScore, "Instance: 1250");
    defaults.put(VEX_SCORE_HUD.vexHudPlayerScore, "Player: 420");
    defaults.put(VEX_SCORE_HUD.vexHudDelta, "+120");
    defaults.put(VEX_SCORE_HUD.vexHudPartyList, "You — HP 120/120 | ST 90/90\nAlice — HP 80/100 | ST 70/90");
    DEFAULTS.put("score-hud", defaults);

    defaults = new LinkedHashMap<>();
    defaults.put(VEX_WELCOME_HUD.vexContentVexWelcomeBody, "Prepare yourself. The portal opens soon.");
    DEFAULTS.put("welcome-hud", defaults);

    defaults = new LinkedHashMap<>();
    defaults.put(VEX_SUMMARY_HUD.vexContentVexSummaryStats, "Total Score: 1250");
    defaults.put(VEX_SUMMARY_HUD.vexContentVexSummaryBody, "Alice +620\nBob +430\nYou +200");
    DEFAULTS.put("summary-hud", defaults);

    defaults = new LinkedHashMap<>();
    defaults.put(VEX_LEADERBOARD_HUD.vexContentVexLeaderboardBody, "1. Alice — 1250\n2. Bob — 980\n3. You — 620");
    DEFAULTS.put("leaderboard-hud", defaults);

    defaults = new LinkedHashMap<>();
    defaults.put(VEX_PORTAL_HUD.vexPortalCountdown, "00:20");
    defaults.put(VEX_PORTAL_HUD.vexPortalLocation, "X: 128  Y: 64  Z: -42");
    DEFAULTS.put("portal-countdown-hud", defaults);
    Map<String, String> portalMinDefaults = new LinkedHashMap<>();
    portalMinDefaults.put(VEX_PORTAL_HUD_MIN.vexPortalCountdown, "00:20");
    portalMinDefaults.put(VEX_PORTAL_HUD_MIN.vexPortalLocation, "X: 128  Y: 64  Z: -42");
    DEFAULTS.put("portal-countdown-hud-min", portalMinDefaults);

    defaults = new LinkedHashMap<>();
    defaults.put(VEX_DEMO_HUD.demoScore, "Score: 240");
    defaults.put(VEX_DEMO_HUD.demoTimer, "Time: 1:45");
    defaults.put(VEX_DEMO_HUD.vexDebugStat, "DBG");
    DEFAULTS.put("demo-hud", defaults);
  }

  @Nonnull
  public static Set<String> listUiIds() {
    registerDefaults();
    return Collections.unmodifiableSet(UI_PRIMARY);
  }

  @Nonnull
  public static Set<String> listHudIds() {
    registerDefaults();
    return Collections.unmodifiableSet(HUD_PRIMARY);
  }

  @Nullable
  public static ResolvedTemplate resolve(@Nullable String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    registerDefaults();
    String key = name.trim().toLowerCase(Locale.ROOT);
    String uiPrimary = UI_ALIAS.get(key);
    if (uiPrimary != null) {
      String path = UI_PATHS.get(uiPrimary);
      if (path != null) {
        return new ResolvedTemplate(uiPrimary, path, false);
      }
    }
    String hudPrimary = HUD_ALIAS.get(key);
    if (hudPrimary != null) {
      String path = HUD_PATHS.get(hudPrimary);
      if (path != null) {
        return new ResolvedTemplate(hudPrimary, path, true);
      }
    }
    return null;
  }

  @Nonnull
  public static Map<String, String> defaultVars(@Nonnull String primaryId) {
    registerDefaults();
    Map<String, String> defaults = DEFAULTS.get(primaryId);
    if (defaults == null || defaults.isEmpty()) {
      return Collections.emptyMap();
    }
    return new LinkedHashMap<>(defaults);
  }

  private static void registerUi(@Nonnull String primaryId, @Nonnull String path, @Nonnull String... aliases) {
    UI_PRIMARY.add(primaryId);
    UI_PATHS.put(primaryId, path);
    UI_ALIAS.put(primaryId, primaryId);
    for (String alias : aliases) {
      UI_ALIAS.put(alias, primaryId);
    }
  }

  private static void registerHud(@Nonnull String primaryId, @Nonnull String path, @Nonnull String... aliases) {
    HUD_PRIMARY.add(primaryId);
    HUD_PATHS.put(primaryId, path);
    HUD_ALIAS.put(primaryId, primaryId);
    for (String alias : aliases) {
      HUD_ALIAS.put(alias, primaryId);
    }
  }

  public static final class ResolvedTemplate {
    private final String primaryId;
    private final String path;
    private final boolean hud;

    private ResolvedTemplate(@Nonnull String primaryId, @Nonnull String path, boolean hud) {
      this.primaryId = Objects.requireNonNull(primaryId, "primaryId");
      this.path = Objects.requireNonNull(path, "path");
      this.hud = hud;
    }

    @Nonnull
    public String getPrimaryId() {
      return Objects.requireNonNull(primaryId, "primaryId");
    }

    @Nonnull
    public String getPath() {
      return Objects.requireNonNull(path, "path");
    }

    public boolean isHud() {
      return hud;
    }
  }
}
