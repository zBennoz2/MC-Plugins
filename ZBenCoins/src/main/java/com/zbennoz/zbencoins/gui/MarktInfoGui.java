package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.service.CoinService;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Info-Untermenü.
 */
public class MarktInfoGui implements ManagedGui {

    private final Inventory inventory;
    private final ZBenCoinsPlugin plugin;
    private final CoinService coinService;

    public MarktInfoGui(ZBenCoinsPlugin plugin, CoinService coinService, Player player) {
        this.plugin = plugin;
        this.coinService = coinService;
        String title = plugin.getConfigManager().message("info-title");
        this.inventory = Bukkit.createInventory(this, 27, title);
        build(player);
    }

    private void build(Player player) {
        long coins = coinService.getBalance(player.getUniqueId());
        long transactions = coinService.countTransactionsLastDays(player.getUniqueId(), 7);

        List<String> infoLines = new ArrayList<>();
        for (String line : plugin.getConfigManager().getMessages().getStringList("info-lines")) {
            infoLines.add(Text.format(line, Map.of(
                    "coins", String.valueOf(coins),
                    "transactions", String.valueOf(transactions)
            )));
        }

        ItemStack info = new GuiItemBuilder(Material.BOOK)
                .name("&eDeine Infos")
                .lore(infoLines)
                .build();

        ItemStack close = new GuiItemBuilder(Material.BARRIER)
                .name("&cZurück")
                .build();

        inventory.setItem(13, info);
        inventory.setItem(22, close);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() == 22) {
            player.closeInventory();
            plugin.getGuiManager().openGui(player, new MarktMainGui(plugin, coinService, plugin.getMarketService(), player));
        }
    }
}
