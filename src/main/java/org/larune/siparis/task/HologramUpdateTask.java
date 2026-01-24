package org.larune.siparis.task;

import org.bukkit.scheduler.BukkitRunnable;
import org.larune.siparis.OrderPlugin;

public class HologramUpdateTask extends BukkitRunnable {

    private final OrderPlugin plugin;

    public HologramUpdateTask(OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getNpcManager() != null) {
            plugin.getNpcManager().updateAllHolograms();
        }
    }
}
