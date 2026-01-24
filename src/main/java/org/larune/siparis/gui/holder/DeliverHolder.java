package org.larune.siparis.gui.holder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.model.Order;
import org.larune.siparis.service.OrderService;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.SoundUtil;
import org.larune.siparis.util.Text;

import java.util.ArrayList;
import java.util.List;

public class DeliverHolder extends BaseHolder {

    private int orderId;
    private int deliverAmount = 1;

    public static void open(GuiManager gui, Player p, int orderId) {
        DeliverHolder holder = new DeliverHolder();
        holder.orderId = orderId;
        holder.render(gui, p);

        if (gui.getPlugin().getConfig().getBoolean("sounds.enabled", true)) {
            SoundUtil.playMenuOpen(p);
        }
    }

    private void render(GuiManager gui, Player p) {
        Order o = gui.orders().getOrder(orderId);
        if (o == null) {
            p.sendMessage(Text.msg("messages.invalid"));
            gui.openOrders(p, false, 0);
            return;
        }

        String title = Text.color(gui.getPlugin().getConfig().getString("gui.deliverTitle", "&8Teslim Et"));
        Inventory inv = Bukkit.createInventory(this, 27, title);
        this.inv = inv;

        int inInv = ItemUtil.countInInventory(p.getInventory(), o.material);
        int maxDeliver = Math.min(o.remainingAmount, inInv);
        if (deliverAmount > maxDeliver) deliverAmount = Math.max(1, maxDeliver);

        List<String> orderLore = new ArrayList<>();
        orderLore.add("&7Kalan: &f" + o.remainingAmount);
        orderLore.add("&7Birim: &f" + o.unitPrice + "$");
        orderLore.add("&7Envanterinde: &f" + inInv);

        if (o.expiresAt > 0) {
            String timeLeft = o.getFormattedRemainingTime();
            String timeColor = o.getRemainingTime() < 3600000 ? "&c" : "&a";
            orderLore.add("&7Kalan Süre: " + timeColor + timeLeft);
        }

        inv.setItem(10, ItemUtil.named(o.material, "&fSipariş #" + o.id, orderLore));

        inv.setItem(13, ItemUtil.named(Material.OAK_SIGN, "&eTeslim Miktarı: &f" + deliverAmount,
                List.of("&7Sol: +1 | Shift+Sol: +16",
                        "&7Sağ: -1 | Shift+Sağ: -16",
                        "&8Orta: max")));

        long pay = (long) deliverAmount * o.unitPrice;
        inv.setItem(16, ItemUtil.named(Material.GOLD_NUGGET, "&bKazanç: &f" + pay + "$",
                List.of("&7Teslim miktarı x birim fiyat")));

        inv.setItem(21, ItemUtil.named(Material.LIME_WOOL, "&aOnayla", List.of("&7Teslim et")));
        inv.setItem(23, ItemUtil.named(Material.YELLOW_WOOL, "&eHızlı Teslim", List.of("&7Maksimum miktarı teslim et")));

        inv.setItem(18, ItemUtil.named(Material.ARROW, "&7Geri", List.of("&8Sipariş listesi")));
        inv.setItem(26, ItemUtil.named(Material.BARRIER, "&cKapat", List.of("&8")));

        p.openInventory(inv);
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        ClickType ct = e.getClick();
        boolean sounds = gui.getPlugin().getConfig().getBoolean("sounds.enabled", true);

        if (slot == 18) {
            if (sounds) SoundUtil.playClick(p);
            gui.openOrders(p, false, 0);
            return;
        }
        if (slot == 26) {
            if (sounds) SoundUtil.playClick(p);
            p.closeInventory();
            return;
        }

        if (slot == 13) {
            Order o = gui.orders().getOrder(orderId);
            if (o == null) return;

            int inInv = ItemUtil.countInInventory(p.getInventory(), o.material);
            int maxDeliver = Math.min(o.remainingAmount, inInv);

            int stepSmall = gui.getPlugin().getConfig().getInt("amountSteps.small", 1);
            int stepMed = gui.getPlugin().getConfig().getInt("amountSteps.medium", 16);

            if (ct == ClickType.MIDDLE) deliverAmount = Math.max(1, maxDeliver);
            else if (ct.isLeftClick()) deliverAmount += ct.isShiftClick() ? stepMed : stepSmall;
            else if (ct.isRightClick()) deliverAmount -= ct.isShiftClick() ? stepMed : stepSmall;

            if (deliverAmount < 1) deliverAmount = 1;
            if (deliverAmount > Math.max(1, maxDeliver)) deliverAmount = Math.max(1, maxDeliver);

            if (sounds) SoundUtil.playValueChange(p);
            render(gui, p);
            return;
        }

        if (slot == 23) {
            Order o = gui.orders().getOrder(orderId);
            if (o == null) {
                p.sendMessage(Text.msg("messages.invalid"));
                if (sounds) SoundUtil.playError(p);
                gui.openOrders(p, false, 0);
                return;
            }

            int inInv = ItemUtil.countInInventory(p.getInventory(), o.material);
            int maxDeliver = Math.min(o.remainingAmount, inInv);
            if (maxDeliver <= 0) {
                p.sendMessage(Text.msg("messages.nothingToDeliver"));
                if (sounds) SoundUtil.playError(p);
                return;
            }

            OrderService.DeliveryResult res = gui.orders().deliver(p, orderId, maxDeliver);

            if (!res.ok) {
                if ("NO_ITEMS".equals(res.error)) p.sendMessage(Text.msg("messages.nothingToDeliver"));
                else if ("EXPIRED".equals(res.error)) p.sendMessage(Text.msg("messages.orderExpiredCantDeliver"));
                else p.sendMessage(Text.msg("messages.invalid"));
                if (sounds) SoundUtil.playError(p);
                return;
            }

            p.sendMessage(Text.msg("messages.delivered")
                    .replace("{pay}", String.valueOf(res.pay))
                    .replace("{amt}", String.valueOf(res.delivered)));

            gui.openOrders(p, false, 0);
            return;
        }

        if (slot == 21) {
            Order o = gui.orders().getOrder(orderId);
            if (o == null) {
                p.sendMessage(Text.msg("messages.invalid"));
                if (sounds) SoundUtil.playError(p);
                gui.openOrders(p, false, 0);
                return;
            }

            int inInv = ItemUtil.countInInventory(p.getInventory(), o.material);
            int maxDeliver = Math.min(o.remainingAmount, inInv);
            if (maxDeliver <= 0) {
                p.sendMessage(Text.msg("messages.nothingToDeliver"));
                if (sounds) SoundUtil.playError(p);
                return;
            }

            int tryDeliver = Math.min(deliverAmount, maxDeliver);
            OrderService.DeliveryResult res = gui.orders().deliver(p, orderId, tryDeliver);

            if (!res.ok) {
                if ("NO_ITEMS".equals(res.error)) p.sendMessage(Text.msg("messages.nothingToDeliver"));
                else if ("EXPIRED".equals(res.error)) p.sendMessage(Text.msg("messages.orderExpiredCantDeliver"));
                else p.sendMessage(Text.msg("messages.invalid"));
                if (sounds) SoundUtil.playError(p);
                return;
            }

            p.sendMessage(Text.msg("messages.delivered")
                    .replace("{pay}", String.valueOf(res.pay))
                    .replace("{amt}", String.valueOf(res.delivered)));

            gui.openOrders(p, false, 0);
        }
    }
}
