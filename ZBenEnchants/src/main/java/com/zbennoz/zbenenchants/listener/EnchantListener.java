package com.zbennoz.zbenenchants.listener;

import com.zbennoz.zbenenchants.ZBenEnchantsPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class EnchantListener implements Listener {

    private final ZBenEnchantsPlugin plugin;
    private final NamespacedKey telekinesisKey;

    public EnchantListener(ZBenEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.telekinesisKey = new NamespacedKey(plugin, "telekinesis");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (hasTelekinesis(tool)) {
            event.setDropItems(false);
            event.getPlayer().getInventory().addItem(new ItemStack(event.getBlock().getType() == Material.IRON_ORE ? Material.RAW_IRON : event.getBlock().getType()));
            event.getPlayer().sendMessage(Component.text("Telekinesis picked up drops."));
        }
    }

    private boolean hasTelekinesis(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(telekinesisKey, PersistentDataType.BYTE);
    }
}
