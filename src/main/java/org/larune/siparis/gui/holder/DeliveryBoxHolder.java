package org.larune.siparis.gui.holder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.SoundUtil;
import org.larune.siparis.util.Text;

import java.util.*;

public class DeliveryBoxHolder implements InventoryHolder {

    private final GuiManager gui;
    private final int page;
    private Inventory inv;

    private DeliveryBoxHolder(GuiManager gui, int page) {
        this.gui = gui;
        this.page = page;
    }

    public static void open(GuiManager gui, Player p, int page) {
        DeliveryBoxHolder holder = new DeliveryBoxHolder(gui, page);
        String title = Text.color(gui.getPlugin().getConfig().getString("gui.boxTitle", "&8Teslim Kutusu"));
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

        Map<Material, Integer> box = gui.orders().getDeliveryBox(p.getUniqueId());
        List<Map.Entry<Material, Integer>> entries = new ArrayList<>(box.entrySet());
        entries.sort(Comparator.comparing(e -> e.getKey().name()));

        int perPage = 28;
        int maxPage = Math.max(0, (entries.size() - 1) / perPage);
        int safePage = Math.max(0, Math.min(page, maxPage));

        inv.setItem(49, ItemUtil.named(Material.PAPER, "&bBilgi", ItemUtil.lore(
                "&7Sayfa: &f" + (safePage + 1) + "&7/&f" + (maxPage + 1),
                "&7Çeşit: &f" + entries.size(),
                "&eSol: &a1 &7| &eSağ: &a64 &7| &eShift: &aHepsi"
        )));

        if (safePage > 0) inv.setItem(52, ItemUtil.named(Material.SPECTRAL_ARROW, "&eÖnceki", ItemUtil.lore("&7Sayfa geri")));
        if (safePage < maxPage) inv.setItem(53, ItemUtil.named(Material.SPECTRAL_ARROW, "&eSonraki", ItemUtil.lore("&7Sayfa ileri")));

        int start = safePage * perPage;
        int end = Math.min(entries.size(), start + perPage);

        int[] slots = gridSlots();
        int idx = 0;

        for (int i = start; i < end && idx < slots.length; i++) {
            Material mat = entries.get(i).getKey();
            int amt = entries.get(i).getValue();

            ItemStack it = ItemUtil.materialLoreItem(mat, ItemUtil.lore(
                    "&b&lTeslim Kutusu",
                    "&8&m----------------------",
                    "&7Miktar: &f" + amt,
                    "&8&m----------------------",
                    "&eSol Tık &7→ &a1 adet al",
                    "&eSağ Tık &7→ &a64 adet al",
                    "&eShift &7→ &aHepsini al"
            ));

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
        if (slot == 52) {
            if (sounds) SoundUtil.playPageTurn(p);
            open(gui, p, Math.max(0, page - 1));
            return;
        }
        if (slot == 53) {
            if (sounds) SoundUtil.playPageTurn(p);
            open(gui, p, page + 1);
            return;
        }

        if (!isGridSlot(slot)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        Material mat = clicked.getType();

        int want;
        ClickType ct = e.getClick();

        if (ct.isShiftClick()) want = Integer.MAX_VALUE;
        else if (ct.isRightClick()) want = 64;
        else want = 1;

        int taken = gui.orders().withdrawFromBox(p.getUniqueId(), mat, want);
        if (taken <= 0) {
            p.sendMessage(Text.msg("messages.boxEmpty"));
            if (sounds) SoundUtil.playError(p);
            return;
        }

        ItemUtil.giveOrDrop(p, new ItemStack(mat, taken));

        if (sounds) SoundUtil.playBoxWithdraw(p);

        p.sendMessage(Text.msg("messages.withdrew")
                .replace("{mat}", mat.name())
                .replace("{amt}", String.valueOf(taken)));

        open(gui, p, page);
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
