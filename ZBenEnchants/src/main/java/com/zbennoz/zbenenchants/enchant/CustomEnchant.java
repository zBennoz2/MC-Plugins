package com.zbennoz.zbenenchants.enchant;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

/**
 * Registry aller Custom Enchants. Enthält Basisinformationen und Material-Filter.
 */
public enum CustomEnchant {
    TELEKINESIS("telekinesis", "Telekinese", 3, TargetType.TOOL),
    SMELT("smelt", "Schmelzen", 2, TargetType.PICKAXE),
    EXCAVATOR("excavator", "Bagger", 1, TargetType.PICKAXE),
    REPLANT("replant", "Neuanpflanzen", 3, TargetType.HOE),
    LUMBERJACK("lumberjack", "Holzfäller", 3, TargetType.AXE),
    LUCKY_FIND("luckyfind", "Glücksfund", 3, TargetType.TOOL),
    GUARDIAN("guardian", "Wächter", 3, TargetType.CHESTPLATE),
    GRAPPLE("grapple", "Enterhaken", 3, TargetType.BOOTS),
    STABILITY("stability", "Standfestigkeit", 3, TargetType.BOOTS),
    SECOND_WIND("secondwind", "Zweiter Atem", 1, TargetType.CHESTPLATE),
    SURVEYOR("surveyor", "Vermesser", 1, TargetType.COMPASS);

    private final String key;
    private final String displayName;
    private final int maxLevel;
    private final TargetType targetType;

    CustomEnchant(String key, String displayName, int maxLevel, TargetType targetType) {
        this.key = key;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.targetType = targetType;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isApplicable(Material material) {
        return targetType.isApplicable(material);
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public enum TargetType {
        TOOL(EnumSet.of(Material.WOODEN_AXE, Material.WOODEN_HOE, Material.WOODEN_PICKAXE, Material.WOODEN_SHOVEL,
                Material.STONE_AXE, Material.STONE_HOE, Material.STONE_PICKAXE, Material.STONE_SHOVEL,
                Material.IRON_AXE, Material.IRON_HOE, Material.IRON_PICKAXE, Material.IRON_SHOVEL,
                Material.GOLDEN_AXE, Material.GOLDEN_HOE, Material.GOLDEN_PICKAXE, Material.GOLDEN_SHOVEL,
                Material.DIAMOND_AXE, Material.DIAMOND_HOE, Material.DIAMOND_PICKAXE, Material.DIAMOND_SHOVEL,
                Material.NETHERITE_AXE, Material.NETHERITE_HOE, Material.NETHERITE_PICKAXE, Material.NETHERITE_SHOVEL)),
        PICKAXE(EnumSet.of(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE)),
        AXE(EnumSet.of(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE,
                Material.DIAMOND_AXE, Material.NETHERITE_AXE)),
        HOE(EnumSet.of(Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE,
                Material.DIAMOND_HOE, Material.NETHERITE_HOE)),
        BOOTS(EnumSet.of(Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS,
                Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS)),
        CHESTPLATE(EnumSet.of(Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
                Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE)),
        COMPASS(EnumSet.of(Material.COMPASS));

        private final Set<Material> materials;

        TargetType(Set<Material> materials) {
            this.materials = materials;
        }

        public boolean isApplicable(Material material) {
            return material != null && materials.contains(material);
        }
    }
}
