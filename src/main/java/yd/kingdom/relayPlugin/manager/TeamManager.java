package yd.kingdom.relayPlugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {

    public enum Team { RED, BLUE }

    private final Map<Team, LinkedList<UUID>> teamOrder = new EnumMap<>(Team.class);
    private final Map<UUID, Team> byPlayer = new HashMap<>();

    public TeamManager() {
        for (Team t : Team.values()) teamOrder.put(t, new LinkedList<>());
    }

    public synchronized void joinTeam(Player p, Team t) {
        leaveIfInTeam(p.getUniqueId());
        teamOrder.get(t).add(p.getUniqueId()); // 입력 순서대로 뒤에 붙임
        byPlayer.put(p.getUniqueId(), t);
    }

    public synchronized void leaveIfInTeam(UUID id) {
        Team prev = byPlayer.remove(id);
        if (prev != null) teamOrder.get(prev).remove(id);
    }

    public synchronized Team getTeam(UUID id) { return byPlayer.get(id); }

    public synchronized List<UUID> getOrder(Team t) {
        return Collections.unmodifiableList(teamOrder.get(t));
    }

    public synchronized int size(Team t) { return teamOrder.get(t).size(); }

    public synchronized OptionalInt indexOf(Team t, UUID id) {
        int idx = teamOrder.get(t).indexOf(id);
        return idx >= 0 ? OptionalInt.of(idx) : OptionalInt.empty();
    }

    public synchronized Optional<Player> getPlayer(UUID id) {
        return Optional.ofNullable(Bukkit.getPlayer(id));
    }

    public synchronized Optional<Player> getPlayerByIndex(Team t, int index) {
        List<UUID> order = teamOrder.get(t);
        if (order.isEmpty() || index < 0 || index >= order.size()) return Optional.empty();
        return getPlayer(order.get(index));
    }

    public synchronized Collection<UUID> allPlayers() {
        List<UUID> all = new ArrayList<>();
        for (Team t : Team.values()) all.addAll(teamOrder.get(t));
        return all;
    }
}