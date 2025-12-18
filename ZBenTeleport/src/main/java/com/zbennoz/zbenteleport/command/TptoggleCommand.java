package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.util.PlayerSettingsManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TptoggleCommand implements CommandExecutor {

    private final ZBenTeleportPlugin plugin;
    private final PlayerSettingsManager settingsManager;

    public TptoggleCommand(ZBenTeleportPlugin plugin, PlayerSettingsManager settingsManager) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this."));
            return true;
        }
        if (!player.hasPermission("zbenteleport.tptoggle")) {
            player.sendMessage(plugin.messages().component("no-permission"));
            return true;
        }
        boolean enabled = settingsManager.toggle(player.getUniqueId());
        if (enabled) {
            player.sendMessage(plugin.messages().component("tptoggle.enabled"));
        } else {
            player.sendMessage(plugin.messages().component("tptoggle.disabled"));
        }
        return true;
    }
}
