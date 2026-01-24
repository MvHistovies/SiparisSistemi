package org.larune.siparis.gui.holder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.model.CategoryDef;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.SoundUtil;
import org.larune.siparis.util.Text;

import java.util.ArrayList;
import java.util.List;

public class CreateOrderHolder extends BaseHolder {

    private CategoryDef category;
    private Material material;

    private int amount = 64;
    private long unitPrice = 100;

    public static void open(GuiManager gui, Player p, CategoryDef category, Material mat) {
        CreateOrderHolder holder = new CreateOrderHolder();
        holder.category = category;
        holder.material = mat;

        long min = gui.orders().minUnitPrice();
        holder.unitPrice = Math.max(min, 100);

        holder.render(gui, p);

        if (gui.getPlugin().getConfig().getBoolean("sounds.enabled", true)) {
            SoundUtil.playMenuOpen(p);
        }
    }

    private void render(GuiManager gui, Player p) {
        String title = Text.color(gui.getPlugin().getConfig().getString("gui.createTitle", "&8Sipariş Oluştur"));
        Inventory inv = Bukkit.createInventory(this, 27, title);
        this.inv = inv;

        long total = gui.orders().totalCost(amount, unitPrice);

        inv.setItem(10, ItemUtil.named(material, "&fSeçilen Item", List.of("&7" + material.name())));
        inv.setItem(12, ItemUtil.named(Material.OAK_SIGN, "&eMiktar: &f" + amount,
                List.of("&7Sol: +1 | Shift+Sol: +16", "&7Sağ: -1 | Shift+Sağ: -16", "&8Orta: sıfırla(1)")));

        inv.setItem(14, ItemUtil.named(Material.GOLD_INGOT, "&eBirim Fiyat: &f" + unitPrice + "$",
                List.of("&7Sol: +small | Shift+Sol: +medium", "&7Sağ: -small | Shift+Sağ: -medium", "&7Drop: +large | Ctrl+Drop: -large")));

        List<String> totalLore = new ArrayList<>();
        totalLore.add("&7Miktar x Birim fiyat");

        if (gui.orders().isExpiryEnabled()) {
            long hours = gui.orders().getExpiryHours();
            totalLore.add("");
            totalLore.add("&7Sipariş Süresi: &e" + hours + " saat");
        }

        inv.setItem(16, ItemUtil.named(Material.PAPER, "&bToplam: &f" + total + "$", totalLore));

        inv.setItem(22, ItemUtil.named(Material.LIME_WOOL, "&aOnayla", List.of("&7Siparişi oluştur")));
        inv.setItem(18, ItemUtil.named(Material.ARROW, "&7Geri", List.of("&8Item listesine dön")));
        inv.setItem(26, ItemUtil.named(Material.BARRIER, "&cVazgeç", List.of("&8Kapat")));

        p.openInventory(inv);
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        ClickType ct = e.getClick();
        boolean sounds = gui.getPlugin().getConfig().getBoolean("sounds.enabled", true);

        int stepSmall = gui.getPlugin().getConfig().getInt("amountSteps.small", 1);
        int stepMed = gui.getPlugin().getConfig().getInt("amountSteps.medium", 16);

        long priceSmall = gui.getPlugin().getConfig().getLong("priceSteps.small", 10);
        long priceMed = gui.getPlugin().getConfig().getLong("priceSteps.medium", 100);
        long priceLarge = gui.getPlugin().getConfig().getLong("priceSteps.large", 1000);

        if (slot == 18) {
            if (sounds) SoundUtil.playClick(p);
            gui.openCategoryItems(p, category, 0);
            return;
        }
        if (slot == 26) {
            if (sounds) SoundUtil.playClick(p);
            p.closeInventory();
            return;
        }

        if (slot == 12) {
            if (ct == ClickType.MIDDLE) amount = 1;
            else if (ct.isLeftClick()) amount += ct.isShiftClick() ? stepMed : stepSmall;
            else if (ct.isRightClick()) amount -= ct.isShiftClick() ? stepMed : stepSmall;

            if (amount < 1) amount = 1;
            if (amount > 64 * 54) amount = 64 * 54;

            if (sounds) SoundUtil.playValueChange(p);
            render(gui, p);
            return;
        }

        if (slot == 14) {
            if (ct == ClickType.DROP) unitPrice += priceLarge;
            else if (ct == ClickType.CONTROL_DROP) unitPrice -= priceLarge;
            else if (ct.isLeftClick()) unitPrice += ct.isShiftClick() ? priceMed : priceSmall;
            else if (ct.isRightClick()) unitPrice -= ct.isShiftClick() ? priceMed : priceSmall;

            if (unitPrice < gui.orders().minUnitPrice()) unitPrice = gui.orders().minUnitPrice();
            if (unitPrice > gui.orders().maxUnitPrice()) unitPrice = gui.orders().maxUnitPrice();

            if (sounds) SoundUtil.playValueChange(p);
            render(gui, p);
            return;
        }

        if (slot == 22) {
            int id = gui.orders().createOrder(p, material, amount, unitPrice);

            if (id == -4) {
                p.sendMessage(Text.msg("messages.limitReached")
                        .replace("{limit}", String.valueOf(gui.orders().getMaxActiveOrders())));
                if (sounds) SoundUtil.playLimitReached(p);
                return;
            }
            if (id == -5) {
                p.sendMessage(Text.msg("messages.cooldown")
                        .replace("{sec}", String.valueOf(gui.orders().getCooldownSeconds())));
                if (sounds) SoundUtil.playLimitReached(p);
                return;
            }
            if (id == -6) {
                long total = gui.orders().totalCost(amount, unitPrice);
                p.sendMessage(Text.msg("messages.notEnoughMoney").replace("{total}", String.valueOf(total)));
                if (sounds) SoundUtil.playNoMoney(p);
                return;
            }
            if (id <= 0) {
                p.sendMessage(Text.msg("messages.invalid"));
                if (sounds) SoundUtil.playError(p);
                return;
            }

            long total = gui.orders().totalCost(amount, unitPrice);
            p.sendMessage(Text.msg("messages.created")
                    .replace("{id}", String.valueOf(id))
                    .replace("{total}", String.valueOf(total)));

            gui.openMainMenu(p);
        }
    }
}
