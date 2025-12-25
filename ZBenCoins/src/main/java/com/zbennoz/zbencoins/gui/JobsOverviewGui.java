package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.job.JobRecord;
import com.zbennoz.zbencoins.job.JobStatus;
import com.zbennoz.zbencoins.service.JobService;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Zeigt eigene Jobs, angenommene Jobs und Verlaufsinfos.
 */
public class JobsOverviewGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final JobService jobService;
    private final Player player;
    private final Inventory inventory;

    public JobsOverviewGui(ZBenCoinsPlugin plugin, JobService jobService, Player player) {
        this.plugin = plugin;
        this.jobService = jobService;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, Text.colorize("&8Meine Jobs"));
        build();
    }

    private void build() {
        List<JobRecord> created = jobService.listCreated(player);
        List<JobRecord> accepted = jobService.listAccepted(player);
        Map<JobStatus, Integer> createdCounts = countByStatus(created);
        Map<JobStatus, Integer> acceptedCounts = countByStatus(accepted);

        ItemStack createdItem = new GuiItemBuilder(Material.CRAFTING_TABLE)
                .name("&aErstellte Jobs")
                .lore(buildLore(createdCounts))
                .build();

        ItemStack acceptedItem = new GuiItemBuilder(Material.ENDER_CHEST)
                .name("&bAngenommene Jobs")
                .lore(buildLore(acceptedCounts))
                .build();

        List<String> logLines = new ArrayList<>();
        for (String line : jobService.recentLogs(player)) {
            logLines.add("&7- &f" + line);
        }
        if (logLines.isEmpty()) {
            logLines.add("&7Keine Einträge.");
        }
        ItemStack logs = new GuiItemBuilder(Material.WRITTEN_BOOK)
                .name("&eVerlauf")
                .lore(logLines)
                .build();

        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();

        inventory.setItem(11, createdItem);
        inventory.setItem(13, acceptedItem);
        inventory.setItem(15, logs);
        inventory.setItem(40, back);
    }

    private List<String> buildLore(Map<JobStatus, Integer> map) {
        List<String> lore = new ArrayList<>();
        for (JobStatus status : JobStatus.values()) {
            lore.add("&7" + status + ": &e" + map.getOrDefault(status, 0));
        }
        return lore;
    }

    private Map<JobStatus, Integer> countByStatus(List<JobRecord> records) {
        Map<JobStatus, Integer> map = new EnumMap<>(JobStatus.class);
        for (JobRecord record : records) {
            map.put(record.getStatus(), map.getOrDefault(record.getStatus(), 0) + 1);
        }
        return map;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (event.getRawSlot() == 40) {
            plugin.getGuiManager().openGui(p, new JobsMainGui(plugin, jobService));
        }
    }
}
