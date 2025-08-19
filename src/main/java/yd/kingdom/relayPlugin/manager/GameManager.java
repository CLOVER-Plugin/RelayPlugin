package yd.kingdom.relayPlugin.manager;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

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
            list = Arrays.asList("DIAMOND_BLOCK", "TINTED_GLASS", "ENCHANTING_TABLE", "NAME_TAG");

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

        enforceRunnerDoesNotHaveCurrentObjective(TeamManager.Team.RED);
        enforceRunnerDoesNotHaveCurrentObjective(TeamManager.Team.BLUE);

        int period = Math.max(1, plugin.getConfig().getInt("actionbarPeriodTicks", 2));
        actionBarTask = new BukkitRunnable() { @Override public void run() { tickActionBars(); } };
        actionBarTask.runTaskTimer(plugin, 0L, period);

        board.updateAll();
        running = true;

        Bukkit.broadcastMessage("§a팀 릴레이 시작! 목표: §f" +
                objectives.stream().map(Enum::name).collect(Collectors.joining(" §7→ §f")));

        checkAllActiveInventories();
        updateBossBars();
    }

    public synchronized void forceStopGame(boolean silent) {
        if (actionBarTask != null) { actionBarTask.cancel(); actionBarTask = null; }
        if (!running && !finished) return;

        for (UUID id : team.allPlayers()) {
            team.getPlayer(id).ifPresent(p -> p.setGameMode(GameMode.SURVIVAL));
        }
        running = false;
        if (!silent) Bukkit.broadcastMessage("§c게임이 종료되었습니다.");
        clearBossBars();
    }

    public synchronized void stopGameWithWinner(TeamManager.Team winTeam) {
        if (finished) return;
        finished = true;
        winner = winTeam;
        if (actionBarTask != null) { actionBarTask.cancel(); actionBarTask = null; }

        long now = System.nanoTime();
        endNano.put(winTeam, now);

        for (UUID id : team.allPlayers()) {
            team.getPlayer(id).ifPresent(p -> p.setGameMode(GameMode.SURVIVAL));
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
        clearBossBars();
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
        long ms = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(ns);
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds); // mm:ss
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
            Player next = nextRunnerOpt.get();
            transferInventory(currentRunner, next);
            swapLocations(currentRunner, next);
        }

        enforceNextRunnerDoesNotHaveNextObjective(t, currentRunner, nextRunnerOpt, lastObjective, s);

        // 모드 전환 (동일 주자면 그대로 유지)
        if (!sameRunner) {
            currentRunner.setGameMode(GameMode.SPECTATOR);
            nextRunnerOpt.ifPresent(n -> n.setGameMode(GameMode.SURVIVAL));
        }

        step.put(t, s + 1);
        board.updateAll();
        updateBossBars();

        if (lastObjective) {
            stopGameWithWinner(t);
        }
    }

    private void swapLocations(Player a, Player b) {
        Location locA = a.getLocation().clone();
        Location locB = b.getLocation().clone();

        if (a.isInsideVehicle()) a.leaveVehicle();
        if (b.isInsideVehicle()) b.leaveVehicle();

        a.setFallDistance(0f);
        b.setFallDistance(0f);

        a.teleport(locB);
        b.teleport(locA);
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

    private void updateBossBars() {
        for (UUID id : team.allPlayers()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            TeamManager.Team t = team.getTeam(id);
            if (t == null) { removeBossBar(id); continue; }

            int s = step.getOrDefault(t, 0);
            int total = objectives.size();

            // 팀의 "현재" 목표 결정
            String title;
            double progress;
            if (s >= total) {
                title = "§a모든 목표 완료!";
                progress = 1.0;
            } else {
                Material need = objectives.get(s);
                title = "§e현재 목표: §f" + need.name() + " §7(" + (s + 1) + "/" + total + ")";
                // 진행률: 현재 단계/전체
                progress = Math.min(1.0, (double) s / (double) total);
            }

            BossBar bar = getOrCreateBossBar(p, t); // 색상(레드/블루) 유지
            bar.setTitle(title);
            bar.setProgress(progress);
            bar.setVisible(true);
        }
    }

    /** 보스바 생성/획득 + 색상/스타일/가시성 적용 */
    private BossBar getOrCreateBossBar(Player p, TeamManager.Team t) {
        BossBar bar = bossBars.get(p.getUniqueId());
        BarColor color = (t == TeamManager.Team.RED)
                ? BarColor.RED : BarColor.BLUE;

        if (bar == null) {
            bar = Bukkit.createBossBar("목표 대기중", color, BarStyle.SEGMENTED_10);
            bar.addPlayer(p);
            bar.setVisible(true);
            bossBars.put(p.getUniqueId(), bar);
        } else {
            if (bar.getColor() != color) bar.setColor(color);
            if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
            bar.setVisible(true);
        }
        return bar;
    }

    /** 모든 보스바 제거(게임 종료/강제중지 시) */
    private void clearBossBars() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
    }

    /** 특정 플레이어 보스바 제거(팀 이탈 등) */
    private void removeBossBar(UUID id) {
        BossBar bar = bossBars.remove(id);
        if (bar != null) bar.removeAll();
    }

    /** 현재 팀 주자에게 필요한 '현 단계 목표' 아이템이 이미 있으면 제거 */
    private void enforceRunnerDoesNotHaveCurrentObjective(TeamManager.Team t) {
        Material need = getRequired(t);
        if (need == null) return;

        int runnerIdx = getRunnerIndex(t);
        team.getPlayerByIndex(t, runnerIdx).ifPresent(p -> {
            int removed = InventoryUtil.removeAll(p.getInventory(), need);
            if (removed > 0) {
                p.updateInventory();
                p.sendMessage("§e현재 목표 아이템 §f(" + need.name() + ") §e이(가) 인벤토리에 있어 제거되었습니다. 다시 획득하세요!");
            }
        });
    }

    /** 다음 주자가 맡을 '다음 단계 목표' 아이템을 미리 들고 있으면 제거 */
    private void enforceNextRunnerDoesNotHaveNextObjective(TeamManager.Team t, Player currentRunner, Optional<Player> nextRunnerOpt, boolean lastObjective, int currentStep) {
        if (lastObjective) return; // 마지막이면 바로 종료되므로 불필요
        Material nextNeed = objectives.get(currentStep + 1);

        // 다음 주자가 나 자신(1인 팀)일 수도 있음
        Player target = nextRunnerOpt.orElse(currentRunner);
        int removed = InventoryUtil.removeAll(target.getInventory(), nextNeed);
        if (removed > 0) {
            target.updateInventory();
            target.sendMessage("§e다음 목표 아이템 §f(" + nextNeed.name() + ") §e이(가) 인벤토리에 있어 제거되었습니다. 다시 획득하세요!");
        }
    }
}