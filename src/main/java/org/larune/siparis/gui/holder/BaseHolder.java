package org.larune.siparis.gui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class BaseHolder implements InventoryHolder {
    protected Inventory inv;

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
