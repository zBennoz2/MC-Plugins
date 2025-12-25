package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.job.JobRecord;
import com.zbennoz.zbencoins.job.JobQueryOptions;
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
    private final JobQueryOptions options;
    private final Player viewer;
    private int maxPage = 0;

    public JobBrowseGui(ZBenCoinsPlugin plugin, JobService jobService, JobQueryOptions options, Player viewer) {
        this.plugin = plugin;
        this.jobService = jobService;
        this.options = options;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 54, Text.colorize("&8&lJobs"));
        build();
    }

    private void build() {
        List<JobRecord> jobs = jobService.listFiltered(options);
        int pageSize = 45;
        maxPage = Math.max(0, (int) Math.ceil(jobs.size() / (double) pageSize) - 1);
        if (options.getPage() > maxPage) {
            options.setPage(maxPage);
        }
        int from = options.getPage() * pageSize;
        int to = Math.min(from + pageSize, jobs.size());
        List<JobRecord> pageJobs = jobs.subList(Math.min(from, jobs.size()), to);
        int slot = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(java.time.ZoneId.systemDefault());
        for (JobRecord job : pageJobs) {
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

        ItemStack coins = new GuiItemBuilder(Material.GOLD_NUGGET)
                .name("&eDeine Coins")
                .lore(List.of(
                        "&7Kontostand: &e" + plugin.getCoinService().getBalance(viewer.getUniqueId()) + " "
                                + plugin.getConfig().getString("currency-name", "Coins"),
                        "&7Nutze &e/pay &7für Transfers"
                ))
                .build();
        inventory.setItem(8, coins);

        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();
        ItemStack next = new GuiItemBuilder(Material.ARROW).name("&aWeiter").lore(List.of("&7Seite " + (options.getPage() + 1) + "/" + (maxPage + 1))).build();
        ItemStack previous = new GuiItemBuilder(Material.ARROW).name("&aZurück").build();
        ItemStack search = new GuiItemBuilder(options.getSearchTerm().isBlank() ? Material.MAP : Material.WRITABLE_BOOK)
                .name("&bSuche")
                .lore(List.of(options.getSearchTerm().isBlank() ? "&7Titel oder Beschreibung suchen" : "&7Aktiv: &f" + options.getSearchTerm()))
                .build();
        ItemStack reset = new GuiItemBuilder(Material.SUNFLOWER)
                .name("&eFilter zurücksetzen")
                .build();
        ItemStack sort = new GuiItemBuilder(Material.HOPPER)
                .name("&bSortierung")
                .lore(List.of("&7Aktuell: &f" + options.getSortOption().name().replace('_', ' ')))
                .glow(true)
                .build();
        ItemStack typeFilter = new GuiItemBuilder(Material.ITEM_FRAME)
                .name("&aJob-Typ")
                .lore(List.of("&7Aktuell: &f" + (options.getTypeFilter() == null ? "Alle" : options.getTypeFilter().name())))
                .build();
        ItemStack statusFilter = new GuiItemBuilder(Material.COMPASS)
                .name("&aStatus")
                .lore(List.of("&7Aktuell: &f" + (options.getStatusFilter() == null ? "Alle" : options.getStatusFilter().name())))
                .build();
        ItemStack pageInfo = new GuiItemBuilder(Material.PAPER)
                .name("&7Seite " + (options.getPage() + 1) + " / " + (maxPage + 1))
                .build();
        inventory.setItem(45, previous);
        inventory.setItem(46, search);
        inventory.setItem(47, reset);
        inventory.setItem(48, sort);
        inventory.setItem(49, back);
        inventory.setItem(50, typeFilter);
        inventory.setItem(51, statusFilter);
        inventory.setItem(52, pageInfo);
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
        if (raw == 45 && options.getPage() > 0) {
            JobQueryOptions newOptions = options.copy();
            newOptions.setPage(options.getPage() - 1);
            reopen(player, newOptions);
        } else if (raw == 53 && options.getPage() < maxPage) {
            JobQueryOptions newOptions = options.copy();
            newOptions.setPage(options.getPage() + 1);
            reopen(player, newOptions);
        } else if (raw == 46) {
            if (player.hasPermission("zbencoins.jobs.search")) {
                jobService.requestBrowseSearch(player);
            } else {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
            }
        } else if (raw == 47) {
            if (!player.hasPermission("zbencoins.jobs.filter")) {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
                return;
            }
            JobQueryOptions newOptions = options.copy();
            newOptions.reset();
            reopen(player, newOptions);
        } else if (raw == 48) {
            if (!player.hasPermission("zbencoins.jobs.filter")) {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
                return;
            }
            JobQueryOptions newOptions = options.copy();
            newOptions.setSortOption(nextSort(options.getSortOption()));
            newOptions.setPage(0);
            reopen(player, newOptions);
        } else if (raw == 50) {
            if (!player.hasPermission("zbencoins.jobs.filter")) {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
                return;
            }
            JobQueryOptions newOptions = options.copy();
            newOptions.setTypeFilter(nextType(options.getTypeFilter()));
            newOptions.setPage(0);
            reopen(player, newOptions);
        } else if (raw == 51) {
            if (!player.hasPermission("zbencoins.jobs.filter")) {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
                return;
            }
            JobQueryOptions newOptions = options.copy();
            newOptions.setStatusFilter(nextStatus(options.getStatusFilter()));
            newOptions.setPage(0);
            reopen(player, newOptions);
        } else if (raw == 49) {
            plugin.getGuiManager().openGui(player, new JobsMainGui(plugin, jobService));
        }
    }

    private JobQueryOptions.SortOption nextSort(JobQueryOptions.SortOption current) {
        return switch (current) {
            case NEUESTE -> JobQueryOptions.SortOption.BELOHNUNG_ABSTEIGEND;
            case BELOHNUNG_ABSTEIGEND -> JobQueryOptions.SortOption.BELOHNUNG_AUFSTEIGEND;
            case BELOHNUNG_AUFSTEIGEND -> JobQueryOptions.SortOption.ABLAUFEND;
            case ABLAUFEND -> JobQueryOptions.SortOption.NEUESTE;
        };
    }

    private JobType nextType(JobType current) {
        if (current == null) {
            return JobType.LIEFERUNG;
        }
        if (current == JobType.LIEFERUNG) {
            return JobType.DIENSTLEISTUNG;
        }
        return null;
    }

    private JobStatus nextStatus(JobStatus current) {
        if (current == null) {
            return JobStatus.OFFEN;
        }
        if (current == JobStatus.OFFEN) {
            return JobStatus.ANGENOMMEN;
        }
        if (current == JobStatus.ANGENOMMEN) {
            return JobStatus.ABGESCHLOSSEN;
        }
        return null;
    }

    private void reopen(Player player, JobQueryOptions newOptions) {
        JobQueryOptions stored = jobService.getBrowseOptions(player.getUniqueId());
        stored.setSearchTerm(newOptions.getSearchTerm());
        stored.setSortOption(newOptions.getSortOption());
        stored.setTypeFilter(newOptions.getTypeFilter());
        stored.setStatusFilter(newOptions.getStatusFilter());
        stored.setPage(newOptions.getPage());
        plugin.getGuiManager().openGui(player, new JobBrowseGui(plugin, jobService, stored.copy(), player));
    }
}
