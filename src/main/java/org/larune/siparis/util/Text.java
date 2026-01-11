package org.larune.siparis.util;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Text {

    private static JavaPlugin plugin;
    private static String prefix;

    private Text() {}

    public static void init(JavaPlugin pl) {
        plugin = pl;
        prefix = color(plugin.getConfig().getString("messages.prefix", "&7"));
    }

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String msg(String path) {
        String raw = plugin.getConfig().getString(path, "&cMissing: " + path);
        return prefix + color(raw);
    }

    public static String money(long v) {
        return String.format("%,d", v).replace(',', '.');
    }
}
