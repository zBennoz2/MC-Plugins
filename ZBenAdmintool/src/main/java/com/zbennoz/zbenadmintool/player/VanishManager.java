package com.zbennoz.zbenadmintool.player;

import com.zbennoz.zbenadmintool.permission.PermissionResolver;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final Set<UUID> vanished = new HashSet<>();
    private final PermissionResolver permissionResolver;
    private final Plugin plugin;

    public VanishManager(Plugin plugin, PermissionResolver permissionResolver) {
        this.plugin = plugin;
        this.permissionResolver = permissionResolver;
    }

    public boolean toggle(Player player) {
        boolean enable = !vanished.contains(player.getUniqueId());
        setVanish(player, enable);
        return enable;
    }

    public void setVanish(Player player, boolean enable) {
        if (enable) {
            vanished.add(player.getUniqueId());
        } else {
            vanished.remove(player.getUniqueId());
        }
        refreshVisibility(player);
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void refreshVisibility(Player target) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            if (isVanished(target) && !permissionResolver.has(other, "zbenadmintool.vanish.see")) {
                other.hidePlayer(plugin, target);
            } else {
                other.showPlayer(plugin, target);
            }
        }
    }
}
