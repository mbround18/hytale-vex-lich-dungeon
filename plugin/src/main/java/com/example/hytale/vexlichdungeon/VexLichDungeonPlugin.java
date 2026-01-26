package com.example.hytale.vexlichdungeon;

import com.example.hytale.vexlichdungeon.logging.PluginLog;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import javax.annotation.Nonnull;

/**
 * Minimal example plugin that logs during startup and shutdown using the built-in Hytale logger API.
 */
public class VexLichDungeonPlugin extends JavaPlugin {
  private final PluginLog log;

  public VexLichDungeonPlugin(@Nonnull JavaPluginInit init) {
    super(init);
    this.log = PluginLog.forPlugin(this, "VexLichDungeon");
  }

  @Override
  protected void start0() {
    log.lifecycle().atInfo().log("Plugin starting up");
    log.info("Hello, Hytale world! If you see this, the plugin loaded correctly.");
  }

  @Override
  protected void shutdown() {
    log.lifecycle().atInfo().log("Plugin shutting down");
  }
}
