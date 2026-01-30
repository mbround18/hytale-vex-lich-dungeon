package MBRound18.hytale.shared.interfaces;

import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import MBRound18.hytale.shared.interfaces.commands.FriendListCommand;
import MBRound18.hytale.shared.interfaces.commands.HelloWorldCommand;

public class InterfacesPlugin extends JavaPlugin {
  public InterfacesPlugin(@Nonnull JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    getLogger().at(Level.INFO).log("InterfacesPlugin setup complete");

    getCommandRegistry().registerCommand(new HelloWorldCommand());
    getCommandRegistry().registerCommand(new FriendListCommand());

    getLogger().at(Level.INFO).log("InterfacesPlugin commands registered\n/helloworld to test!");
  }

}
