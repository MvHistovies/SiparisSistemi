package org.larune.siparis.model;

import java.util.UUID;

public class OrderDeliveryStat {
    public final UUID delivererUuid;
    public final String delivererName;
    public final int amount;
    public final long pay;

    public OrderDeliveryStat(UUID delivererUuid, String delivererName, int amount, long pay) {
        this.delivererUuid = delivererUuid;
        this.delivererName = delivererName;
        this.amount = amount;
        this.pay = pay;
    }
}
