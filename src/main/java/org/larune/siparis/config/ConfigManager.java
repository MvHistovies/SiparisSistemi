package org.larune.siparis.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.larune.siparis.model.CategoryDef;
import org.larune.siparis.service.CategoryAutoPopulator;
import org.larune.siparis.util.Text;

import java.util.*;

public class ConfigManager {

    private final JavaPlugin plugin;
    private List<CategoryDef> categories = new ArrayList<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        loadCategories();
    }

    public List<CategoryDef> getCategories() {
        return categories;
    }

    private void loadCategories() {
        categories = new ArrayList<>();

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("categories");
        if (sec == null) {
            plugin.getLogger().warning("config.yml içinde 'categories:' bulunamadı.");
            return;
        }

        boolean auto = sec.getBoolean("autoPopulate", true);

        // =========================
        // EN ÖNEMLİ DEĞİŞİKLİK:
        // craftables yerine ALL ITEMS
        // =========================
        Set<Material> allItems = EnumSet.noneOf(Material.class);
        for (Material m : Material.values()) {
            if (m == null) continue;
            if (m.isAir()) continue;
            if (!m.isItem()) continue; // block/material ama item değilse alma
            allItems.add(m);
        }

        // Kategori setleri: parametre olarak "allItems" veriyoruz ki craft dışı itemler de dahil olsun
        Set<Material> maden = auto ? CategoryAutoPopulator.buildMaden(allItems) : Collections.emptySet();
        Set<Material> ciftci = auto ? CategoryAutoPopulator.buildCiftci(allItems) : Collections.emptySet();
        Set<Material> mob = auto ? CategoryAutoPopulator.buildMob(allItems) : Collections.emptySet();
        Set<Material> diger = auto ? CategoryAutoPopulator.buildDiger(allItems, maden, ciftci, mob) : Collections.emptySet();

        // configteki alt başlıklar: MADEN/CIFTCI/MOB/DIGER
        for (String key : sec.getKeys(false)) {
            if (key.equalsIgnoreCase("autoPopulate")) continue;

            ConfigurationSection c = sec.getConfigurationSection(key);
            if (c == null) continue;

            String displayName = Text.color(c.getString("displayName", key));
            Material icon = Material.matchMaterial(c.getString("icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;

            List<Material> mats = new ArrayList<>();
            if (auto) {
                switch (key.toUpperCase(Locale.ROOT)) {
                    case "MADEN" -> mats.addAll(maden);
                    case "CIFTCI" -> mats.addAll(ciftci);
                    case "MOB" -> mats.addAll(mob);
                    case "DIGER" -> mats.addAll(diger);
                    default -> mats.addAll(diger);
                }
            }

            // güvenlik filtresi
            mats.removeIf(m -> m == null || m.isAir() || !m.isItem());

            // sıralama (isteğe bağlı ama GUI daha düzgün olur)
            mats.sort(Comparator.comparing(Enum::name));

            categories.add(new CategoryDef(key, displayName, icon, mats));
        }

        plugin.getLogger().info("Kategoriler yüklendi: " + categories.size());
        plugin.getLogger().info("Toplam item havuzu: " + allItems.size());
    }
}
