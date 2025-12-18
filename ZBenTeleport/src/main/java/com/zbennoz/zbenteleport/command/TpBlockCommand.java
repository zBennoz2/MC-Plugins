package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.util.PlayerSettingsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TpBlockCommand implements CommandExecutor {

    public enum Mode {
        BLOCK,
        UNBLOCK,
        LIST
    }

    private final ZBenTeleportPlugin plugin;
    private final PlayerSettingsManager settingsManager;
    private final Mode mode;

    public TpBlockCommand(ZBenTeleportPlugin plugin, PlayerSettingsManager settingsManager, Mode mode) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        this.mode = mode;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this."));
            return true;
        }

        switch (mode) {
            case BLOCK -> {
                if (!player.hasPermission("zbenteleport.tpblock")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                if (args.length == 0) {
                    player.sendMessage(plugin.messages().component("usage.tpblock"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                UUID targetId = target.getUniqueId();
                if (targetId.equals(player.getUniqueId())) {
                    player.sendMessage(plugin.messages().component("tpblock.self"));
                    return true;
                }
                if (settingsManager.addBlock(player.getUniqueId(), targetId)) {
                    player.sendMessage(plugin.messages().component("tpblock.added", Placeholder.unparsed("player", target.getName() == null ? targetId.toString() : target.getName())));
                } else {
                    player.sendMessage(plugin.messages().component("tpblock.already"));
                }
            }
            case UNBLOCK -> {
                if (!player.hasPermission("zbenteleport.tpunblock")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                if (args.length == 0) {
                    player.sendMessage(plugin.messages().component("usage.tpunblock"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                UUID targetId = target.getUniqueId();
                if (settingsManager.removeBlock(player.getUniqueId(), targetId)) {
                    player.sendMessage(plugin.messages().component("tpblock.removed", Placeholder.unparsed("player", target.getName() == null ? targetId.toString() : target.getName())));
                } else {
                    player.sendMessage(plugin.messages().component("tpblock.notfound"));
                }
            }
            case LIST -> {
                if (!player.hasPermission("zbenteleport.tpblocklist")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                var blocked = settingsManager.getBlocks(player.getUniqueId());
                if (blocked.isEmpty()) {
                    player.sendMessage(plugin.messages().component("tpblock.empty"));
                    return true;
                }
                StringBuilder names = new StringBuilder();
                for (UUID id : blocked) {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(id);
                    String name = off.getName() != null ? off.getName() : id.toString();
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(name);
                }
                player.sendMessage(plugin.messages().component("tpblock.list", Placeholder.unparsed("players", names.toString())));
            }
        }
        return true;
    }
}
