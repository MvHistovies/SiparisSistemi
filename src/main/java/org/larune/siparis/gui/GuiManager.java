package org.larune.siparis.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.larune.siparis.OrderPlugin;
import org.larune.siparis.gui.holder.*;
import org.larune.siparis.model.CategoryDef;
import org.larune.siparis.service.OrderService;
import org.larune.siparis.util.Text;

import java.util.*;
import java.util.stream.Collectors;

public class GuiManager {

    private final OrderPlugin plugin;
    private final OrderService orders;

    private List<CategoryDef> cachedCategories = new ArrayList<>();
    private final Map<UUID, SearchContext> waitingSearch = new HashMap<>();

    public GuiManager(OrderPlugin plugin, OrderService orders) {
        this.plugin = plugin;
        this.orders = orders;
        reloadCategories();
    }

    public OrderPlugin getPlugin() {
        return plugin;
    }

    public OrderService orders() {
        return orders;
    }

    public void openMainMenu(Player p) {
        try {
            MainMenuHolder.open(this, p);
        } catch (Throwable ignored) {
            openCategories(p);
        }
    }

    public void openCategories(Player p) {
        CategoryHolder.open(this, p);
    }

    public void openCategoryItems(Player p, CategoryDef cat, int page) {
        CategoryItemsHolder.open(this, p, cat, page);
    }

    public void openCreateOrder(Player p, CategoryDef cat, Material mat) {
        CreateOrderHolder.open(this, p, cat, mat);
    }

    public void openDeliver(Player p, int orderId) {
        DeliverHolder.open(this, p, orderId);
    }

    public void openOrders(Player p, boolean myOrders, int page) {
        OrdersListHolder.open(this, p, myOrders, page);
    }

    public void openBox(Player p, int page) {
        DeliveryBoxHolder.open(this, p, page);
    }

    public void openAdmin(Player p) {
        try {
            AdminMenuHolder.open(this, p);
        } catch (Throwable t) {
            p.sendMessage(Text.color("&cAdmin menüsü bulunamadı (AdminMenuHolder yok)."));
        }
    }

    public void openLogs(Player p, int page) {
        try {
            LogsHolder.open(this, p, page);
        } catch (Throwable t) {
            p.sendMessage(Text.color("&cLog menüsü bulunamadı (LogsHolder yok)."));
        }
    }
    public void openLogDetail(Player p, int orderId, int backPage) {
        try {
            OrderLogDetailHolder.open(this, p, orderId, backPage);
        } catch (Throwable t) {

            p.sendMessage(Text.color("&cLog detay menüsü bulunamadı (OrderLogDetailHolder yok)."));
        }
    }
    public void beginAdminCancel(Player p, boolean myOnly) {
        openOrders(p, myOnly, 0);
    }

    public void beginSearch(Player p, CategoryDef category, int backPage) {
        try {
            SearchInputHolder.open(this, p, category, backPage);
        } catch (Throwable ignored) {
            openSearchResults(p, category, "", 0, backPage);
        }
    }

    public void openSearchResults(Player p, CategoryDef category, String query, int page, int backPage) {
        SearchResultsHolder.open(this, p, category, query, page, backPage);
    }

    public boolean isWaitingSearch(UUID uuid) {
        return waitingSearch.containsKey(uuid);
    }

    public void setWaitingSearch(UUID uuid, CategoryDef cat, int backPage) {
        waitingSearch.put(uuid, new SearchContext(cat, backPage));
    }

    public void finishSearchFromChat(Player p, String rawMessage) {
        SearchContext ctx = waitingSearch.remove(p.getUniqueId());
        if (ctx == null) return;

        String q = rawMessage == null ? "" : rawMessage.trim();

        if (q.isBlank()) {
            openCategoryItems(p, ctx.category, ctx.backPage);
            return;
        }

        if (q.length() > 32) q = q.substring(0, 32);

        openSearchResults(p, ctx.category, q, 0, ctx.backPage);
    }

    private static class SearchContext {
        final CategoryDef category;
        final int backPage;

        SearchContext(CategoryDef category, int backPage) {
            this.category = category;
            this.backPage = backPage;
        }
    }

    public List<CategoryDef> getCategories() {
        return cachedCategories;
    }

    public void reloadCategories() {
        cachedCategories = new ArrayList<>();

        boolean auto = plugin.getConfig().getBoolean("categories.autoPopulate", true);

        ConfigurationSection catsSec = plugin.getConfig().getConfigurationSection("categories");
        if (catsSec == null) return;

        Set<String> keys = catsSec.getKeys(false).stream()
                .filter(k -> !k.equalsIgnoreCase("autoPopulate"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, List<Material>> autoMap = auto ? autoMaterialsFor(keys) : Collections.emptyMap();

        for (String key : keys) {
            ConfigurationSection c = catsSec.getConfigurationSection(key);
            if (c == null) continue;

            String displayName = Text.color(c.getString("displayName", "&f" + key));
            Material icon = Material.matchMaterial(c.getString("icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;

            List<Material> mats;

            List<String> cfgMats = c.getStringList("materials");
            if (cfgMats != null && !cfgMats.isEmpty()) {
                mats = cfgMats.stream()
                        .map(Material::matchMaterial)
                        .filter(Objects::nonNull)
                        .filter(m -> !m.isAir())
                        .distinct()
                        .toList();
            } else {

                mats = auto ? autoMap.getOrDefault(key.toUpperCase(Locale.ROOT), List.of()) : List.of();
            }

            cachedCategories.add(new CategoryDef(key, displayName, icon, mats));
        }
    }

    private Map<String, List<Material>> autoMaterialsFor(Set<String> keys) {
        List<Material> all = Arrays.stream(Material.values())
                .filter(Objects::nonNull)
                .filter(m -> !m.isAir())
                .toList();

        Set<Material> maden = new LinkedHashSet<>();
        Set<Material> ciftci = new LinkedHashSet<>();
        Set<Material> mob = new LinkedHashSet<>();
        Set<Material> diger = new LinkedHashSet<>(all);

        for (Material m : all) {
            String n = m.name();

            boolean oreLike = n.endsWith("_ORE") || n.contains("ORE");
            boolean rawLike = n.startsWith("RAW_");
            boolean ingotLike = n.endsWith("_INGOT") || n.endsWith("_NUGGET");
            boolean gemLike = Set.of("DIAMOND", "EMERALD", "LAPIS_LAZULI", "REDSTONE", "COAL", "QUARTZ", "AMETHYST_SHARD").contains(n);
            boolean mineralBlock = n.endsWith("_BLOCK") && (n.contains("IRON") || n.contains("GOLD") || n.contains("DIAMOND") ||
                    n.contains("EMERALD") || n.contains("COPPER") || n.contains("NETHERITE") || n.contains("REDSTONE") ||
                    n.contains("LAPIS") || n.contains("COAL"));

            if (oreLike || rawLike || ingotLike || gemLike || mineralBlock) {
                maden.add(m);
                continue;
            }

            boolean farm = n.contains("WHEAT") || n.contains("CARROT") || n.contains("POTATO") || n.contains("BEETROOT")
                    || n.contains("MELON") || n.contains("PUMPKIN") || n.contains("SUGAR_CANE") || n.contains("SEEDS")
                    || n.contains("BREAD") || n.contains("CAKE") || n.contains("COOKIE") || n.contains("STEW") || n.contains("SOUP")
                    || n.contains("BERRIES") || n.contains("KELP") || n.contains("COCOA") || n.contains("HONEY") || n.contains("MILK")
                    || n.contains("HAY_BLOCK") || n.contains("DRIED_KELP");

            if (farm) {
                ciftci.add(m);
                continue;
            }

            boolean mobDrop = n.contains("ROTTEN_FLESH") || n.contains("BONE") || n.contains("STRING") || n.contains("SPIDER_EYE")
                    || n.contains("GUNPOWDER") || n.contains("ENDER_PEARL") || n.contains("SLIME") || n.contains("BLAZE")
                    || n.contains("MAGMA") || n.contains("GHAST") || n.contains("LEATHER") || n.contains("FEATHER")
                    || n.contains("PRISMARINE") || n.contains("PHANTOM") || n.contains("WITHER") || n.contains("SHULKER")
                    || n.contains("RABBIT") || n.contains("MUTTON") || n.contains("BEEF") || n.contains("PORK");

            if (mobDrop) mob.add(m);
        }

        diger.removeAll(maden);
        diger.removeAll(ciftci);
        diger.removeAll(mob);

        Map<String, List<Material>> map = new HashMap<>();
        map.put("MADEN", new ArrayList<>(maden));
        map.put("CIFTCI", new ArrayList<>(ciftci));
        map.put("MOB", new ArrayList<>(mob));
        map.put("DIGER", new ArrayList<>(diger));

        for (String k : keys) {
            String up = k.toUpperCase(Locale.ROOT);
            map.putIfAbsent(up, new ArrayList<>(diger));
        }
        return map;
    }
}
