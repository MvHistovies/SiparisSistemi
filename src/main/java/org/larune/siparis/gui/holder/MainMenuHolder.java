package org.larune.siparis.gui.holder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.SoundUtil;
import org.larune.siparis.util.Text;

import java.util.List;

public class MainMenuHolder extends BaseHolder {

    public static void open(GuiManager gui, Player p) {
        MainMenuHolder holder = new MainMenuHolder();
        String title = Text.color(gui.getPlugin().getConfig().getString("gui.title", "&8Sipariş Sistemi"));
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.inv = inv;

        inv.setItem(11, ItemUtil.named(Material.EMERALD, "&aSipariş Aç", List.of("&7Kategori seçerek sipariş oluştur")));
        inv.setItem(13, ItemUtil.named(Material.PAPER, "&fSiparişler", List.of("&7Aktif siparişleri görüntüle")));
        inv.setItem(15, ItemUtil.named(Material.BOOK, "&eBenim Siparişlerim", List.of("&7Kendi siparişlerini gör / iptal et")));
        inv.setItem(22, ItemUtil.named(Material.CHEST, "&bTeslim Kutusu", List.of("&7Gelen itemleri buradan al")));

        if (p.hasPermission("siparis.admin")) {
            inv.setItem(26, ItemUtil.named(Material.REDSTONE, "&cAdmin Menü", List.of("&7Admin işlemleri")));
        }

        p.openInventory(inv);

        if (gui.getPlugin().getConfig().getBoolean("sounds.enabled", true)) {
            SoundUtil.playMenuOpen(p);
        }
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        boolean sounds = gui.getPlugin().getConfig().getBoolean("sounds.enabled", true);

        switch (e.getRawSlot()) {
            case 11 -> {
                if (sounds) SoundUtil.playClick(p);
                gui.openCategories(p);
            }
            case 13 -> {
                if (sounds) SoundUtil.playClick(p);
                gui.openOrders(p, false, 0);
            }
            case 15 -> {
                if (sounds) SoundUtil.playClick(p);
                gui.openOrders(p, true, 0);
            }
            case 22 -> {
                if (sounds) SoundUtil.playClick(p);
                gui.openBox(p, 0);
            }
            case 26 -> {
                if (!p.hasPermission("siparis.admin")) return;
                if (sounds) SoundUtil.playClick(p);
                gui.openAdmin(p);
            }
        }
    }
}
