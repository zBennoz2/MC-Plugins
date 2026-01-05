package com.zbennoz.zbenadmintool.rank;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RankPermissionBridge {

    private final ZBenAdmintool plugin;
    private final RankManager rankManager;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public RankPermissionBridge(ZBenAdmintool plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
    }

    public void applyPermissions(Player player) {
        clear(player);

        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), attachment);

        Rank rank = rankManager.getPlayerRank(player);
        if (rank == null) {
            return;
        }

        rank.getBukkitPermissions().forEach(permission -> grantPermission(attachment, permission));
        player.recalculatePermissions();
    }

    private void grantPermission(PermissionAttachment attachment, String permission) {
        attachment.setPermission(permission, true);

        if (permission.endsWith(".*")) {
            String prefix = permission.substring(0, permission.length() - 2).toLowerCase(Locale.ROOT);
            Bukkit.getPluginManager().getPermissions().stream()
                    .map(Permission::getName)
                    .filter(registered -> registered.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .forEach(registered -> attachment.setPermission(registered, true));
        }
    }

    public void clear(Player player) {
        PermissionAttachment existing = attachments.remove(player.getUniqueId());
        if (existing != null) {
            player.removeAttachment(existing);
            player.recalculatePermissions();
        }
    }
}
