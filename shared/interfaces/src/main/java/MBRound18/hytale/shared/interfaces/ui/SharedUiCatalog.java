package MBRound18.hytale.shared.interfaces.ui;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

import MBRound18.hytale.shared.interfaces.ui.generated.DemosHudsDemohudobjectivesUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosHudsDemohudpartystatusUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosHudsDemohudquickactionsUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosHudsDemohudstatsUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosHudsDemohudwidgetstripUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemogridUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemoinputsUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemomodalUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemopaginationUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemorowsUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemostatsUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemotabsUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemotoastUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemotoolbarUi;
import MBRound18.hytale.shared.interfaces.ui.generated.DemosPagesDemoutilityUi;
import MBRound18.hytale.shared.interfaces.ui.generated.PagesFriendslistpageUi;
import MBRound18.hytale.shared.interfaces.ui.generated.PagesHelloworldpageUi;
import MBRound18.hytale.shared.interfaces.ui.generated.PagesWidgetexamplesUi;

public final class SharedUiCatalog {
  private static final Set<String> UI_IDS = new LinkedHashSet<>();
  private static final Set<String> HUD_IDS = new LinkedHashSet<>();

  private SharedUiCatalog() {
  }

  public static void registerDefaults() {
    // Demo pages
    registerUi("grid", DemosPagesDemogridUi.UI_PATH);
    registerUi("inputs", DemosPagesDemoinputsUi.UI_PATH);
    registerUi("modal", DemosPagesDemomodalUi.UI_PATH);
    registerUi("pagination", DemosPagesDemopaginationUi.UI_PATH);
    registerUi("rows", DemosPagesDemorowsUi.UI_PATH);
    registerUi("stats", DemosPagesDemostatsUi.UI_PATH);
    registerUi("tabs", DemosPagesDemotabsUi.UI_PATH);
    registerUi("toast", DemosPagesDemotoastUi.UI_PATH);
    registerUi("toolbar", DemosPagesDemotoolbarUi.UI_PATH);
    registerUi("utility", DemosPagesDemoutilityUi.UI_PATH);

    // Core pages
    registerUi("helloworld", PagesHelloworldpageUi.UI_PATH);
    registerUi("friendslist", PagesFriendslistpageUi.UI_PATH);
    registerUi("widgetexamples", PagesWidgetexamplesUi.UI_PATH);
    registerUi("friendscommon", "Friends/FriendsCommon.ui");

    // Macros (optional direct rendering)
    registerUi("macro-layout", "Macros/Layout.ui");
    registerUi("macro-pagination", "Macros/Pagination.ui");
    registerUi("macro-modal", "Macros/Modal.ui");
    registerUi("macro-rows", "Macros/Rows.ui");
    registerUi("macro-utility", "Macros/Utility.ui");
    registerUi("macro-tabs", "Macros/Tabs.ui");
    registerUi("macro-toolbar", "Macros/Toolbar.ui");
    registerUi("macro-stats", "Macros/Stats.ui");
    registerUi("macro-toast", "Macros/Toast.ui");
    registerUi("macro-grid", "Macros/Grid.ui");
    registerUi("macro-inputs", "Macros/Inputs.ui");

    // Demo HUDs
    registerHud("widgetstrip", DemosHudsDemohudwidgetstripUi.UI_PATH);
    registerHud("objectives", DemosHudsDemohudobjectivesUi.UI_PATH);
    registerHud("partystatus", DemosHudsDemohudpartystatusUi.UI_PATH);
    registerHud("quickactions", DemosHudsDemohudquickactionsUi.UI_PATH);
    registerHud("stats", DemosHudsDemohudstatsUi.UI_PATH);
  }

  public static boolean isSharedUiId(String id) {
    return UI_IDS.contains(id);
  }

  public static boolean isSharedHudId(String id) {
    return HUD_IDS.contains(id);
  }

  private static void registerUi(String id, String path) {
    String safeId = Objects.requireNonNull(id, "id");
    String safePath = Objects.requireNonNull(path, "path");
    UI_IDS.add(safeId);
    UiRegistry.register(new UiTemplate(safeId, safePath,
        Objects.requireNonNull(List.<String>of(), "vars")));
  }

  private static void registerHud(String id, String path) {
    String safeId = Objects.requireNonNull(id, "id");
    String safePath = Objects.requireNonNull(path, "path");
    HUD_IDS.add(safeId);
    HudRegistry.register(new UiTemplate(safeId, safePath,
        Objects.requireNonNull(List.<String>of(), "vars")));
  }
}
