package org.larune.siparis.gui.holder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.model.Order;
import org.larune.siparis.model.OrderDeliveryStat;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.Text;

import java.util.*;

public class OrderLogDetailHolder implements InventoryHolder {

    private final GuiManager gui;
    private final int orderId;
    private final int backPage;
    private Inventory inv;

    private OrderLogDetailHolder(GuiManager gui, int orderId, int backPage) {
        this.gui = gui;
        this.orderId = orderId;
        this.backPage = backPage;
    }

    public static void open(GuiManager gui, Player p, int orderId, int backPage) {
        OrderLogDetailHolder holder = new OrderLogDetailHolder(gui, orderId, backPage);
        holder.inv = Bukkit.createInventory(holder, 54, Text.color("&8Log Detay &7(#" + orderId + ")"));
        holder.render();
        p.openInventory(holder.inv);
    }

    private void render() {
        inv.clear();

        // Üst bilgi
        inv.setItem(45, ItemUtil.named(Material.ARROW, "&eGeri", ItemUtil.lore("&7Log listesi")));
        inv.setItem(49, ItemUtil.named(Material.PAPER, "&bDetay", ItemUtil.lore(
                "&7Bu siparişte kim ne kadar teslim etti?",
                "&7Her teslim eden: &fKafa olarak listelenir"
        )));

        Order o = gui.orders().getOrder(orderId);
        if (o != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(o.owner);
            String ownerName = owner.getName() == null ? "Bilinmiyor" : owner.getName();

            long total = (long) o.totalAmount * o.unitPrice;
            int remaining = Math.max(0, o.remainingAmount);

            inv.setItem(4, ItemUtil.named(o.material, "&6&lSipariş #" + o.id, ItemUtil.lore(
                    "&7Sahip: &b" + ownerName,
                    "&7Item: &f" + o.material.name(),
                    "&7Miktar: &f" + (o.totalAmount - remaining) + "&7/&f" + o.totalAmount,
                    "&7Kalan: &f" + remaining,
                    "&7Birim: &f" + Text.money(o.unitPrice) + "$",
                    "&7Toplam: &f" + Text.money(total) + "$",
                    "&7Durum: &f" + o.status
            )));
        }

        List<OrderDeliveryStat> deliveries = gui.orders().listDeliveriesForOrder(orderId);

        int[] slots = gridSlots();
        int idx = 0;

        if (deliveries.isEmpty()) {
            inv.setItem(22, ItemUtil.named(Material.BARRIER, "&cHenüz teslim yok", ItemUtil.lore(
                    "&7Bu siparişte DELIVER kaydı bulunamadı."
            )));
            return;
        }

        for (OrderDeliveryStat d : deliveries) {
            if (idx >= slots.length) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                OfflinePlayer off = Bukkit.getOfflinePlayer(d.delivererUuid);
                meta.setOwningPlayer(off);

                meta.setDisplayName(Text.color("&a" + safe(d.delivererName)));

                meta.setLore(List.of(
                        Text.color("&8&m----------------------"),
                        Text.color("&7Teslim: &e" + d.amount + " adet"),
                        Text.color("&7Kazanç: &a" + Text.money(d.pay) + "$"),
                        Text.color("&8&m----------------------")
                ));

                head.setItemMeta(meta);
            }

            inv.setItem(slots[idx++], head);
        }
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == 45) {
            LogsHolder.open(gui, p, backPage);
        }
    }

    private int[] gridSlots() {
        List<Integer> s = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) s.add(row * 9 + col);
        }
        return s.stream().mapToInt(i -> i).toArray();
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "Bilinmiyor" : s;
    }

    @Override
    public Inventory getInventory() { return inv; }
}
