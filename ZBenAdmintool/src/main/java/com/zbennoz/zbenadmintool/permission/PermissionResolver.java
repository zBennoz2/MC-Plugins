package com.zbennoz.zbenadmintool.permission;

import com.zbennoz.zbenadmintool.rank.RankManager;
import org.bukkit.entity.Player;

public class PermissionResolver {

    private final RankManager rankManager;

    public PermissionResolver(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    public boolean has(Player player, String permission) {
        return player.isOp() || player.hasPermission(permission) || rankManager.hasRankPermission(player, permission);
    }
}
