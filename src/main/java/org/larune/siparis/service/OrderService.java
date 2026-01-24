package org.larune.siparis.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.larune.siparis.economy.EconomyService;
import org.larune.siparis.model.Order;
import org.larune.siparis.storage.AsyncDatabase;
import org.larune.siparis.storage.SQLiteStorage;
import org.larune.siparis.util.ItemUtil;
import org.larune.siparis.util.SoundUtil;
import org.larune.siparis.util.Text;

import java.util.*;
import java.util.function.Consumer;

public class OrderService {

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final EconomyService economy;
    private final SQLiteStorage storage;
    private final AsyncDatabase async;

    private final Map<UUID, Long> createCooldown = new HashMap<>();

    public OrderService(org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;

        this.economy = new EconomyService(plugin);
        if (!this.economy.setup()) {
            plugin.getLogger().severe("Vault bulunamadı veya Economy provider yok! Plugin devre dışı.");
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("Vault/Economy missing");
        }

        this.storage = new SQLiteStorage(plugin);
        this.storage.init();

        this.async = new AsyncDatabase(plugin);
    }

    public void shutdown() {
        async.shutdown();
        storage.close();
    }

    public AsyncDatabase getAsync() {
        return async;
    }

    public int getMaxActiveOrders() {
        return plugin.getConfig().getInt("limits.maxActiveOrders", 3);
    }

    public int countActiveOrders() {
        return storage.countActiveOrders();
    }

    public int getCooldownSeconds() {
        return plugin.getConfig().getInt("limits.createCooldownSeconds", 30);
    }

    public long minUnitPrice() {
        return plugin.getConfig().getLong("limits.minUnitPrice", 1);
    }

    public long maxUnitPrice() {
        return plugin.getConfig().getLong("limits.maxUnitPrice", 100000000);
    }

    public long totalCost(int amount, long unitPrice) {
        return (long) amount * unitPrice;
    }

    public boolean isBlacklisted(Material mat) {
        List<String> bl = plugin.getConfig().getStringList("blacklist");
        return bl.stream().anyMatch(s -> s.equalsIgnoreCase(mat.name()));
    }

    public boolean isExpiryEnabled() {
        return plugin.getConfig().getBoolean("expiry.enabled", false);
    }

    public long getExpiryHours() {
        return plugin.getConfig().getLong("expiry.hours", 24);
    }

    public long calculateExpiryTime() {
        if (!isExpiryEnabled()) return 0L;
        return System.currentTimeMillis() + (getExpiryHours() * 3600000L);
    }

    public int createOrder(Player owner, Material mat, int amount, long unitPrice) {
        if (amount <= 0 || unitPrice <= 0) return -1;
        if (unitPrice < minUnitPrice() || unitPrice > maxUnitPrice()) return -2;
        if (isBlacklisted(mat)) return -3;

        int active = storage.countActiveOrdersByOwner(owner.getUniqueId());
        if (active >= getMaxActiveOrders()) return -4;

        long now = System.currentTimeMillis();
        long last = createCooldown.getOrDefault(owner.getUniqueId(), 0L);
        long cdMs = (long) getCooldownSeconds() * 1000L;
        if (now - last < cdMs) return -5;

        long total = totalCost(amount, unitPrice);
        double bal = economy.balance(owner);
        if (bal < total) return -6;
        if (!economy.withdraw(owner, total)) return -6;

        long expiresAt = calculateExpiryTime();
        int id = storage.insertOrder(owner.getUniqueId(), mat, amount, unitPrice, expiresAt);
        createCooldown.put(owner.getUniqueId(), now);

        storage.insertLog(
                "CREATE",
                id,
                owner.getUniqueId(),
                owner.getName(),
                owner.getUniqueId(),
                owner.getName(),
                mat,
                amount,
                unitPrice,
                0,
                now
        );

        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            SoundUtil.playOrderCreated(owner);
        }

        return id;
    }

    public void createOrderAsync(Player owner, Material mat, int amount, long unitPrice, Consumer<Integer> callback) {
        async.supplyAsync(() -> createOrder(owner, mat, amount, unitPrice), callback);
    }

    public Order getOrder(int id) {
        return storage.getOrder(id);
    }

    public void getOrderAsync(int id, Consumer<Order> callback) {
        async.supplyAsync(() -> storage.getOrder(id), callback);
    }

    public List<Order> listActive(int limit, int offset) {
        return storage.listActiveOrders(limit, offset);
    }

    public void listActiveAsync(int limit, int offset, Consumer<List<Order>> callback) {
        async.supplyAsync(() -> storage.listActiveOrders(limit, offset), callback);
    }

    public List<Order> listActiveSorted(int limit, int offset, String sortBy, boolean ascending) {
        return storage.listActiveOrdersSorted(limit, offset, sortBy, ascending);
    }

    public void listActiveSortedAsync(int limit, int offset, String sortBy, boolean ascending, Consumer<List<Order>> callback) {
        async.supplyAsync(() -> storage.listActiveOrdersSorted(limit, offset, sortBy, ascending), callback);
    }

    public List<Order> listByOwner(UUID owner, int limit, int offset) {
        return storage.listOrdersByOwner(owner, limit, offset);
    }

    public void listByOwnerAsync(UUID owner, int limit, int offset, Consumer<List<Order>> callback) {
        async.supplyAsync(() -> storage.listOrdersByOwner(owner, limit, offset), callback);
    }

    public long cancelOrder(Player owner, int orderId) {
        Order o = storage.getOrder(orderId);
        if (o == null) return -1;
        if (!o.owner.equals(owner.getUniqueId())) return -2;
        if (!"ACTIVE".equalsIgnoreCase(o.status)) return -3;

        long refund = o.remainingCost();
        storage.updateOrderRemainingAndStatus(orderId, o.remainingAmount, "CANCELLED");
        economy.deposit(owner, refund);

        storage.insertLog(
                "CANCEL",
                o.id,
                o.owner,
                Bukkit.getOfflinePlayer(o.owner).getName() == null ? "Bilinmiyor" : Bukkit.getOfflinePlayer(o.owner).getName(),
                owner.getUniqueId(),
                owner.getName(),
                o.material,
                o.remainingAmount,
                o.unitPrice,
                refund,
                System.currentTimeMillis()
        );

        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            SoundUtil.playOrderCancelled(owner);
        }

        return refund;
    }

    public void cancelOrderAsync(Player owner, int orderId, Consumer<Long> callback) {
        async.supplyAsync(() -> cancelOrder(owner, orderId), callback);
    }

    public DeliveryResult deliver(Player deliverer, int orderId, int deliverAmount) {
        Order o = storage.getOrder(orderId);
        if (o == null) return DeliveryResult.error("NOT_FOUND");
        if (!"ACTIVE".equalsIgnoreCase(o.status)) return DeliveryResult.error("NOT_ACTIVE");
        if (o.isExpired()) return DeliveryResult.error("EXPIRED");
        if (deliverAmount <= 0) return DeliveryResult.error("BAD_AMOUNT");

        int max = Math.min(o.remainingAmount, deliverAmount);

        int inInv = ItemUtil.countInInventory(deliverer.getInventory(), o.material);
        if (inInv <= 0) return DeliveryResult.error("NO_ITEMS");

        int real = Math.min(inInv, max);
        int removed = ItemUtil.removeFromInventory(deliverer.getInventory(), o.material, real);
        if (removed <= 0) return DeliveryResult.error("NO_ITEMS");

        long pay = (long) removed * o.unitPrice;
        economy.deposit(deliverer, pay);

        storage.addToDeliveryBox(o.owner, o.material, removed);

        int remaining = o.remainingAmount - removed;
        String status = remaining <= 0 ? "CLOSED" : "ACTIVE";
        storage.updateOrderRemainingAndStatus(o.id, Math.max(0, remaining), status);

        String ownerName = Bukkit.getOfflinePlayer(o.owner).getName();
        if (ownerName == null) ownerName = "Bilinmiyor";

        storage.insertLog(
                "DELIVER",
                o.id,
                o.owner,
                ownerName,
                deliverer.getUniqueId(),
                deliverer.getName(),
                o.material,
                removed,
                o.unitPrice,
                pay,
                System.currentTimeMillis()
        );

        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            SoundUtil.playDeliverySuccess(deliverer);
            if ("CLOSED".equals(status)) {
                SoundUtil.playOrderCompleted(deliverer);
            }
        }

        if (plugin.getConfig().getBoolean("notifications.delivery", true)) {
            Player ownerPlayer = Bukkit.getPlayer(o.owner);
            if (ownerPlayer != null && ownerPlayer.isOnline() && !ownerPlayer.equals(deliverer)) {
                ownerPlayer.sendMessage(Text.msg("messages.deliveryNotification")
                        .replace("{player}", deliverer.getName())
                        .replace("{amount}", String.valueOf(removed))
                        .replace("{item}", o.material.name())
                        .replace("{id}", String.valueOf(o.id)));
                if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
                    SoundUtil.playNewDeliveryNotification(ownerPlayer);
                }
            }
        }

        return DeliveryResult.success(removed, pay, remaining, status);
    }

    public void deliverAsync(Player deliverer, int orderId, int deliverAmount, Consumer<DeliveryResult> callback) {
        async.runSync(() -> {
            DeliveryResult result = deliver(deliverer, orderId, deliverAmount);
            callback.accept(result);
        });
    }

    public int deliverMax(Player deliverer, int orderId) {
        Order o = storage.getOrder(orderId);
        if (o == null || !"ACTIVE".equalsIgnoreCase(o.status) || o.isExpired()) return 0;

        int inInv = ItemUtil.countInInventory(deliverer.getInventory(), o.material);
        int max = Math.min(o.remainingAmount, inInv);
        if (max <= 0) return 0;

        DeliveryResult result = deliver(deliverer, orderId, max);
        return result.ok ? result.delivered : 0;
    }

    public Map<Material, Integer> getDeliveryBox(UUID owner) {
        return storage.getDeliveryBox(owner);
    }

    public void getDeliveryBoxAsync(UUID owner, Consumer<Map<Material, Integer>> callback) {
        async.supplyAsync(() -> storage.getDeliveryBox(owner), callback);
    }

    public int withdrawFromBox(UUID owner, Material mat, int amount) {
        return storage.takeFromDeliveryBox(owner, mat, amount);
    }

    public ExpireResult expireOrder(int orderId) {
        Order o = storage.getOrder(orderId);
        if (o == null) return ExpireResult.fail();
        if (!"ACTIVE".equalsIgnoreCase(o.status)) return ExpireResult.fail();

        long refund = o.remainingCost();
        storage.updateOrderRemainingAndStatus(orderId, o.remainingAmount, "EXPIRED");

        OfflinePlayer off = Bukkit.getOfflinePlayer(o.owner);
        economy.deposit(off, refund);

        String ownerName = off.getName();
        if (ownerName == null) ownerName = "Bilinmiyor";

        storage.insertLog(
                "EXPIRE",
                o.id,
                o.owner,
                ownerName,
                o.owner,
                "SYSTEM",
                o.material,
                o.remainingAmount,
                o.unitPrice,
                refund,
                System.currentTimeMillis()
        );

        return ExpireResult.ok(refund);
    }

    public void expireOrderAsync(int orderId, Consumer<ExpireResult> callback) {
        async.supplyAsync(() -> expireOrder(orderId), callback);
    }

    public AdminDeleteResult adminDeleteOrder(int orderId, boolean refundRemaining, String adminName, UUID adminUuid) {
        Order o = storage.getOrder(orderId);
        if (o == null) return AdminDeleteResult.fail();

        long refund = 0;
        if (refundRemaining && "ACTIVE".equalsIgnoreCase(o.status)) {
            refund = o.remainingCost();
            OfflinePlayer off = Bukkit.getOfflinePlayer(o.owner);
            economy.deposit(off, refund);
        }

        boolean deleted = storage.deleteOrder(orderId);
        if (!deleted) return AdminDeleteResult.fail();

        String ownerName = Bukkit.getOfflinePlayer(o.owner).getName();
        if (ownerName == null) ownerName = "Bilinmiyor";

        storage.insertLog(
                "ADMIN_DELETE",
                o.id,
                o.owner,
                ownerName,
                adminUuid == null ? o.owner : adminUuid,
                adminName == null ? "ADMIN" : adminName,
                o.material,
                o.remainingAmount,
                o.unitPrice,
                refund,
                System.currentTimeMillis()
        );

        return AdminDeleteResult.ok(refund);
    }

    public List<org.larune.siparis.model.OrderLogSummary> listLogSummaries(int limit, int offset) {
        return storage.listLogSummaries(limit, offset);
    }

    public void listLogSummariesAsync(int limit, int offset, Consumer<List<org.larune.siparis.model.OrderLogSummary>> callback) {
        async.supplyAsync(() -> storage.listLogSummaries(limit, offset), callback);
    }

    public List<org.larune.siparis.model.OrderDeliveryStat> listDeliveriesForOrder(int orderId) {
        return storage.listDeliveriesForOrder(orderId);
    }

    public void listDeliveriesForOrderAsync(int orderId, Consumer<List<org.larune.siparis.model.OrderDeliveryStat>> callback) {
        async.supplyAsync(() -> storage.listDeliveriesForOrder(orderId), callback);
    }

    public static class ExpireResult {
        public final boolean ok;
        public final long refund;

        private ExpireResult(boolean ok, long refund) {
            this.ok = ok;
            this.refund = refund;
        }

        public static ExpireResult ok(long refund) { return new ExpireResult(true, refund); }
        public static ExpireResult fail() { return new ExpireResult(false, 0); }
    }

    public static class AdminDeleteResult {
        public final boolean ok;
        public final long refund;

        private AdminDeleteResult(boolean ok, long refund) {
            this.ok = ok;
            this.refund = refund;
        }

        public static AdminDeleteResult ok(long refund) { return new AdminDeleteResult(true, refund); }
        public static AdminDeleteResult fail() { return new AdminDeleteResult(false, 0); }
    }

    public static class DeliveryResult {
        public final boolean ok;
        public final String error;
        public final int delivered;
        public final long pay;
        public final int remaining;
        public final String status;

        private DeliveryResult(boolean ok, String error, int delivered, long pay, int remaining, String status) {
            this.ok = ok;
            this.error = error;
            this.delivered = delivered;
            this.pay = pay;
            this.remaining = remaining;
            this.status = status;
        }

        public static DeliveryResult success(int delivered, long pay, int remaining, String status) {
            return new DeliveryResult(true, null, delivered, pay, remaining, status);
        }

        public static DeliveryResult error(String code) {
            return new DeliveryResult(false, code, 0, 0, 0, null);
        }
    }
}
