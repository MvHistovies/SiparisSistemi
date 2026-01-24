package org.larune.siparis.npc;

import org.bukkit.Location;

import java.util.UUID;

public class NPCData {

    private final int id;
    private final String name;
    private UUID entityUuid;
    private UUID hologramUuid;
    private UUID countHologramUuid;
    private Location location;

    public NPCData(int id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public UUID getHologramUuid() {
        return hologramUuid;
    }

    public void setHologramUuid(UUID hologramUuid) {
        this.hologramUuid = hologramUuid;
    }

    public UUID getCountHologramUuid() {
        return countHologramUuid;
    }

    public void setCountHologramUuid(UUID countHologramUuid) {
        this.countHologramUuid = countHologramUuid;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}