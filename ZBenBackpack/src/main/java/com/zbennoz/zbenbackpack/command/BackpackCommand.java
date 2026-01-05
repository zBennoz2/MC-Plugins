package com.zbennoz.zbenbackpack.command;

import com.zbennoz.zbenbackpack.api.BackpackService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BackpackCommand implements CommandExecutor, Listener {

    private final BackpackService service;

    public BackpackCommand(BackpackService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        service.openBackpack(player);
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        if (!service.isBackpack(item)) return;
        event.setCancelled(true);
        service.openBackpack(event.getPlayer());
    }
}
