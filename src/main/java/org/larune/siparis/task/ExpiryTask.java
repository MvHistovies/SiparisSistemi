package org.larune.siparis.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.larune.siparis.OrderPlugin;
import org.larune.siparis.model.Order;
import org.larune.siparis.util.SoundUtil;
import org.larune.siparis.util.Text;

import java.util.HashSet;
import java.util.Set;

public class ExpiryTask extends BukkitRunnable {

    private final OrderPlugin plugin;
    private final Set<Integer> warnedOrders = new HashSet<>();

    public ExpiryTask(OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("expiry.enabled", false)) return;

        long now = System.currentTimeMillis();
        long warnMs = plugin.getConfig().getLong("expiry.warnBeforeMinutes", 30) * 60000L;

        plugin.orders().listActiveAsync(1000, 0, orders -> {
            for (Order o : orders) {
                if (o.expiresAt <= 0) continue;

                long remaining = o.expiresAt - now;

                if (remaining <= 0) {
                    handleExpiry(o);
                    warnedOrders.remove(o.id);
                } else if (remaining <= warnMs && !warnedOrders.contains(o.id)) {
                    handleWarning(o, remaining);
                    warnedOrders.add(o.id);
                }
            }
        });
    }

    private void handleExpiry(Order o) {
        plugin.orders().expireOrderAsync(o.id, result -> {
            if (result.ok) {
                Player owner = Bukkit.getPlayer(o.owner);
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage(Text.msg("messages.orderExpired")
                            .replace("{id}", String.valueOf(o.id))
                            .replace("{refund}", Text.money(result.refund)));
                    SoundUtil.playOrderExpired(owner);
                }
            }
        });
    }

    private void handleWarning(Order o, long remainingMs) {
        Player owner = Bukkit.getPlayer(o.owner);
        if (owner != null && owner.isOnline()) {
            long mins = remainingMs / 60000;
            owner.sendMessage(Text.msg("messages.orderExpiryWarning")
                    .replace("{id}", String.valueOf(o.id))
                    .replace("{minutes}", String.valueOf(mins)));
            SoundUtil.playExpiryWarning(owner);
        }
    }

    public void clearWarning(int orderId) {
        warnedOrders.remove(orderId);
    }
}
