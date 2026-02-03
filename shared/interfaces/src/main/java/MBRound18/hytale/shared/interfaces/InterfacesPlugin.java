package MBRound18.hytale.shared.interfaces;

import java.util.logging.Level;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import MBRound18.hytale.shared.interfaces.commands.FriendListCommand;
import MBRound18.hytale.shared.interfaces.commands.HelloWorldCommand;
import MBRound18.hytale.shared.interfaces.commands.DemoHudCommand;
import MBRound18.hytale.shared.interfaces.debug.interactions.FriendInteractions;
import MBRound18.hytale.shared.interfaces.ui.SharedUiCatalog;

public class InterfacesPlugin extends JavaPlugin {
  public InterfacesPlugin(@Nonnull JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    Level info = Objects.requireNonNull(Level.INFO, "Level.INFO");
    getLogger().at(info).log("InterfacesPlugin setup complete");

    SharedUiCatalog.registerDefaults();

    getCommandRegistry().registerCommand(new DemoHudCommand(getLogger()));
    getCommandRegistry().registerCommand(new HelloWorldCommand());
    getCommandRegistry().registerCommand(new FriendListCommand(FriendInteractions::new));

    getLogger().at(info).log("InterfacesPlugin commands registered\n/helloworld, /friends, /dui, /dhud");
  }

}
