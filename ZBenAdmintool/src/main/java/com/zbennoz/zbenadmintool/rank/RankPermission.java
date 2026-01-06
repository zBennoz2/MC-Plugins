package com.zbennoz.zbenadmintool.rank;

import java.util.Arrays;
import java.util.Locale;

public enum RankPermission {
    ALL,
    BAN,
    KICK,
    MUTE,
    WARN,
    INSPECT,
    RANK_MANAGE,
    ADMIN_MENU,
    ADMIN_MODE,
    VANISH,
    LOGS,
    OFFLINE_INVENTORY,
    OFFLINE_ENDERCHEST,
    OBSERVE,
    TELEPORT;

    public static RankPermission fromString(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.toUpperCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }
}
