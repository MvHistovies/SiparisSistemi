package org.larune.siparis.gui.holder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.model.Order;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.SoundUtil;
import org.larune.siparis.util.Text;

import java.util.ArrayList;
import java.util.List;

public class OrdersListHolder implements InventoryHolder {

    private static final String PDC_ORDER_ID = "order_id";

    private final GuiManager gui;
    private final boolean myOrders;
    private final int page;
    private String sortBy = "id";
    private boolean ascending = false;
    private Inventory inv;

    private OrdersListHolder(GuiManager gui, boolean myOrders, int page, String sortBy, boolean ascending) {
        this.gui = gui;
        this.myOrders = myOrders;
        this.page = page;
        this.sortBy = sortBy;
        this.ascending = ascending;
    }

    public static void open(GuiManager gui, Player p, boolean myOrders, int page) {
        open(gui, p, myOrders, page, "id", false);
    }

    public static void open(GuiManager gui, Player p, boolean myOrders, int page, String sortBy, boolean ascending) {
        OrdersListHolder holder = new OrdersListHolder(gui, myOrders, page, sortBy, ascending);

        String titlePath = myOrders ? "gui.myOrdersTitle" : "gui.ordersTitle";
        String title = Text.color(gui.getPlugin().getConfig().getString(titlePath, "&8Aktif Siparişler"));

        holder.inv = gui.getPlugin().getServer().createInventory(holder, 54, title);
        holder.render(p);
        p.openInventory(holder.inv);

        if (gui.getPlugin().getConfig().getBoolean("sounds.enabled", true)) {
            SoundUtil.playMenuOpen(p);
        }
    }

    private void render(Player p) {
        inv.clear();
        fillFrame();

        inv.setItem(45, ItemUtil.named(Material.ARROW, "&eGeri", ItemUtil.lore("&7Ana menü")));

        int perPage = 28;
        int safePage = Math.max(0, page);
        int offset = safePage * perPage;

        List<Order> list;
        if (myOrders) {
            list = gui.orders().listByOwner(p.getUniqueId(), perPage, offset);
        } else {
            list = gui.orders().listActiveSorted(perPage, offset, sortBy, ascending);
        }

        String sortName = switch (sortBy) {
            case "price" -> "Fiyat";
            case "amount" -> "Miktar";
            case "time", "expires" -> "Süre";
            default -> "Tarih";
        };
        String sortDir = ascending ? "↑" : "↓";

        inv.setItem(47, ItemUtil.named(Material.HOPPER, "&6Sırala: &f" + sortName + " " + sortDir,
                ItemUtil.lore("&7Sol: Sıralama türü değiştir", "&7Sağ: Yön değiştir")));

        inv.setItem(49, ItemUtil.named(Material.PAPER, "&bBilgi", ItemUtil.lore(
                "&7Görünüm: " + (myOrders ? "&aBenim Siparişlerim" : "&eAktif Siparişler"),
                "&7Sayfa: &f" + (safePage + 1),
                "&7Gösterilen: &f" + list.size()
        )));

        if (safePage > 0) inv.setItem(52, ItemUtil.named(Material.SPECTRAL_ARROW, "&eÖnceki", ItemUtil.lore("&7Sayfa geri")));
        if (list.size() >= perPage) inv.setItem(53, ItemUtil.named(Material.SPECTRAL_ARROW, "&eSonraki", ItemUtil.lore("&7Sayfa ileri")));

        int[] slots = gridSlots();
        int idx = 0;

        NamespacedKey key = new NamespacedKey(gui.getPlugin(), PDC_ORDER_ID);

        for (Order o : list) {
            if (idx >= slots.length) break;

            String ownerName = Bukkit.getOfflinePlayer(o.owner).getName();
            if (ownerName == null) ownerName = "Bilinmiyor";

            long total = (long) o.totalAmount * o.unitPrice;
            String statusLine = "ACTIVE".equalsIgnoreCase(o.status) ? "&aAKTİF" : "&c" + o.status;

            List<String> lore = new ArrayList<>();
            lore.add("&6&lSipariş #" + o.id);
            lore.add("&8&m----------------------");
            lore.add("&7Kalan: &f" + o.remainingAmount + " &8/&f " + o.totalAmount);
            lore.add("&7Birim Fiyat: &e" + Text.money(o.unitPrice) + "$");
            lore.add("&7Toplam: &e" + Text.money(total) + "$");
            lore.add("&7Durum: " + statusLine);
            lore.add("&7Sahip: &f" + ownerName);

            if (o.expiresAt > 0) {
                String timeLeft = o.getFormattedRemainingTime();
                String timeColor = o.getRemainingTime() < 3600000 ? "&c" : "&a";
                lore.add("&7Kalan Süre: " + timeColor + timeLeft);
            }

            lore.add("&8&m----------------------");
            lore.add("&aSol Tık: &fTeslim et");
            lore.add("&eShift+Sol: &fHızlı teslim (max)");

            ItemStack it = ItemUtil.materialLoreItem(o.material, lore);

            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, o.id);
                it.setItemMeta(meta);
            }

            inv.setItem(slots[idx++], it);
        }
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        boolean sounds = gui.getPlugin().getConfig().getBoolean("sounds.enabled", true);

        if (slot == 45) {
            if (sounds) SoundUtil.playClick(p);
            gui.openMainMenu(p);
            return;
        }

        if (slot == 47) {
            if (e.isLeftClick()) {
                sortBy = switch (sortBy) {
                    case "id" -> "price";
                    case "price" -> "amount";
                    case "amount" -> "time";
                    default -> "id";
                };
            } else if (e.isRightClick()) {
                ascending = !ascending;
            }
            if (sounds) SoundUtil.playClick(p);
            open(gui, p, myOrders, 0, sortBy, ascending);
            return;
        }

        if (slot == 52) {
            if (sounds) SoundUtil.playPageTurn(p);
            open(gui, p, myOrders, Math.max(0, page - 1), sortBy, ascending);
            return;
        }

        if (slot == 53) {
            if (sounds) SoundUtil.playPageTurn(p);
            open(gui, p, myOrders, page + 1, sortBy, ascending);
            return;
        }

        if (!isGridSlot(slot)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(gui.getPlugin(), PDC_ORDER_ID);
        Integer orderId = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        if (orderId == null) return;

        if (sounds) SoundUtil.playClick(p);

        if (e.isShiftClick() && e.isLeftClick()) {
            int delivered = gui.orders().deliverMax(p, orderId);
            if (delivered > 0) {
                Order o = gui.orders().getOrder(orderId);
                long pay = (long) delivered * (o != null ? o.unitPrice : 0);
                p.sendMessage(Text.msg("messages.delivered")
                        .replace("{pay}", String.valueOf(pay))
                        .replace("{amt}", String.valueOf(delivered)));
            } else {
                p.sendMessage(Text.msg("messages.nothingToDeliver"));
                if (sounds) SoundUtil.playError(p);
            }
            open(gui, p, myOrders, page, sortBy, ascending);
        } else {
            gui.openDeliver(p, orderId);
        }
    }

    private boolean isGridSlot(int slot) {
        for (int s : gridSlots()) if (s == slot) return true;
        return false;
    }

    private int[] gridSlots() {
        List<Integer> s = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                s.add(row * 9 + col);
            }
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

    @Override
    public Inventory getInventory() { return inv; }
}
