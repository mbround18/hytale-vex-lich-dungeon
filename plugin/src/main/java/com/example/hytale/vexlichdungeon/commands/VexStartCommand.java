package com.example.hytale.vexlichdungeon.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class VexStartCommand extends AbstractCommand {
  public VexStartCommand() {
    super("start", "Start Vex The Lich Dungeon (WIP)");
  }

  @Nullable
  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
    context.sendMessage(Message.raw("[Vex] Start requested — test echo!"));
    return CompletableFuture.completedFuture(null);
  }
}
