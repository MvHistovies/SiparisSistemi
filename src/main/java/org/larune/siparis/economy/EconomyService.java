package org.larune.siparis.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyService {

    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public double balance(Player p) {
        return economy.getBalance(p);
    }

    public boolean withdraw(Player p, double amount) {
        return economy.withdrawPlayer(p, amount).transactionSuccess();
    }

    public boolean deposit(Player p, double amount) {
        return economy.depositPlayer(p, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer p, double amount) {
        return economy.depositPlayer(p, amount).transactionSuccess();
    }
}
