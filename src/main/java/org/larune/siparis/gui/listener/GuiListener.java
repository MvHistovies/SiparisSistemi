package org.larune.siparis.gui.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.gui.holder.*;
import org.larune.siparis.gui.holder.SearchInputHolder;


public class GuiListener implements Listener {

    private final GuiManager gui;

    public GuiListener(JavaPlugin plugin, GuiManager gui) {
        this.gui = gui;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // Sadece üst envanter tıklamaları
        int topSize = e.getView().getTopInventory().getSize();
        if (e.getRawSlot() < 0 || e.getRawSlot() >= topSize) return;

        InventoryHolder holder = e.getView().getTopInventory().getHolder();
        if (holder == null) return;

        // Ortak güvenlik: bizim GUI’lerde item taşımasın
        e.setCancelled(true);

        // ---- Menüler ----
        if (holder instanceof MainMenuHolder h) { h.handle(gui, p, e); return; }
        if (holder instanceof CategoryHolder h) { h.handle(gui, p, e); return; }
        if (holder instanceof CategoryItemsHolder h) { h.handle(gui, p, e); return; }
        if (holder instanceof OrdersListHolder h) { h.handle(gui, p, e); return; }
        if (holder instanceof DeliveryBoxHolder h) { h.handle(gui, p, e); return; }

        // ---- Sipariş işlemleri ----
        if (holder instanceof CreateOrderHolder h) { h.handle(gui, p, e); return; }
        if (holder instanceof DeliverHolder h) { h.handle(gui, p, e); return; }

        // ---- Admin / Logs ----
        if (holder instanceof AdminMenuHolder h) { h.handle(gui, p, e); return; }
        if (holder instanceof LogsHolder h) { h.handle(gui, p, e); return; }
        if (holder instanceof OrderLogDetailHolder h) { h.handle(gui, p, e); return; }

        // ---- ARAMA ----
        if (holder instanceof SearchInputHolder h) { h.handle(gui, p, e); return; }
        if (holder instanceof SearchResultsHolder h) { h.handle(gui, p, e); }
    }
}
