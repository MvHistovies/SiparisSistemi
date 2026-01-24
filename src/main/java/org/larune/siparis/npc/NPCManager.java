package org.larune.siparis.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.larune.siparis.OrderPlugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager {

    private final OrderPlugin plugin;
    private final Map<Integer, NPCData> npcs = new ConcurrentHashMap<>();
    private final Set<UUID> npcUuids = new HashSet<>();
    private final Set<UUID> hologramUuids = new HashSet<>();
    private int nextId = 1;

    public NPCManager(OrderPlugin plugin) {
        this.plugin = plugin;
        loadNPCs();
    }

    public NPCData createNPC(String name, Location loc) {
        int id = nextId++;
        NPCData data = new NPCData(id, name, loc);
        npcs.put(id, data);
        spawnEntity(data);
        saveNPCs();
        return data;
    }

    public boolean removeNPC(int id) {
        NPCData data = npcs.remove(id);
        if (data == null) return false;

        if (data.getCountHologramUuid() != null) {
            hologramUuids.remove(data.getCountHologramUuid());
            Entity countHolo = Bukkit.getEntity(data.getCountHologramUuid());
            if (countHolo != null) countHolo.remove();
        }

        if (data.getHologramUuid() != null) {
            hologramUuids.remove(data.getHologramUuid());
            Entity hologram = Bukkit.getEntity(data.getHologramUuid());
            if (hologram != null) hologram.remove();
        }

        if (data.getEntityUuid() != null) {
            npcUuids.remove(data.getEntityUuid());
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity != null) entity.remove();
        }

        plugin.getConfig().set("npc.list." + id, null);
        plugin.saveConfig();
        return true;
    }

    public NPCData getNPC(int id) {
        return npcs.get(id);
    }

    public Collection<NPCData> getAllNPCs() {
        return npcs.values();
    }

    public boolean isNPC(UUID uuid) {
        return npcUuids.contains(uuid);
    }

    public boolean isHologram(UUID uuid) {
        return hologramUuids.contains(uuid);
    }

    public NPCData getNPCByUuid(UUID uuid) {
        for (NPCData data : npcs.values()) {
            if (uuid.equals(data.getEntityUuid())) {
                return data;
            }
        }
        return null;
    }

    private void spawnEntity(NPCData data) {
        if (data.getLocation() == null || data.getLocation().getWorld() == null) {
            plugin.getLogger().warning("NPC spawn edilemedi: Location veya World null");
            return;
        }

        try {
            Location loc = data.getLocation().clone();

            Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
            villager.setCustomNameVisible(false);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setProfession(Villager.Profession.NITWIT);
            villager.setVillagerType(Villager.Type.PLAINS);

            Location holoLoc = loc.clone().add(0, 2.3, 0);
            ArmorStand hologram = (ArmorStand) loc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
            String coloredName = colorize(data.getName());
            hologram.setCustomName(coloredName);
            hologram.setCustomNameVisible(true);
            hologram.setVisible(false);
            hologram.setGravity(false);
            hologram.setInvulnerable(true);
            hologram.setMarker(true);
            hologram.setPersistent(true);
            hologram.setSmall(true);

            Location countLoc = loc.clone().add(0, 2.0, 0);
            ArmorStand countHolo = (ArmorStand) loc.getWorld().spawnEntity(countLoc, EntityType.ARMOR_STAND);
            int activeCount = plugin.orders().countActiveOrders();
            countHolo.setCustomName(colorize("&7Aktif Siparis: &a" + activeCount));
            countHolo.setCustomNameVisible(true);
            countHolo.setVisible(false);
            countHolo.setGravity(false);
            countHolo.setInvulnerable(true);
            countHolo.setMarker(true);
            countHolo.setPersistent(true);
            countHolo.setSmall(true);

            data.setEntityUuid(villager.getUniqueId());
            data.setHologramUuid(hologram.getUniqueId());
            data.setCountHologramUuid(countHolo.getUniqueId());
            npcUuids.add(villager.getUniqueId());
            hologramUuids.add(hologram.getUniqueId());
            hologramUuids.add(countHolo.getUniqueId());

            plugin.getLogger().info("NPC spawn edildi: ID=" + data.getId());
        } catch (Exception e) {
            plugin.getLogger().warning("NPC spawn hatasi: " + e.getMessage());
        }
    }

    private String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "\u00A7");
    }

    public void respawnAllNPCs() {
        for (NPCData data : npcs.values()) {
            boolean needsRespawn = false;

            if (data.getEntityUuid() != null) {
                Entity existing = Bukkit.getEntity(data.getEntityUuid());
                if (existing == null || existing.isDead()) {
                    needsRespawn = true;
                }
            } else {
                needsRespawn = true;
            }

            if (data.getHologramUuid() != null) {
                Entity holo = Bukkit.getEntity(data.getHologramUuid());
                if (holo == null || holo.isDead()) {
                    needsRespawn = true;
                }
            }

            if (data.getCountHologramUuid() != null) {
                Entity countHolo = Bukkit.getEntity(data.getCountHologramUuid());
                if (countHolo == null || countHolo.isDead()) {
                    needsRespawn = true;
                }
            }

            if (needsRespawn) {
                if (data.getEntityUuid() != null) {
                    Entity old = Bukkit.getEntity(data.getEntityUuid());
                    if (old != null) old.remove();
                }
                if (data.getHologramUuid() != null) {
                    Entity oldHolo = Bukkit.getEntity(data.getHologramUuid());
                    if (oldHolo != null) oldHolo.remove();
                }
                if (data.getCountHologramUuid() != null) {
                    Entity oldCount = Bukkit.getEntity(data.getCountHologramUuid());
                    if (oldCount != null) oldCount.remove();
                }
                spawnEntity(data);
            }
        }
        saveNPCs();
    }

    private void saveNPCs() {
        for (NPCData data : npcs.values()) {
            String path = "npc.list." + data.getId();
            plugin.getConfig().set(path + ".name", data.getName());
            plugin.getConfig().set(path + ".world", data.getLocation().getWorld().getName());
            plugin.getConfig().set(path + ".x", data.getLocation().getX());
            plugin.getConfig().set(path + ".y", data.getLocation().getY());
            plugin.getConfig().set(path + ".z", data.getLocation().getZ());
            plugin.getConfig().set(path + ".yaw", data.getLocation().getYaw());
            plugin.getConfig().set(path + ".pitch", data.getLocation().getPitch());
            if (data.getEntityUuid() != null) {
                plugin.getConfig().set(path + ".uuid", data.getEntityUuid().toString());
            }
            if (data.getHologramUuid() != null) {
                plugin.getConfig().set(path + ".hologramUuid", data.getHologramUuid().toString());
            }
            if (data.getCountHologramUuid() != null) {
                plugin.getConfig().set(path + ".countHologramUuid", data.getCountHologramUuid().toString());
            }
        }
        plugin.getConfig().set("npc.nextId", nextId);
        plugin.saveConfig();
    }

    private void loadNPCs() {
        nextId = plugin.getConfig().getInt("npc.nextId", 1);

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("npc.list");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String name = section.getString(key + ".name", "NPC");
                String worldName = section.getString(key + ".world");
                double x = section.getDouble(key + ".x");
                double y = section.getDouble(key + ".y");
                double z = section.getDouble(key + ".z");
                float yaw = (float) section.getDouble(key + ".yaw");
                float pitch = (float) section.getDouble(key + ".pitch");
                String uuidStr = section.getString(key + ".uuid");
                String holoUuidStr = section.getString(key + ".hologramUuid");
                String countHoloUuidStr = section.getString(key + ".countHologramUuid");

                if (worldName == null || Bukkit.getWorld(worldName) == null) continue;

                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                NPCData data = new NPCData(id, name, loc);

                if (uuidStr != null) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        data.setEntityUuid(uuid);
                        npcUuids.add(uuid);
                    } catch (Exception ignored) {}
                }

                if (holoUuidStr != null) {
                    try {
                        UUID uuid = UUID.fromString(holoUuidStr);
                        data.setHologramUuid(uuid);
                        hologramUuids.add(uuid);
                    } catch (Exception ignored) {}
                }

                if (countHoloUuidStr != null) {
                    try {
                        UUID uuid = UUID.fromString(countHoloUuidStr);
                        data.setCountHologramUuid(uuid);
                        hologramUuids.add(uuid);
                    } catch (Exception ignored) {}
                }

                npcs.put(id, data);

                if (id >= nextId) nextId = id + 1;
            } catch (Exception ignored) {}
        }
    }

    public void shutdown() {
        saveNPCs();
    }

    public void updateAllHolograms() {
        int activeCount = plugin.orders().countActiveOrders();
        String countText = colorize("&7Aktif Siparis: &a" + activeCount);

        for (NPCData data : npcs.values()) {
            if (data.getCountHologramUuid() != null) {
                Entity entity = Bukkit.getEntity(data.getCountHologramUuid());
                if (entity instanceof ArmorStand armorStand) {
                    armorStand.setCustomName(countText);
                }
            }
        }
    }
}