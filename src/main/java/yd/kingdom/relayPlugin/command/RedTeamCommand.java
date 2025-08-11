package yd.kingdom.relayPlugin.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yd.kingdom.relayPlugin.manager.TeamManager;
import yd.kingdom.relayPlugin.service.ScoreboardService;

public class RedTeamCommand implements CommandExecutor {

    private final TeamManager teamManager;
    private final ScoreboardService board;

    public RedTeamCommand(TeamManager teamManager, ScoreboardService board) {
        this.teamManager = teamManager;
        this.board = board;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }
        teamManager.joinTeam(p, TeamManager.Team.RED);
        p.sendMessage("§a팀 변경: " + ChatColor.RED + "레드팀");
        board.updateAll();
        return true;
    }
}