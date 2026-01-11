package org.larune.siparis.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Recipe;

import java.util.*;

public final class CategoryAutoPopulator {

    private CategoryAutoPopulator() {}

    /** Minecraft'taki tüm item/blok materyalleri (AIR hariç, item olanlar). */
    public static Set<Material> collectAllItems() {
        Set<Material> all = EnumSet.noneOf(Material.class);
        for (Material m : Material.values()) {
            if (m == null || m.isAir()) continue;
            if (!m.isItem()) continue;
            all.add(m);
        }
        return all;
    }

    /** İstersen kullanırsın: craft edilebilir sonuçlar */
    public static Set<Material> collectCraftables() {
        Set<Material> craftables = EnumSet.noneOf(Material.class);
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r == null || r.getResult() == null) continue;
            Material m = r.getResult().getType();
            if (m.isAir()) continue;
            craftables.add(m);
        }
        return craftables;
    }

    public static Set<Material> buildMaden(Set<Material> base) {
        Set<Material> out = EnumSet.noneOf(Material.class);

        for (Material m : base) {
            String n = m.name();

            boolean oreLike = n.endsWith("_ORE");
            boolean rawLike = n.startsWith("RAW_");
            boolean ingotLike = n.endsWith("_INGOT") || n.endsWith("_NUGGET");
            boolean gemLike = n.equals("DIAMOND") || n.equals("EMERALD") || n.equals("LAPIS_LAZULI")
                    || n.equals("REDSTONE") || n.equals("COAL");
            boolean mineralBlock = n.endsWith("_BLOCK") && (n.contains("IRON") || n.contains("GOLD") || n.contains("DIAMOND") ||
                    n.contains("EMERALD") || n.contains("COPPER") || n.contains("NETHERITE") || n.contains("REDSTONE") ||
                    n.contains("LAPIS") || n.contains("COAL"));

            if (oreLike || rawLike || ingotLike || gemLike || mineralBlock) out.add(m);
        }

        return out;
    }

    public static Set<Material> buildCiftci(Set<Material> base) {
        Set<Material> out = EnumSet.noneOf(Material.class);

        Material[] basics = {
                Material.WHEAT, Material.WHEAT_SEEDS,
                Material.CARROT, Material.POTATO,
                Material.BEETROOT, Material.BEETROOT_SEEDS,
                Material.SUGAR_CANE,
                Material.MELON_SLICE, Material.MELON_SEEDS,
                Material.PUMPKIN, Material.PUMPKIN_SEEDS,
                Material.COCOA_BEANS,
                Material.NETHER_WART,
                Material.CACTUS,
                Material.BAMBOO,
                Material.KELP,
                Material.SWEET_BERRIES,
                Material.GLOW_BERRIES
        };
        out.addAll(Arrays.asList(basics));

        for (Material m : base) {
            String n = m.name();
            if (n.contains("BREAD") || n.contains("CAKE") || n.contains("COOKIE") ||
                    n.contains("PIE") || n.contains("STEW") || n.contains("SOUP") ||
                    n.contains("CARROT") || n.contains("POTATO") || n.contains("BEETROOT") ||
                    n.contains("MELON") || n.contains("PUMPKIN") || n.contains("SUGAR") ||
                    n.contains("HAY_BLOCK") || n.contains("DRIED_KELP") ||
                    n.contains("GOLDEN_APPLE") || n.contains("GOLDEN_CARROT")) {
                out.add(m);
            }
        }

        return out;
    }

    public static Set<Material> buildMob(Set<Material> base) {
        Set<Material> out = EnumSet.noneOf(Material.class);

        Material[] drops = {
                Material.ROTTEN_FLESH, Material.BONE, Material.STRING, Material.SPIDER_EYE,
                Material.GUNPOWDER, Material.ENDER_PEARL, Material.SLIME_BALL,
                Material.BLAZE_ROD, Material.MAGMA_CREAM, Material.GHAST_TEAR,
                Material.LEATHER, Material.FEATHER, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS
        };
        out.addAll(Arrays.asList(drops));

        for (Material m : base) {
            String n = m.name();
            if (n.contains("BONE_MEAL") || n.contains("LEAD") || n.contains("BOW") || n.contains("ARROW") ||
                    n.contains("FISHING_ROD") || n.contains("ENDER_CHEST") || n.contains("FIRE_CHARGE")) {
                out.add(m);
            }
        }

        return out;
    }

    public static Set<Material> buildDiger(Set<Material> base, Set<Material> maden, Set<Material> ciftci, Set<Material> mob) {
        Set<Material> out = EnumSet.copyOf(base);
        out.removeAll(maden);
        out.removeAll(ciftci);
        out.removeAll(mob);
        return out;
    }
}
