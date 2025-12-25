package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.job.JobRecord;
import com.zbennoz.zbencoins.job.JobStatus;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Detailansicht eines einzelnen Jobs.
 */
public class JobDetailGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final JobService jobService;
    private JobRecord record;
    private final Player viewer;
    private final Inventory inventory;

    public JobDetailGui(ZBenCoinsPlugin plugin, JobService jobService, JobRecord record, Player viewer) {
        this.plugin = plugin;
        this.jobService = jobService;
        this.record = record;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 27, Text.colorize("&8Job #" + record.getId()));
        build();
    }

    private void build() {
        inventory.clear();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(java.time.ZoneId.systemDefault());
        List<String> lore = new ArrayList<>();
        lore.add("&7Typ: &e" + record.getType());
        lore.add("&7Status: &e" + record.getStatus());
        lore.add("&7Ersteller: &e" + record.getCreatorName());
        lore.add("&7Ausführender: &e" + (record.getAssigneeName() == null ? "-" : record.getAssigneeName()));
        lore.add("&7Belohnung: &6" + record.getReward() + " " + plugin.getConfig().getString("currency-name", "Coins"));
        if (record.getType() == JobType.LIEFERUNG) {
            lore.add("&7Ziel: &e" + record.getItemAmount() + "x " + record.getItemType());
        }
        lore.add("&7Beschreibung:");
        for (String line : Text.wrap(record.getDescription(), 30)) {
            lore.add(" &f" + line);
        }
        lore.add(record.getExpiresAt() == null ? "&7Ablauf: &eoffen" : "&7Ablauf: &c" + formatter.format(record.getExpiresAt()));
        if (record.isCompletionRequested()) {
            lore.add("&eAbschluss angefragt");
        }

        ItemStack info = new GuiItemBuilder(Material.BOOK).name("&aJobdetails").lore(lore).build();
        inventory.setItem(11, info);

        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();
        inventory.setItem(22, back);

        buildActions();
    }

    private void buildActions() {
        if (record.getStatus() == JobStatus.OFFEN) {
            ItemStack accept = new GuiItemBuilder(Material.LIME_WOOL).name("&aAnnehmen").build();
            inventory.setItem(15, accept);
        }
        if ((record.getStatus() == JobStatus.OFFEN || record.getStatus() == JobStatus.ANGENOMMEN)
                && viewer.getUniqueId().equals(record.getCreatorUuid())) {
            ItemStack cancel = new GuiItemBuilder(Material.RED_WOOL).name("&cAbbrechen").build();
            inventory.setItem(16, cancel);
        }
        if (record.getStatus() == JobStatus.ANGENOMMEN && record.getAssigneeUuid() != null) {
            if (record.getType() == JobType.LIEFERUNG && viewer.getUniqueId().equals(record.getAssigneeUuid())) {
                ItemStack deliver = new GuiItemBuilder(Material.HOPPER).name("&aAbgeben").build();
                inventory.setItem(13, deliver);
            }
            if (record.getType() == JobType.DIENSTLEISTUNG) {
                if (viewer.getUniqueId().equals(record.getAssigneeUuid())) {
                    ItemStack request = new GuiItemBuilder(Material.BELL)
                            .name("&eAbschluss anfragen")
                            .build();
                    inventory.setItem(13, request);
                } else if (viewer.getUniqueId().equals(record.getCreatorUuid()) && record.isCompletionRequested()) {
                    ItemStack finish = new GuiItemBuilder(Material.EMERALD_BLOCK)
                            .name("&aAls erledigt markieren")
                            .build();
                    inventory.setItem(13, finish);
                }
            }
        }
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
        if (raw == 22) {
            com.zbennoz.zbencoins.job.JobQueryOptions opts = jobService.getBrowseOptions(player.getUniqueId()).copy();
            plugin.getGuiManager().openGui(player, new JobBrowseGui(plugin, jobService, opts, player));
            return;
        }
        if (raw == 15 && record.getStatus() == JobStatus.OFFEN) {
            jobService.accept(player, record).ifPresentOrElse(
                    player::sendMessage,
                    () -> player.sendMessage(plugin.getConfigManager().message("job-accepted-self")));
        }
        if (raw == 16 && (record.getStatus() == JobStatus.OFFEN || record.getStatus() == JobStatus.ANGENOMMEN)) {
            jobService.cancel(player, record).ifPresentOrElse(
                    player::sendMessage,
                    () -> player.sendMessage(plugin.getConfigManager().message("job-cancelled")));
        }
        if (raw == 13 && record.getStatus() == JobStatus.ANGENOMMEN) {
            if (record.getType() == JobType.LIEFERUNG) {
                jobService.completeDelivery(player, record).ifPresentOrElse(
                        player::sendMessage,
                        () -> player.sendMessage(plugin.getConfigManager().message("job-finished")));
            } else if (record.getType() == JobType.DIENSTLEISTUNG) {
                if (player.getUniqueId().equals(record.getAssigneeUuid())) {
                    jobService.requestCompletion(player, record).ifPresentOrElse(
                            player::sendMessage,
                            () -> player.sendMessage(plugin.getConfigManager().message("job-completion-asked")));
                } else if (player.getUniqueId().equals(record.getCreatorUuid()) && record.isCompletionRequested()) {
                    jobService.completeService(player, record).ifPresentOrElse(
                            player::sendMessage,
                            () -> player.sendMessage(plugin.getConfigManager().message("job-finished")));
                }
            }
        }
        refresh(player);
    }

    private void refresh(Player player) {
        jobService.findById(record.getId()).ifPresent(updated -> {
            this.record = updated;
            build();
            player.openInventory(inventory);
        });
    }
}
