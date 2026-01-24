package org.larune.siparis.util;

import org.bukkit.Material;

import java.util.*;

public final class SearchUtil {

    private SearchUtil() {}
    private static final Map<String, List<String>> TR_ALIASES = new HashMap<>();
    static {

        put("sandık", "CHEST", "TRAPPED_CHEST", "BARREL", "ENDER_CHEST", "SHULKER_BOX");
        put("kutu", "CHEST", "BARREL", "SHULKER_BOX");
        put("kum", "SAND", "RED_SAND", "SANDSTONE");
        put("cam", "GLASS", "GLASS_PANE");
        put("tahta", "PLANKS", "WOOD");
        put("odun", "LOG", "WOOD");
        put("kütük", "LOG", "WOOD");
        put("kömür", "COAL", "CHARCOAL");
        put("elmas", "DIAMOND");
        put("zümrüt", "EMERALD");
        put("demir", "IRON");
        put("altın", "GOLD");
        put("bakır", "COPPER");
        put("kılıç", "SWORD");
        put("kazma", "PICKAXE");
        put("kürek", "SHOVEL");
        put("balta", "AXE");
        put("çapa", "HOE");
        put("kemik", "BONE", "BONE_MEAL");
        put("ip", "STRING");
        put("barut", "GUNPOWDER");
        put("ender", "ENDER_PEARL", "ENDER_EYE", "ENDER_CHEST");
        put("taş", "STONE", "COBBLESTONE");
        put("kırık taş", "COBBLESTONE");
        put("toprak", "DIRT", "GRASS_BLOCK");
        put("çimen", "GRASS_BLOCK");
    }

    private static void put(String tr, String... tokens) {
        TR_ALIASES.put(norm(tr), Arrays.asList(tokens));
    }
    public static String norm(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase(Locale.ROOT);
        s = s.replace('ı', 'i')
                .replace('İ', 'i')
                .replace('ş', 's')
                .replace('Ş', 's')
                .replace('ğ', 'g')
                .replace('Ğ', 'g')
                .replace('ü', 'u')
                .replace('Ü', 'u')
                .replace('ö', 'o')
                .replace('Ö', 'o')
                .replace('ç', 'c')
                .replace('Ç', 'c');
        s = s.replaceAll("\\s+", " ");
        return s;
    }
    public static boolean matches(Material m, String queryRaw) {
        if (m == null || m.isAir()) return false;

        String q = norm(queryRaw);
        if (q.isEmpty()) return false;
        String name = m.name().toLowerCase(Locale.ROOT);
        if (name.contains(q)) return true;
        List<String> tokens = TR_ALIASES.get(q);
        if (tokens != null) {
            for (String t : tokens) {
                String tt = t.toUpperCase(Locale.ROOT);

                if (m.name().contains(tt)) return true;
            }
        }

        String[] parts = q.split(" ");
        if (parts.length >= 2) {
            int hit = 0;
            for (String part : parts) {
                if (part.length() < 2) continue;
                if (name.contains(part)) hit++;
            }
            return hit >= 2;
        }

        return false;
    }
}
