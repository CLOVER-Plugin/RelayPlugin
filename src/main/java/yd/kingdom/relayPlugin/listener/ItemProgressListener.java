package yd.kingdom.relayPlugin.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import yd.kingdom.relayPlugin.manager.GameManager;

public class ItemProgressListener implements Listener {

    private final Plugin plugin;
    private final GameManager game;

    public ItemProgressListener(Plugin plugin, GameManager game) {
        this.plugin = plugin;
        this.game = game;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) game.tryCompleteIfHasObjective(p);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> game.tryCompleteIfHasObjective(p), 1L);
        }
    }

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent e) {
        game.tryCompleteIfHasObjective(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> game.tryCompleteIfHasObjective(p), 1L);
        }
    }
}