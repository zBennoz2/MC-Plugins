package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.data.TeleportDatabase;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand implements CommandExecutor {

    private final ZBenTeleportPlugin plugin;
    private final TeleportDatabase database;

    public BackCommand(ZBenTeleportPlugin plugin, TeleportDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        String prefer = plugin.getConfig().getBoolean("back.prefer-death", true) ? "death" : "teleport";
        Location location = database.loadLastLocation(player.getUniqueId(), prefer);
        if (location == null) {
            location = database.loadAnyLastLocation(player.getUniqueId());
        }
        if (location == null) {
            player.sendMessage(Component.text("No location stored."));
            return true;
        }
        player.teleportAsync(location);
        player.sendMessage(Component.text("Teleported back."));
        return true;
    }
}
