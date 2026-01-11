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
    public String status; // ACTIVE/CLOSED/CANCELLED

    public Order(int id, UUID owner, Material material, int totalAmount, int remainingAmount, long unitPrice, long createdAt, String status) {
        this.id = id;
        this.owner = owner;
        this.material = material;
        this.totalAmount = totalAmount;
        this.remainingAmount = remainingAmount;
        this.unitPrice = unitPrice;
        this.createdAt = createdAt;
        this.status = status;
    }

    public long totalCost() {
        return (long) totalAmount * unitPrice;
    }

    public long remainingCost() {
        return (long) remainingAmount * unitPrice;
    }
}
