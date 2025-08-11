package yd.kingdom.relayPlugin.manager;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import yd.kingdom.relayPlugin.service.ScoreboardService;
import yd.kingdom.relayPlugin.util.InventoryUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GameManager {

    private final Plugin plugin;
    private final TeamManager team;
    private final ScoreboardService board;

    private final List<Material> objectives = new ArrayList<>();
    private boolean running = false;
    private boolean finished = false;
    private TeamManager.Team winner = null;

    private final Map<TeamManager.Team, Integer> step = new EnumMap<>(TeamManager.Team.class);
    private final Map<TeamManager.Team, Long> startNano = new EnumMap<>(TeamManager.Team.class);
    private final Map<TeamManager.Team, Long> endNano = new EnumMap<>(TeamManager.Team.class);

    private BukkitRunnable actionBarTask;

    public GameManager(Plugin plugin, TeamManager team, ScoreboardService board) {
        this.plugin = plugin;
        this.team = team;
        this.board = board;
        loadObjectivesFromConfig();
    }

    private void loadObjectivesFromConfig() {
        objectives.clear();
        List<String> list = plugin.getConfig().getStringList("objectives");
        if (list == null || list.isEmpty())
            list = Arrays.asList("DIAMOND", "NETHERITE_INGOT", "NETHERITE_SWORD", "DRAGON_EGG");

        for (String s : list) {
            Material m = Material.matchMaterial(s.trim());
            if (m == null) {
                plugin.getLogger().warning("Unknown material in config: " + s + " (skip)");
                continue;
            }
            objectives.add(m);
        }
        if (objectives.isEmpty()) throw new IllegalStateException("No valid objectives.");
    }

    public synchronized boolean isRunning() { return running; }

    public synchronized void startGame(CommandSender sender) {
        if (running) { sender.sendMessage("§c이미 게임이 진행 중입니다."); return; }
        if (team.size(TeamManager.Team.RED) == 0 || team.size(TeamManager.Team.BLUE) == 0) {
            sender.sendMessage("§c양 팀 모두 최소 1명 이상이어야 합니다.");
            return;
        }

        finished = false; winner = null;
        step.put(TeamManager.Team.RED, 0);
        step.put(TeamManager.Team.BLUE, 0);
        startNano.put(TeamManager.Team.RED, System.nanoTime());
        startNano.put(TeamManager.Team.BLUE, System.nanoTime());
        endNano.clear();

        applyModesForAll();

        int period = Math.max(1, plugin.getConfig().getInt("actionbarPeriodTicks", 10));
        actionBarTask = new BukkitRunnable() { @Override public void run() { tickActionBars(); } };
        actionBarTask.runTaskTimer(plugin, 0L, period);

        board.updateAll();
        running = true;

        Bukkit.broadcastMessage("§a팀 릴레이 시작! 목표: §f" +
                objectives.stream().map(Enum::name).collect(Collectors.joining(" §7→ §f")));

        checkAllActiveInventories();
    }

    public synchronized void forceStopGame(boolean silent) {
        if (actionBarTask != null) { actionBarTask.cancel(); actionBarTask = null; }
        if (!running && !finished) return;

        for (UUID id : team.allPlayers()) {
            team.getPlayer(id).ifPresent(p -> p.setGameMode(GameMode.SURVIVAL));
        }
        running = false;
        if (!silent) Bukkit.broadcastMessage("§c게임이 종료되었습니다.");
    }

    public synchronized void stopGameWithWinner(TeamManager.Team winTeam) {
        if (finished) return;
        finished = true; winner = winTeam;
        if (actionBarTask != null) { actionBarTask.cancel(); actionBarTask = null; }

        long now = System.nanoTime();
        endNano.put(winTeam, now);

        for (UUID id : team.allPlayers()) {
            team.getPlayer(id).ifPresent(p -> p.setGameMode(GameMode.ADVENTURE));
        }

        String timeStr = formatElapsed(startNano.get(winTeam), endNano.get(winTeam));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(
                    (winTeam == TeamManager.Team.RED ? "§c레드팀 승리!" : "§9블루팀 승리!"),
                    "§f총 시간: §e" + timeStr,
                    10, 70, 20
            );
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        running = false;
    }

    private void tickActionBars() {
        for (TeamManager.Team t : TeamManager.Team.values()) {
            Long start = startNano.get(t);
            if (start == null) continue;
            boolean done = endNano.containsKey(t);
            long end = done ? endNano.get(t) : System.nanoTime();
            String msg = (t == TeamManager.Team.RED ? "§c레드 §7" : "§9블루 §7") +
                    "경과: §e" + formatElapsed(start, end) + (done ? " §a(완료)" : "");
            for (UUID id : team.getOrder(t)) {
                team.getPlayer(id).ifPresent(p ->
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg)));
            }
        }
    }

    private String formatElapsed(long start, long end) {
        long ns = Math.max(0, end - start);
        long ms = TimeUnit.NANOSECONDS.toMillis(ns);
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;
        long tenths  = (ms % 1000) / 100;
        return String.format("%02d:%02d.%d", minutes, seconds, tenths);
    }

    private void applyModesForAll() {
        for (TeamManager.Team t : TeamManager.Team.values()) {
            List<UUID> order = new ArrayList<>(team.getOrder(t));
            int runnerIndex = getRunnerIndex(t);
            for (int i = 0; i < order.size(); i++) {
                UUID id = order.get(i);
                int idx = i;
                team.getPlayer(id).ifPresent(p ->
                        p.setGameMode(idx == runnerIndex ? GameMode.SURVIVAL : GameMode.SPECTATOR));
            }
        }
    }

    private int getRunnerIndex(TeamManager.Team t) {
        List<UUID> order = team.getOrder(t);
        if (order.isEmpty()) return -1;
        int s = step.getOrDefault(t, 0);
        return s % order.size();
    }

    private Material getRequired(TeamManager.Team t) {
        int s = step.getOrDefault(t, 0);
        if (s < 0 || s >= objectives.size()) return null;
        return objectives.get(s);
    }

    public void checkAllActiveInventories() {
        for (TeamManager.Team t : TeamManager.Team.values()) {
            int idx = getRunnerIndex(t);
            team.getPlayerByIndex(t, idx).ifPresent(this::tryCompleteIfHasObjective);
        }
    }

    public synchronized void tryCompleteIfHasObjective(Player p) {
        if (!running || finished) return;
        TeamManager.Team t = team.getTeam(p.getUniqueId());
        if (t == null) return;

        int runnerIdx = getRunnerIndex(t);
        Optional<Player> runner = team.getPlayerByIndex(t, runnerIdx);
        if (runner.isEmpty() || !runner.get().getUniqueId().equals(p.getUniqueId())) return;

        Material need = getRequired(t);
        if (need == null) return;

        if (InventoryUtil.hasMaterial(p.getInventory(), need)) {
            advanceTeam(t, p);
        }
    }

    private void advanceTeam(TeamManager.Team t, Player currentRunner) {
        int s = step.getOrDefault(t, 0);
        int participants = Math.max(1, team.size(t));
        boolean lastObjective = (s == objectives.size() - 1);

        int nextRunnerIndex = (s + 1) % participants;
        Optional<Player> nextRunnerOpt = team.getPlayerByIndex(t, nextRunnerIndex);

        boolean sameRunner = nextRunnerOpt.isPresent() &&
                nextRunnerOpt.get().getUniqueId().equals(currentRunner.getUniqueId());

        // 인벤/장비 전달 (동일 주자라면 전달 생략)
        if (!sameRunner && nextRunnerOpt.isPresent()) {
            transferInventory(currentRunner, nextRunnerOpt.get());
        }

        // 모드 전환 (동일 주자면 그대로 유지)
        if (!sameRunner) {
            currentRunner.setGameMode(GameMode.SPECTATOR);
            nextRunnerOpt.ifPresent(n -> n.setGameMode(GameMode.SURVIVAL));
        }

        step.put(t, s + 1);
        board.updateAll();

        if (lastObjective) {
            stopGameWithWinner(t);
        }
    }

    private void transferInventory(Player from, Player to) {
        List<ItemStack> all = InventoryUtil.extractAll(from);

        for (ItemStack it : all) {
            Map<Integer, ItemStack> left = to.getInventory().addItem(it);
            if (!left.isEmpty()) {
                for (ItemStack remain : left.values()) {
                    to.getWorld().dropItemNaturally(to.getLocation(), remain);
                }
            }
        }
        from.getInventory().clear();
        from.getInventory().setArmorContents(null);
        from.getInventory().setItemInOffHand(null);
        from.updateInventory();
        to.updateInventory();
    }
}