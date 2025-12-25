package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.service.JobService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Einstiegsmenü für das Jobs-System.
 */
public class JobsMainGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final JobService jobService;
    private final Inventory inventory;

    public JobsMainGui(ZBenCoinsPlugin plugin, JobService jobService) {
        this.plugin = plugin;
        this.jobService = jobService;
        this.inventory = Bukkit.createInventory(this, 27, plugin.getConfigManager().message("jobs-title"));
        build();
    }

    private void build() {
        ItemStack browse = new GuiItemBuilder(Material.MAP)
                .name("&aOffene Jobs")
                .lore(List.of("&7Durchsuche aktuelle Angebote"))
                .build();

        ItemStack create = new GuiItemBuilder(Material.EMERALD)
                .name("&aJob anbieten")
                .lore(List.of("&7Erstelle einen Lieferjob", "&7oder eine Dienstleistung"))
                .build();

        ItemStack info = new GuiItemBuilder(Material.BOOK)
                .name("&eMeine Jobs")
                .lore(List.of("&7Übersicht & Verlauf"))
                .build();

        ItemStack back = new GuiItemBuilder(Material.BARRIER)
                .name("&cZurück")
                .build();

        inventory.setItem(11, browse);
        inventory.setItem(13, create);
        inventory.setItem(15, info);
        inventory.setItem(22, back);
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
        switch (event.getRawSlot()) {
            case 11 -> {
                com.zbennoz.zbencoins.job.JobQueryOptions opts = jobService.getBrowseOptions(player.getUniqueId()).copy();
                opts.setPage(0);
                plugin.getGuiManager().openGui(player, new JobBrowseGui(plugin, jobService, opts, player));
            }
            case 13 -> {
                if (!player.hasPermission("zbencoins.jobs.create")) {
                    player.sendMessage(plugin.getConfigManager().message("no-permission"));
                    return;
                }
                jobService.startDraft(player, com.zbennoz.zbencoins.job.JobType.LIEFERUNG);
                plugin.getGuiManager().openGui(player, new JobCreateGui(plugin, jobService, player));
            }
            case 15 -> plugin.getGuiManager().openGui(player, new JobsOverviewGui(plugin, jobService, player));
            case 22 -> plugin.getGuiManager().openGui(player,
                    new MarktMainGui(plugin, plugin.getCoinService(), plugin.getMarketService(), player));
            default -> {
            }
        }
    }
}
