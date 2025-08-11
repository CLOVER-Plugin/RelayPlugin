package yd.kingdom.relayPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import yd.kingdom.relayPlugin.manager.GameManager;

public class GameStopCommand implements CommandExecutor {

    private final GameManager game;

    public GameStopCommand(GameManager game) { this.game = game; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        game.forceStopGame(false);
        return true;
    }
}