package com.zbennoz.zbenlootr.container;

import org.bukkit.Material;

public enum ContainerType {
    CHEST,
    BARREL;

    public boolean matches(Material material) {
        return switch (this) {
            case CHEST -> material == Material.CHEST || material == Material.TRAPPED_CHEST;
            case BARREL -> material == Material.BARREL;
        };
    }
}
