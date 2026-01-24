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
import org.larune.siparis.util.SoundUtil;
import org.larune.siparis.util.Text;

import java.util.List;

public class CategoryHolder implements InventoryHolder {

    private final GuiManager gui;
    private Inventory inv;

    private CategoryHolder(GuiManager gui) {
        this.gui = gui;
    }

    public static void open(GuiManager gui, Player p) {
        CategoryHolder holder = new CategoryHolder(gui);

        String title = Text.color(gui.getPlugin().getConfig().getString("gui.categoryTitle", "&8Kategori Seç"));
        holder.inv = gui.getPlugin().getServer().createInventory(holder, 27, title);

        holder.render();
        p.openInventory(holder.inv);

        if (gui.getPlugin().getConfig().getBoolean("sounds.enabled", true)) {
            SoundUtil.playMenuOpen(p);
        }
    }

    private void render() {
        inv.clear();
        fillFrame();

        inv.setItem(18, ItemUtil.named(Material.ARROW, "&eGeri", ItemUtil.lore("&7Ana menü")));

        List<CategoryDef> cats = gui.getCategories();

        int[] slots = {10, 12, 14, 16};
        for (int i = 0; i < cats.size() && i < slots.length; i++) {
            CategoryDef c = cats.get(i);
            int count = (c.getMaterials() == null) ? 0 : c.getMaterials().size();

            ItemStack it = ItemUtil.named(
                    c.getIcon(),
                    c.getDisplayName(),
                    ItemUtil.lore(
                            "&8&m----------------------",
                            "&7İçerik: &f" + count + " &7item",
                            "&8&m----------------------",
                            "&eTıkla &7→ &aListeyi aç"
                    )
            );
            inv.setItem(slots[i], it);
        }
    }

    public void handle(GuiManager gui, Player p, InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        boolean sounds = gui.getPlugin().getConfig().getBoolean("sounds.enabled", true);

        if (slot == 18) {
            if (sounds) SoundUtil.playClick(p);
            gui.openMainMenu(p);
            return;
        }

        int idx = switch (slot) {
            case 10 -> 0;
            case 12 -> 1;
            case 14 -> 2;
            case 16 -> 3;
            default -> -1;
        };
        if (idx < 0) return;

        List<CategoryDef> cats = gui.getCategories();
        if (idx >= cats.size()) return;

        if (sounds) SoundUtil.playClick(p);
        gui.openCategoryItems(p, cats.get(idx), 0);
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
}
