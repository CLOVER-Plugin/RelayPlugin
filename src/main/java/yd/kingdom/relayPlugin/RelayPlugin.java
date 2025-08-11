package yd.kingdom.relayPlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import yd.kingdom.relayPlugin.command.BlueTeamCommand;
import yd.kingdom.relayPlugin.command.GameStartCommand;
import yd.kingdom.relayPlugin.command.GameStopCommand;
import yd.kingdom.relayPlugin.command.RedTeamCommand;
import yd.kingdom.relayPlugin.listener.ItemProgressListener;
import yd.kingdom.relayPlugin.manager.GameManager;
import yd.kingdom.relayPlugin.manager.TeamManager;
import yd.kingdom.relayPlugin.service.ScoreboardService;

public class RelayPlugin extends JavaPlugin {

    private static RelayPlugin instance;
    private TeamManager teamManager;
    private GameManager gameManager;
    private ScoreboardService scoreboardService;

    public static RelayPlugin inst() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        teamManager = new TeamManager();
        scoreboardService = new ScoreboardService(this, teamManager);
        gameManager = new GameManager(this, teamManager, scoreboardService);

        // Commands
        getCommand("레드팀").setExecutor(new RedTeamCommand(teamManager, scoreboardService));
        getCommand("블루팀").setExecutor(new BlueTeamCommand(teamManager, scoreboardService));
        getCommand("게임시작").setExecutor(new GameStartCommand(gameManager));
        getCommand("게임종료").setExecutor(new GameStopCommand(gameManager));

        // Listeners
        Bukkit.getPluginManager().registerEvents(new ItemProgressListener(this, gameManager), this);

        getLogger().info("야생 릴레이 플러그인 활성화");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.forceStopGame(true);
        getLogger().info("야생 릴레이 플러그인 비활성화");
    }

    public TeamManager team() { return teamManager; }
    public GameManager game() { return gameManager; }
    public ScoreboardService board() { return scoreboardService; }
}