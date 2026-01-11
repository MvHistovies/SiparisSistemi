package org.larune.siparis.gui.holder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.model.CategoryDef;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CategoryItemsHolder implements InventoryHolder {

    private final GuiManager gui;
    private final CategoryDef category;
    private final int page;
    private Inventory inv;

    private CategoryItemsHolder(GuiManager gui, CategoryDef category, int page) {
        this.gui = gui;
        this.category = category;
        this.page = page;
    }

    public static void open(GuiManager gui, Player p, CategoryDef cat, int page) {
        CategoryItemsHolder holder = new CategoryItemsHolder(gui, cat, page);

        String title = gui.getPlugin().getConfig().getString("gui.itemListTitle", "&8{category} &7- Item Seç")
                .replace("{category}", Text.color(cat.getDisplayName()));

        holder.inv = gui.getPlugin().getServer().createInventory(holder, 54, Text.color(title));
        holder.render();
        p.openInventory(holder.inv);
    }

    private void render() {
        inv.clear();
        fillFrame();

        // Geri
        inv.setItem(45, ItemUtil.named(Material.ARROW, "&eGeri", ItemUtil.lore("&7Kategori menüsü")));

        // Arama
        inv.setItem(47, ItemUtil.named(Material.COMPASS, "&bArama", ItemUtil.lore(
                "&7Türkçe/İngilizce arayabilirsin",
                "&7Örn: &fsandık&7, &ffırın&7, &felmas&7, &fchest"
        )));

        // Liste
        List<Material> mats = new ArrayList<>(category.getMaterials());
        mats.sort(Comparator.comparing(Enum::name));

        int perPage = 28;
        int maxPage = Math.max(0, (mats.size() - 1) / perPage);
        int safePage = Math.max(0, Math.min(page, maxPage));

        int start = safePage * perPage;
        int end = Math.min(mats.size(), start + perPage);

        // Bilgi
        inv.setItem(49, ItemUtil.named(Material.PAPER, "&bBilgi", ItemUtil.lore(
                "&7Kategori: " + Text.color(category.getDisplayName()),
                "&7Sayfa: &f" + (safePage + 1) + "&7/&f" + (maxPage + 1),
                "&7Toplam: &f" + mats.size()
        )));

        if (safePage > 0)
            inv.setItem(52, ItemUtil.named(Material.SPECTRAL_ARROW, "&eÖnceki", ItemUtil.lore("&7Sayfa geri")));
        if (safePage < maxPage)
            inv.setItem(53, ItemUtil.named(Material.SPECTRAL_ARROW, "&eSonraki", ItemUtil.lore("&7Sayfa ileri")));

        int[] slots = gridSlots();
        int idx = 0;

        for (int i = start; i < end && idx < slots.length; i++) {
            Material mat = mats.get(i);
            if (mat == null || mat.isAir() || !mat.isItem()) continue;

            ItemStack it = ItemUtil.materialLoreItem(mat, ItemUtil.lore(
                    "&8&m----------------------",
                    "&7İşlem: &aSipariş oluştur",
                    "&7Kategori: " + Text.color(category.getDisplayName()),
                    "&8&m----------------------",
                    "&eTıkla &7→ &aSipariş ekranı"
            ));
            inv.setItem(slots[idx++], it);
        }
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        if (slot == 45) { // geri
            gui.openCategories(p);
            return;
        }
        if (slot == 47) { // arama
            gui.beginSearch(p, category, page);
            return;
        }
        if (slot == 52) { // önceki
            gui.openCategoryItems(p, category, Math.max(0, page - 1));
            return;
        }
        if (slot == 53) { // sonraki
            gui.openCategoryItems(p, category, page + 1);
            return;
        }

        if (!isGridSlot(slot)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        Material mat = clicked.getType();
        gui.openCreateOrder(p, category, mat);
    }

    private boolean isGridSlot(int slot) {
        for (int s : gridSlots()) if (s == slot) return true;
        return false;
    }

    private int[] gridSlots() {
        List<Integer> s = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                s.add(row * 9 + col); // 10..16, 19..25, 28..34, 37..43
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
    public Inventory getInventory() {
        return inv;
    }

    public CategoryDef category() { return category; }
    public int page() { return page; }
}
