package org.larune.siparis.npc;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.larune.siparis.OrderPlugin;

import java.util.UUID;

public class NPCListener implements Listener {

    private final OrderPlugin plugin;

    public NPCListener(OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Villager)) return;

        if (plugin.getNpcManager() == null) return;
        if (!plugin.getNpcManager().isNPC(entity.getUniqueId())) return;

        event.setCancelled(true);
        plugin.gui().openMainMenu(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (plugin.getNpcManager() == null) return;

        if (plugin.getNpcManager().isHologram(entity.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (entity instanceof Villager && plugin.getNpcManager().isNPC(entity.getUniqueId())) {
            event.setCancelled(true);
            plugin.gui().openMainMenu(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (plugin.getNpcManager() == null) return;

        UUID uuid = event.getEntity().getUniqueId();
        if (plugin.getNpcManager().isNPC(uuid) || plugin.getNpcManager().isHologram(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (plugin.getNpcManager() == null) return;

        UUID uuid = event.getEntity().getUniqueId();
        if (plugin.getNpcManager().isNPC(uuid) || plugin.getNpcManager().isHologram(uuid)) {
            event.setCancelled(true);

            if (event.getDamager() instanceof Player player) {
                if (plugin.getNpcManager().isNPC(uuid)) {
                    plugin.gui().openMainMenu(player);
                }
            }
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (plugin.getNpcManager() == null) return;

        UUID uuid = event.getEntity().getUniqueId();
        if (plugin.getNpcManager().isNPC(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (plugin.getNpcManager() == null) return;

        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Villager villager) {
                if (plugin.getNpcManager().isNPC(entity.getUniqueId())) {
                    villager.setAI(false);
                    villager.setInvulnerable(true);
                    villager.setSilent(true);
                }
            }
        }
    }
}