package org.larune.siparis.model;

import org.bukkit.Material;

import java.util.UUID;

public class OrderLogSummary {
    public final int orderId;
    public final UUID ownerUuid;
    public final String ownerName;
    public final Material material;

    public final int totalAmount;     // CREATE'den
    public final long unitPrice;      // CREATE'den
    public final int deliveredAmount; // DELIVER toplamı
    public final long deliveredPay;   // DELIVER pay toplamı

    public final long createdAt;      // CREATE zamanı
    public final long lastAt;         // en son log zamanı

    public OrderLogSummary(int orderId, UUID ownerUuid, String ownerName, Material material,
                           int totalAmount, long unitPrice,
                           int deliveredAmount, long deliveredPay,
                           long createdAt, long lastAt) {
        this.orderId = orderId;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.material = material;
        this.totalAmount = totalAmount;
        this.unitPrice = unitPrice;
        this.deliveredAmount = deliveredAmount;
        this.deliveredPay = deliveredPay;
        this.createdAt = createdAt;
        this.lastAt = lastAt;
    }
}
