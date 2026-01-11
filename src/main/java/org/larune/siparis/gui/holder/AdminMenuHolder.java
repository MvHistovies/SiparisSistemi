package org.larune.siparis.gui.holder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.Text;

import java.util.List;

public class AdminMenuHolder extends BaseHolder {

    public static void open(GuiManager gui, Player p) {
        AdminMenuHolder holder = new AdminMenuHolder();
        String title = Text.color("&8Sipariş Admin");
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.inv = inv;

        // Log
        inv.setItem(11, ItemUtil.named(Material.BOOK, "&bLog", List.of(
                "&7Sipariş açma + teslim kayıtları",
                "&7Tıkla: &fLog ekranı"
        )));

        // Sil (iade yok)
        inv.setItem(13, ItemUtil.named(Material.BARRIER, "&cSipariş Sil (İade Yok)", List.of(
                "&7Chat'e sipariş ID yazarsın",
                "&7Kalan para iade edilmez"
        )));

        // Sil (iade var)
        inv.setItem(15, ItemUtil.named(Material.EMERALD, "&aSipariş Sil (İade Var)", List.of(
                "&7Chat'e sipariş ID yazarsın",
                "&7Kalan para sipariş sahibine iade edilir"
        )));

        // Reload
        inv.setItem(22, ItemUtil.named(Material.REDSTONE, "&eReload", List.of(
                "&7Config + kategoriler yenilenir"
        )));

        // Geri
        inv.setItem(26, ItemUtil.named(Material.ARROW, "&eGeri", List.of("&7Ana menü")));

        p.openInventory(inv);
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        switch (slot) {
            case 11 -> gui.openLogs(p, 0);

            case 13 -> gui.beginAdminCancel(p, false);

            case 15 -> gui.beginAdminCancel(p, true);

            case 22 -> {
                gui.getPlugin().reloadConfig();
                Text.init(gui.getPlugin());
                gui.reloadCategories();
                p.sendMessage(Text.color("&aConfig yeniden yüklendi."));
                open(gui, p);
            }

            case 26 -> gui.openMainMenu(p);
        }
    }
}
