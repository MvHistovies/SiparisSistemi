package org.larune.siparis.gui.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.larune.siparis.gui.GuiManager;

public class SearchChatListener implements Listener {

    private final JavaPlugin plugin;
    private final GuiManager gui;

    public SearchChatListener(JavaPlugin plugin, GuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!gui.isWaitingSearch(p.getUniqueId())) return;
        e.setCancelled(true);

        String msg = e.getMessage() == null ? "" : e.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("iptal")) {
                gui.finishSearchFromChat(p, "");
                p.sendMessage("§e[§6Sipariş§e] §7Arama iptal edildi.");
                return;
            }

            gui.finishSearchFromChat(p, msg);
        });
    }
}
