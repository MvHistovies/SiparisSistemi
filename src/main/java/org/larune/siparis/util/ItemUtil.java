package org.larune.siparis.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public final class ItemUtil {

    private ItemUtil() {}

    public static ItemStack named(Material mat, String name, List<String> lore) {
        return named(new ItemStack(mat), name, lore);
    }

    public static ItemStack named(ItemStack it, String name, List<String> lore) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            if (lore != null) meta.setLore(lore.stream().map(Text::color).toList());
            it.setItemMeta(meta);
        }
        return it;
    }

    public static ItemStack materialLoreItem(Material mat, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setLore(lore.stream().map(Text::color).toList());
            it.setItemMeta(meta);
        }
        return it;
    }

    public static List<String> lore(String... lines) {
        return Arrays.asList(lines);
    }

    public static int countInInventory(Inventory inv, Material mat) {
        int count = 0;
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType() != mat) continue;
            count += it.getAmount();
        }
        return count;
    }

    public static int removeFromInventory(Inventory inv, Material mat, int amount) {
        int need = amount;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != mat) continue;

            int take = Math.min(need, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) inv.setItem(i, null);

            need -= take;
            if (need <= 0) break;
        }
        return amount - need;
    }
    public static boolean giveOrDrop(org.bukkit.entity.Player p, ItemStack stack) {
        var inv = p.getInventory();
        var left = inv.addItem(stack).values();
        if (!left.isEmpty()) {
            for (ItemStack it : left) {
                p.getWorld().dropItemNaturally(p.getLocation(), it);
            }
            return false;
        }
        return true;
    }
}
