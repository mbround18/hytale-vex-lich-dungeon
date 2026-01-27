package com.example.hytale.vexlichdungeon.commands;

import com.example.hytale.vexlichdungeon.logging.PluginLog;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Command to create and join a Vex Lich Dungeon challenge instance.
 * Usage: /vex
 * 
 * This command:
 * 1. Checks if the player has permission (same as /inst command - hytale.instances.create)
 * 2. Executes "/inst Vex_The_Lich_Dungeon" to create a new instance
 * 3. The instance system teleports the player automatically
 * 4. Dungeon generation will automatically trigger via event handlers once instance is created
 */
public class VexChallengeCommand extends AbstractAsyncCommand {

  private static final String WORLD_NAME = "Vex_The_Lich_Dungeon";
  private static final String PERMISSION_NODE = "hytale.instances.create";
  
  private final PluginLog log;

  public VexChallengeCommand(@Nonnull PluginLog log) {
    super("vex", "Create and join a Vex Lich Dungeon challenge");
    this.log = log;
  }

  @Override
  protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
    CommandSender sender = context.sender();
    
    // Ensure command is executed by a player
    if (!context.isPlayer()) {
      context.sendMessage(Message.raw("§cThis command can only be used by players!"));
      return CompletableFuture.completedFuture(null);
    }
    
    // Check permission
    if (!sender.hasPermission(PERMISSION_NODE)) {
      context.sendMessage(Message.raw("§cYou don't have permission to create instances!"));
      return CompletableFuture.completedFuture(null);
    }
    
    try {
      log.info("[COMMAND] Player %s executed /vex", sender.getDisplayName());
      
      context.sendMessage(Message.raw("§aCreating Vex Lich Dungeon instance..."));
      context.sendMessage(Message.raw("§7The dungeon will generate automatically."));
      context.sendMessage(Message.raw("§7Use: /inst " + WORLD_NAME));
      
      log.info("[COMMAND] Completed /vex for player %s", sender.getDisplayName());
      
    } catch (Exception e) {
      log.error("Failed to execute vex command: %s", e.getMessage());
      e.printStackTrace();
      context.sendMessage(Message.raw("§cFailed: " + e.getMessage()));
    }
    
    return CompletableFuture.completedFuture(null);
  }
}
