package org.larune.siparis.gui.holder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.model.CategoryDef;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.Text;

import java.util.List;

public class SearchInputHolder extends BaseHolder {

    private CategoryDef category;
    private int backPage;

    public static void open(GuiManager gui, Player p, CategoryDef category, int backPage) {
        SearchInputHolder holder = new SearchInputHolder();
        holder.category = category;
        holder.backPage = backPage;
        holder.render(gui, p);
    }

    private void render(GuiManager gui, Player p) {
        String title = Text.color("&8Arama: " + Text.color(category.getDisplayName()));
        Inventory inv = Bukkit.createInventory(this, 27, title);
        this.inv = inv;

        // çerçeve
        fillFrame(gui);

        inv.setItem(11, ItemUtil.named(Material.NAME_TAG, "&aArama yaz",
                ItemUtil.lore(
                        "&7Tıkla ve sohbetten yaz.",
                        "&7Örnek: &fsandık",
                        "&8Not: yazdıktan sonra otomatik sonuç açılır."
                )));

        inv.setItem(13, ItemUtil.named(Material.PAPER, "&bBilgi",
                ItemUtil.lore(
                        "&7Kategori: " + Text.color(category.getDisplayName()),
                        "&7Arama için sohbeti kullanacağız."
                )));

        inv.setItem(15, ItemUtil.named(Material.BARRIER, "&cİptal",
                ItemUtil.lore("&7Geri dön")));

        inv.setItem(18, ItemUtil.named(Material.ARROW, "&eGeri",
                ItemUtil.lore("&7Item listesine dön")));

        p.openInventory(inv);
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        if (slot == 18 || slot == 15) {
            gui.openCategoryItems(p, category, backPage);
            return;
        }

        if (slot == 11) {
            // chatten arama beklemeye al
            gui.setWaitingSearch(p.getUniqueId(), category, backPage);

            p.closeInventory();
            p.sendMessage(Text.color("&e[&6Sipariş&e] &fArama kelimesini sohbete yaz. &7(Örn: &fsandık&7)"));
            p.sendMessage(Text.color("&7İptal: &fiptal &7yazabilirsin."));

            return;
        }
    }

    private void fillFrame(GuiManager gui) {
        boolean enabled = gui.getPlugin().getConfig().getBoolean("guiTheme.frame.enabled", true);
        if (!enabled) return;

        Material pane = Material.matchMaterial(gui.getPlugin().getConfig().getString("guiTheme.frame.material", "GRAY_STAINED_GLASS_PANE"));
        if (pane == null) pane = Material.GRAY_STAINED_GLASS_PANE;

        var frame = ItemUtil.named(pane, gui.getPlugin().getConfig().getString("guiTheme.frame.name", " "), List.of());

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
