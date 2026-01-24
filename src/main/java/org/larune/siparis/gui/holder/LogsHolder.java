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
import org.larune.siparis.model.OrderLogSummary;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.Text;

import java.text.SimpleDateFormat;
import java.util.*;

public class LogsHolder implements InventoryHolder {

    private final GuiManager gui;
    private final int page;
    private Inventory inv;

    private LogsHolder(GuiManager gui, int page) {
        this.gui = gui;
        this.page = page;
    }

    public static void open(GuiManager gui, Player p, int page) {
        LogsHolder holder = new LogsHolder(gui, page);
        holder.inv = gui.getPlugin().getServer().createInventory(holder, 54, Text.color("&8Sipariş Logları"));
        holder.render();
        p.openInventory(holder.inv);
    }

    private void render() {
        inv.clear();
        fillFrame();

        inv.setItem(45, ItemUtil.named(Material.ARROW, "&eGeri", ItemUtil.lore("&7Admin menü")));
        inv.setItem(49, ItemUtil.named(Material.MAP, "&bBilgi", ItemUtil.lore(
                "&7Her sipariş: &fSiparişi açanın kafası",
                "&7Tıkla: &fDetay (kim ne kadar teslim etti)"
        )));

        int perPage = 28;
        int offset = page * perPage;

        List<OrderLogSummary> list = gui.orders().listLogSummaries(perPage, offset);

        if (page > 0) inv.setItem(52, ItemUtil.named(Material.SPECTRAL_ARROW, "&eÖnceki", ItemUtil.lore("&7Sayfa geri")));
        if (list.size() >= perPage) inv.setItem(53, ItemUtil.named(Material.SPECTRAL_ARROW, "&eSonraki", ItemUtil.lore("&7Sayfa ileri")));

        int[] slots = gridSlots();
        int idx = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        for (OrderLogSummary s : list) {
            if (idx >= slots.length) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                OfflinePlayer off = Bukkit.getOfflinePlayer(s.ownerUuid);
                meta.setOwningPlayer(off);

                meta.setDisplayName(Text.color("&b" + safe(s.ownerName) + " &8(#" + s.orderId + ")"));

                int remaining = Math.max(0, s.totalAmount - s.deliveredAmount);
                long total = (long) s.totalAmount * s.unitPrice;

                List<String> lore = new ArrayList<>();
                lore.add(Text.color("&8&m----------------------"));
                lore.add(Text.color("&7🕒 " + sdf.format(new Date(s.createdAt))));
                lore.add(Text.color("&7📦 &f" + prettyMaterial(s.material)));
                lore.add(Text.color("&7📊 &f" + s.deliveredAmount + "&7/&f" + s.totalAmount + " &7teslim"));
                lore.add(Text.color("&7⏳ Kalan: &f" + remaining));
                lore.add(Text.color("&7💰 Birim: &f" + Text.money(s.unitPrice) + "$"));
                lore.add(Text.color("&7🧾 Toplam: &f" + Text.money(total) + "$"));
                lore.add(Text.color("&8&m----------------------"));
                lore.add(Text.color("&eTıkla &7→ &aDetay"));

                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            inv.setItem(slots[idx++], head);
        }

    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        if (slot == 45) { gui.openAdmin(p); return; }
        if (slot == 52) { open(gui, p, Math.max(0, page - 1)); return; }
        if (slot == 53) { open(gui, p, page + 1); return; }
        if (!isGridSlot(slot)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
        String name = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : null;
        int orderId = parseOrderIdFromName(name);
        if (orderId <= 0) return;

        gui.openLogDetail(p, orderId, page);
    }

    private int parseOrderIdFromName(String displayName) {
        if (displayName == null) return -1;
        String raw = org.bukkit.ChatColor.stripColor(displayName);
        int a = raw.lastIndexOf("(#");
        int b = raw.lastIndexOf(")");
        if (a < 0 || b < 0 || b <= a + 2) return -1;
        String num = raw.substring(a + 2, b).trim();
        try { return Integer.parseInt(num); } catch (Exception ignored) { return -1; }
    }

    private boolean isGridSlot(int slot) {
        for (int s : gridSlots()) if (s == slot) return true;
        return false;
    }

    private int[] gridSlots() {
        List<Integer> s = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) s.add(row * 9 + col);
        }
        return s.stream().mapToInt(i -> i).toArray();
    }

    private void fillFrame() {
        boolean enabled = gui.getPlugin().getConfig().getBoolean("guiTheme.frame.enabled", true);
        if (!enabled) return;

        Material pane = Material.matchMaterial(gui.getPlugin().getConfig().getString("guiTheme.frame.material", "GRAY_STAINED_GLASS_PANE"));
        if (pane == null) pane = Material.GRAY_STAINED_GLASS_PANE;

        ItemStack frame = ItemUtil.named(pane, gui.getPlugin().getConfig().getString("guiTheme.frame.name", " "), List.of());

        int size = inv.getSize();
        int rows = size / 9;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                boolean border = (r == 0 || r == rows - 1 || c == 0 || c == 8);
                if (!border) continue;
                int sl = r * 9 + c;
                if (inv.getItem(sl) == null) inv.setItem(sl, frame);
            }
        }
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "Bilinmiyor" : s;
    }

    private static String prettyMaterial(org.bukkit.Material m) {
        if (m == null) return "Bilinmiyor";
        String n = m.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = n.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    public Inventory getInventory() { return inv; }
}
