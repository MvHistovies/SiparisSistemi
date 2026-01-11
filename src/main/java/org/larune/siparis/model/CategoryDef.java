package org.larune.siparis.model;

import org.bukkit.Material;
import java.util.List;

public class CategoryDef {
    private final String key;
    private final String displayName;
    private final Material icon;
    private final List<Material> materials;

    public CategoryDef(String key, String displayName, Material icon, List<Material> materials) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.materials = materials;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public List<Material> getMaterials() { return materials; }
}
