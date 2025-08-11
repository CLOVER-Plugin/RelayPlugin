package yd.kingdom.relayPlugin.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import yd.kingdom.relayPlugin.manager.TeamManager;

import java.util.List;
import java.util.UUID;

public class ScoreboardService {

    private final Plugin plugin;
    private final TeamManager team;

    public ScoreboardService(Plugin plugin, TeamManager team) {
        this.plugin = plugin;
        this.team = team;
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyBoard(p);
        }
    }

    private void applyBoard(Player p) {
        ScoreboardManager m = Bukkit.getScoreboardManager();
        if (m == null) return;

        Scoreboard board = m.getNewScoreboard();
        // (호환성을 위해 오래된 시그니처 사용)
        Objective obj = board.registerNewObjective("relay", "dummy", ChatColor.GOLD + "팀 릴레이");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;
        line = addTeamSection(obj, "§c[레드팀]", TeamManager.Team.RED, line);
        line = addTeamSection(obj, "§9[블루팀]", TeamManager.Team.BLUE, line);

        p.setScoreboard(board);
    }

    private int addTeamSection(Objective obj, String title, TeamManager.Team t, int line) {
        if (line <= 0) return line;
        line = addLine(obj, title, line);

        List<UUID> order = team.getOrder(t);
        for (int i = 0; i < order.size() && line > 0; i++) {
            String name = team.getPlayer(order.get(i)).map(Player::getName).orElse("오프라인");
            String entry = " " + (i + 1) + ". " + name;
            line = addLine(obj, entry, line);
        }
        return line;
    }

    private int addLine(Objective obj, String text, int line) {
        String safe = text.length() > 40 ? text.substring(0, 40) : text;
        obj.getScore(safe).setScore(line);
        return line - 1;
    }
}