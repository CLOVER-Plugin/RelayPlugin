package yd.kingdom.relayPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import yd.kingdom.relayPlugin.manager.GameManager;

public class GameStartCommand implements CommandExecutor {

    private final GameManager game;

    public GameStartCommand(GameManager game) { this.game = game; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        game.startGame(sender);
        return true;
    }
}