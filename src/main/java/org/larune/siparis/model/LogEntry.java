package org.larune.siparis.model;

import org.bukkit.Material;

import java.util.UUID;

public class LogEntry {
    public final long id;
    public final String type; // CREATE / DELIVER / ADMIN_DELETE / CANCEL (istersen)
    public final int orderId;

    public final UUID actor;        // işlemi yapan (sipariş açan / teslim eden / admin)
    public final String actorName;

    public final UUID owner;        // sipariş sahibi
    public final String ownerName;

    public final Material material;
    public final int amount;        // create: total, deliver: delivered
    public final long unitPrice;    // create için
    public final long pay;          // deliver için (kazanç)
    public final long createdAt;    // epoch ms

    public LogEntry(long id, String type, int orderId,
                    UUID actor, String actorName,
                    UUID owner, String ownerName,
                    Material material, int amount, long unitPrice, long pay, long createdAt) {
        this.id = id;
        this.type = type;
        this.orderId = orderId;
        this.actor = actor;
        this.actorName = actorName;
        this.owner = owner;
        this.ownerName = ownerName;
        this.material = material;
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.pay = pay;
        this.createdAt = createdAt;
    }
}
