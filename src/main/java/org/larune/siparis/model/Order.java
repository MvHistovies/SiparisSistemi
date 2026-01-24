package org.larune.siparis.model;

import org.bukkit.Material;

import java.util.UUID;

public class Order {
    public final int id;
    public final UUID owner;
    public final Material material;
    public final int totalAmount;
    public int remainingAmount;
    public final long unitPrice;
    public final long createdAt;
    public long expiresAt;
    public String status;

    public Order(int id, UUID owner, Material material, int totalAmount, int remainingAmount,
                 long unitPrice, long createdAt, long expiresAt, String status) {
        this.id = id;
        this.owner = owner;
        this.material = material;
        this.totalAmount = totalAmount;
        this.remainingAmount = remainingAmount;
        this.unitPrice = unitPrice;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public Order(int id, UUID owner, Material material, int totalAmount, int remainingAmount,
                 long unitPrice, long createdAt, String status) {
        this(id, owner, material, totalAmount, remainingAmount, unitPrice, createdAt, 0L, status);
    }

    public long totalCost() {
        return (long) totalAmount * unitPrice;
    }

    public long remainingCost() {
        return (long) remainingAmount * unitPrice;
    }

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }

    public long getRemainingTime() {
        if (expiresAt <= 0) return -1;
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    public String getFormattedRemainingTime() {
        long remaining = getRemainingTime();
        if (remaining < 0) return "∞";
        if (remaining <= 0) return "Süresi Doldu";

        long hours = remaining / 3600000;
        long minutes = (remaining % 3600000) / 60000;

        if (hours > 0) {
            return hours + "s " + minutes + "dk";
        }
        return minutes + "dk";
    }
}
