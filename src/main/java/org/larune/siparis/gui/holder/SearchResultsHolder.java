package org.larune.siparis.gui.holder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.model.CategoryDef;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.SearchUtil;
import org.larune.siparis.util.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class SearchResultsHolder extends BaseHolder {

    private CategoryDef category;
    private String query;
    private int page;
    private int backPage;

    public static void open(GuiManager gui, Player p, CategoryDef category, String query, int page, int backPage) {
        SearchResultsHolder h = new SearchResultsHolder();
        h.category = category;
        h.query = query == null ? "" : query.trim();
        h.page = Math.max(0, page);
        h.backPage = Math.max(0, backPage);

        h.render(gui, p);
    }

    private void render(GuiManager gui, Player p) {
        String title = Text.color("&8Arama: &f" + (query.isBlank() ? "-" : query));
        Inventory inv = gui.getPlugin().getServer().createInventory(this, 54, title);
        this.inv = inv;

        fillFrame(gui);

        // Geri / Bilgi
        inv.setItem(45, ItemUtil.named(Material.ARROW, "&eGeri", ItemUtil.lore("&7Item listesine dön")));
        inv.setItem(49, ItemUtil.named(Material.PAPER, "&bBilgi", ItemUtil.lore(
                "&7Kategori: " + Text.color(category.getDisplayName()),
                "&7Arama: &f" + (query.isBlank() ? "-" : query),
                "&7Sayfa: &f" + (page + 1)
        )));

        // Filtrele
        List<Material> base = new ArrayList<>(category.getMaterials());
        base.removeIf(Objects::isNull);

        String qNorm = SearchUtil.norm(query);

        List<Material> results = new ArrayList<>();
        if (!qNorm.isEmpty()) {
            for (Material m : base) {
                if (m.isAir()) continue;
                if (SearchUtil.matches(m, qNorm)) results.add(m);
            }
        }

        results.sort(Comparator.comparing(Enum::name));

        int perPage = 28;
        int maxPage = results.isEmpty() ? 0 : (results.size() - 1) / perPage;
        int safePage = Math.max(0, Math.min(page, maxPage));

        // Sayfalama butonları
        if (safePage > 0)
            inv.setItem(52, ItemUtil.named(Material.SPECTRAL_ARROW, "&eÖnceki", ItemUtil.lore("&7Sayfa geri")));
        if (safePage < maxPage)
            inv.setItem(53, ItemUtil.named(Material.SPECTRAL_ARROW, "&eSonraki", ItemUtil.lore("&7Sayfa ileri")));

        // Boş sonuç
        if (results.isEmpty()) {
            inv.setItem(22, ItemUtil.named(Material.BARRIER, "&cSonuç bulunamadı", ItemUtil.lore(
                    "&7Şunları dene:",
                    "&e- &fsandık, kum, taş, elmas",
                    "&e- &fCHEST, SAND, STONE",
                    "&7Aramayı değiştirmek için geri dön."
            )));
            p.openInventory(inv);
            return;
        }

        // Grid
        int start = safePage * perPage;
        int end = Math.min(results.size(), start + perPage);

        int[] slots = gridSlots();
        int idx = 0;

        for (int i = start; i < end && idx < slots.length; i++) {
            Material mat = results.get(i);
            ItemStack it = ItemUtil.materialLoreItem(mat, ItemUtil.lore(
                    "&8&m----------------------",
                    "&7İşlem: &aSipariş oluştur",
                    "&7Kategori: " + Text.color(category.getDisplayName()),
                    "&8&m----------------------",
                    "&eTıkla &7→ &aSipariş ekranı"
            ));
            inv.setItem(slots[idx++], it);
        }

        p.openInventory(inv);
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        if (slot == 45) {
            gui.openCategoryItems(p, category, backPage);
            return;
        }
        if (slot == 52) {
            open(gui, p, category, query, Math.max(0, page - 1), backPage);
            return;
        }
        if (slot == 53) {
            open(gui, p, category, query, page + 1, backPage);
            return;
        }

        if (!isGridSlot(slot)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        gui.openCreateOrder(p, category, clicked.getType());
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

    private boolean isGridSlot(int slot) {
        for (int s : gridSlots()) if (s == slot) return true;
        return false;
    }

    private void fillFrame(GuiManager gui) {
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
}
