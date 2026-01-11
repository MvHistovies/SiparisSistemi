package org.larune.siparis;

import org.bukkit.plugin.java.JavaPlugin;
import org.larune.siparis.command.SiparisCommand;
import org.larune.siparis.gui.GuiManager;
import org.larune.siparis.gui.listener.GuiListener;
import org.larune.siparis.gui.listener.SearchChatListener;
import org.larune.siparis.service.OrderService;
import org.larune.siparis.util.Text;

public class OrderPlugin extends JavaPlugin {

    private GuiManager gui;
    private OrderService orderService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Text.init(this);

        // SENDEKİ OrderService constructor'ı: OrderService(JavaPlugin)
        this.orderService = new OrderService(this);

        // GuiManager: (OrderPlugin, OrderService)
        this.gui = new GuiManager(this, orderService);

        new SiparisCommand(this);
        new GuiListener(this, gui);

        // Arama chat listener (arama çalışması için şart)
        new SearchChatListener(this, gui);

        getLogger().info("SiparisSistemi aktif!");
    }

    public GuiManager gui() {
        return gui;
    }

    public OrderService orders() {
        return orderService;
    }
}
