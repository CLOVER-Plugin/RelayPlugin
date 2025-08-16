package yd.kingdom.relayPlugin.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class InventoryUtil {

    public static boolean hasMaterial(PlayerInventory inv, Material mat) {
        if (mat == null) return false;

        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() == mat) return true;
        }
        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() == mat) return true;

        ItemStack[] arm = inv.getArmorContents();
        if (arm != null) {
            for (ItemStack it : arm) if (it != null && it.getType() == mat) return true;
        }
        return false;
    }

    /** 플레이어의 모든 아이템(인벤토리/갑옷/보조손)을 복제해 리스트로 반환 */
    public static List<ItemStack> extractAll(Player from) {
        List<ItemStack> list = new ArrayList<>();

        for (ItemStack it : from.getInventory().getContents()) {
            if (it != null) list.add(it.clone());
        }
        ItemStack off = from.getInventory().getItemInOffHand();
        if (off != null) list.add(off.clone());

        ItemStack[] arm = from.getInventory().getArmorContents();
        if (arm != null) {
            for (ItemStack it : arm) if (it != null) list.add(it.clone());
        }
        return list;
    }

    public static int removeAll(PlayerInventory inv, Material mat) {
        if (mat == null) return 0;
        int removed = 0;

        // 메인 인벤
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && it.getType() == mat) {
                removed += it.getAmount();
                contents[i] = null;
            }
        }
        inv.setContents(contents);

        // 보조손
        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() == mat) {
            removed += off.getAmount();
            inv.setItemInOffHand(null);
        }

        // 방어구
        ItemStack[] arm = inv.getArmorContents();
        boolean changed = false;
        for (int i = 0; i < arm.length; i++) {
            ItemStack it = arm[i];
            if (it != null && it.getType() == mat) {
                removed += it.getAmount();
                arm[i] = null;
                changed = true;
            }
        }
        if (changed) inv.setArmorContents(arm);

        return removed;
    }
}