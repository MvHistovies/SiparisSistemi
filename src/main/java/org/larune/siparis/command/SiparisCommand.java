package org.larune.siparis.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.larune.siparis.OrderPlugin;

import java.util.ArrayList;
import java.util.List;

public class SiparisCommand implements CommandExecutor, TabExecutor {

    private final OrderPlugin plugin;

    public SiparisCommand(OrderPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("siparis").setExecutor(this);
        plugin.getCommand("siparis").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Bu komut sadece oyuncular için.");
            return true;
        }

        // /siparis
        if (args.length == 0) {
            plugin.gui().openMainMenu(p);
            return true;
        }

        // /siparis admin
        if (args[0].equalsIgnoreCase("admin")) {
            if (!p.hasPermission("siparis.admin")) {
                p.sendMessage("§cBuna yetkin yok.");
                return true;
            }
            plugin.gui().openAdmin(p);
            return true;
        }

        // bilinmeyen
        p.sendMessage("§cKullanım: /siparis");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();

        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (p.hasPermission("siparis.admin")) out.add("admin");
        }
        return out;
    }
}
