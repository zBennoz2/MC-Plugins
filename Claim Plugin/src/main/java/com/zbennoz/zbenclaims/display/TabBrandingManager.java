package com.zbennoz.zbenclaims.display;

import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TabBrandingManager implements Listener {

    private final ZBenClaimsPlugin plugin;

    public TabBrandingManager(ZBenClaimsPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player) {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)
                || !plugin.getConfig().getBoolean("tablist.branding.enabled", true)) {
            player.setPlayerListHeaderFooter("", "");
            return;
        }
        String headerRaw = color(plugin.getConfig().getString("tablist.branding.header", ""));
        String footerRaw = color(plugin.getConfig().getString("tablist.branding.footer", ""));
        player.setPlayerListHeaderFooter(headerRaw, footerRaw);
    }

    public void applyAll() {
        Bukkit.getOnlinePlayers().forEach(this::apply);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer());
    }

    private String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }
}
