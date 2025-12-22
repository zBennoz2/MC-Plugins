package com.zbennoz.zbenadmintool.player;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TabBrandingListener implements Listener {

    private final ZBenAdmintool plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TabBrandingListener(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("tab.branding.enabled", true)) {
            applyBranding(plugin, event.getPlayer());
        }
        plugin.getRankManager().refreshPlayerTeam(event.getPlayer());
    }

    public static void applyBranding(ZBenAdmintool plugin, Player player) {
        if (!plugin.getConfig().getBoolean("tab.branding.enabled", true)) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            return;
        }
        Component header = MiniMessage.miniMessage().deserialize("<gold>Created by ZBenNoZ Gaming\n<green>https://zbennoz.com");
        Component footer = Component.empty();
        player.sendPlayerListHeaderAndFooter(header, footer);
    }
}
