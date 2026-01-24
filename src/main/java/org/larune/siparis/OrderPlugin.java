package org.larune.siparis;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.larune.siparis.command.SiparisCommand;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.gui.listener.GuiListener;
import org.larune.siparis.gui.listener.SearchChatListener;
import org.larune.siparis.npc.NPCListener;
import org.larune.siparis.npc.NPCManager;
import org.larune.siparis.service.OrderService;
import org.larune.siparis.task.ExpiryTask;
import org.larune.siparis.task.HologramUpdateTask;
import org.larune.siparis.util.Text;

public class OrderPlugin extends JavaPlugin {

    private GuiManager gui;
    private OrderService orderService;
    private ExpiryTask expiryTask;
    private NPCManager npcManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Text.init(this);

        this.orderService = new OrderService(this);
        this.gui = new GuiManager(this, orderService);

        new SiparisCommand(this);
        new GuiListener(this, gui);
        new SearchChatListener(this, gui);

        if (getConfig().getBoolean("expiry.enabled", false)) {
            this.expiryTask = new ExpiryTask(this);
            long interval = getConfig().getLong("expiry.checkIntervalSeconds", 60) * 20L;
            expiryTask.runTaskTimerAsynchronously(this, interval, interval);
        }

        if (getConfig().getBoolean("npc.enabled", true)) {
            setupNPC();
        }

        getLogger().info("SiparisSistemi aktif!");
    }

    private void setupNPC() {
        try {
            this.npcManager = new NPCManager(this);
            Bukkit.getPluginManager().registerEvents(new NPCListener(this), this);

            Bukkit.getScheduler().runTaskLater(this, () -> {
                npcManager.respawnAllNPCs();
            }, 40L);

            long updateInterval = getConfig().getLong("npc.updateIntervalSeconds", 10) * 20L;
            new HologramUpdateTask(this).runTaskTimer(this, 60L, updateInterval);

            getLogger().info("NPC sistemi yuklendi!");
        } catch (Exception e) {
            getLogger().warning("NPC sistemi yuklenemedi: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (npcManager != null) {
            npcManager.shutdown();
        }
        if (expiryTask != null) {
            expiryTask.cancel();
        }
        if (orderService != null) {
            orderService.shutdown();
        }
        getLogger().info("SiparisSistemi kapatildi.");
    }

    public GuiManager gui() {
        return gui;
    }

    public OrderService orders() {
        return orderService;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }
}
