package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.job.JobRecord;
import com.zbennoz.zbencoins.job.JobType;
import com.zbennoz.zbencoins.service.JobService;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Übersicht über offene Jobs.
 */
public class JobBrowseGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final JobService jobService;
    private final Inventory inventory;
    private final Map<Integer, Integer> slotJobMap = new HashMap<>();
    private final int page;

    public JobBrowseGui(ZBenCoinsPlugin plugin, JobService jobService, int page) {
        this.plugin = plugin;
        this.jobService = jobService;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, 54, Text.colorize("&8&lJobs"));
        build();
    }

    private void build() {
        List<JobRecord> jobs = jobService.listOpen(page, 45);
        int slot = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(java.time.ZoneId.systemDefault());
        for (JobRecord job : jobs) {
            Material icon = job.getType() == JobType.LIEFERUNG ? Material.CHEST : Material.CLOCK;
            ItemStack item = new GuiItemBuilder(icon)
                    .name("&a#" + job.getId() + " &7- " + job.getTitle())
                    .lore(List.of(
                            "&7Typ: &e" + job.getType(),
                            "&7Belohnung: &6" + job.getReward() + " " + plugin.getConfig().getString("currency-name", "Coins"),
                            "&7Erstellt von: &e" + job.getCreatorName(),
                            job.getExpiresAt() == null ? "&7Ablauf: &eoffen" : "&7Ablauf: &c" + formatter.format(job.getExpiresAt())
                    ))
                    .build();
            inventory.setItem(slot, item);
            slotJobMap.put(slot, job.getId());
            slot++;
        }

        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();
        ItemStack next = new GuiItemBuilder(Material.ARROW).name("&aWeiter").build();
        ItemStack previous = new GuiItemBuilder(Material.ARROW).name("&aZurück").build();
        inventory.setItem(45, previous);
        inventory.setItem(49, back);
        inventory.setItem(53, next);
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
        int raw = event.getRawSlot();
        if (slotJobMap.containsKey(raw)) {
            int jobId = slotJobMap.get(raw);
            jobService.findById(jobId).ifPresent(job ->
                    plugin.getGuiManager().openGui(player, new JobDetailGui(plugin, jobService, job, player)));
            return;
        }
        if (raw == 45 && page > 0) {
            plugin.getGuiManager().openGui(player, new JobBrowseGui(plugin, jobService, page - 1));
        } else if (raw == 53) {
            plugin.getGuiManager().openGui(player, new JobBrowseGui(plugin, jobService, page + 1));
        } else if (raw == 49) {
            plugin.getGuiManager().openGui(player, new JobsMainGui(plugin, jobService));
        }
    }
}
