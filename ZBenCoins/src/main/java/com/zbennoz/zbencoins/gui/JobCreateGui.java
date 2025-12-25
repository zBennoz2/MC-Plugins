package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.job.JobType;
import com.zbennoz.zbencoins.service.JobService;
import com.zbennoz.zbencoins.service.JobService.JobDraft;
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
 * GUI zur Job-Erstellung.
 */
public class JobCreateGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final JobService jobService;
    private final Player player;
    private final Inventory inventory;
    private JobDraft draft;

    public JobCreateGui(ZBenCoinsPlugin plugin, JobService jobService, Player player) {
        this.plugin = plugin;
        this.jobService = jobService;
        this.player = player;
        this.draft = jobService.getDraft(player).orElseGet(() -> jobService.startDraft(player, JobType.LIEFERUNG));
        this.inventory = Bukkit.createInventory(this, 45, plugin.getConfigManager().message("jobs-create-title"));
        build();
    }

    private void build() {
        inventory.clear();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(java.time.ZoneId.systemDefault());
        ItemStack type = new GuiItemBuilder(Material.COMPASS)
                .name("&eJob-Typ: &a" + draft.getType())
                .lore(List.of("&7Klicke zum Wechseln"))
                .build();

        List<String> titleLore = new ArrayList<>();
        titleLore.add("&7Aktuell: &f" + (draft.getTitle().isBlank() ? "-" : draft.getTitle()));
        titleLore.add("&eLinksklick: Titel eingeben");
        ItemStack title = new GuiItemBuilder(Material.NAME_TAG).name("&bTitel").lore(titleLore).build();

        List<String> descLore = new ArrayList<>();
        descLore.add("&7Aktuell: ");
        for (String line : Text.wrap(draft.getDescription().isBlank() ? "-" : draft.getDescription(), 25)) {
            descLore.add(" &f" + line);
        }
        descLore.add("&eLinksklick: Beschreibung eingeben");
        ItemStack description = new GuiItemBuilder(Material.WRITABLE_BOOK).name("&bBeschreibung").lore(descLore).build();

        ItemStack reward = new GuiItemBuilder(Material.GOLD_INGOT)
                .name("&6Belohnung")
                .lore(List.of("&7Aktuell: &e" + draft.getReward() + " " + plugin.getConfig().getString("currency-name", "Coins"),
                        "&eLinksklick: Betrag eingeben"))
                .build();

        List<String> expiryLore = new ArrayList<>();
        expiryLore.add(draft.getExpiresAt() == null ? "&7Ablauf: &eKeiner" : "&7Ablauf: &c" + formatter.format(draft.getExpiresAt()));
        expiryLore.add("&eLinksklick: Stunden eingeben (0 für ohne)");
        ItemStack expiry = new GuiItemBuilder(Material.CLOCK).name("&bAblaufzeit").lore(expiryLore).build();

        ItemStack target = new GuiItemBuilder(Material.CHEST)
                .name("&aLieferziel")
                .lore(List.of(
                        "&7Item: &e" + (draft.getItemType() == null ? "-" : draft.getItemType()),
                        "&7Menge: &e" + draft.getItemAmount(),
                        "&eRechtsklick: Item aus Hand übernehmen",
                        "&eLinksklick: Menge eingeben"
                ))
                .build();

        ItemStack publish = new GuiItemBuilder(Material.EMERALD_BLOCK)
                .name("&aJob einstellen")
                .lore(List.of("&7Belohnung wird reserviert"))
                .build();

        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();

        inventory.setItem(10, type);
        inventory.setItem(12, title);
        inventory.setItem(14, description);
        inventory.setItem(16, reward);
        inventory.setItem(20, expiry);
        if (draft.getType() == JobType.LIEFERUNG) {
            inventory.setItem(22, target);
        }
        inventory.setItem(24, publish);
        inventory.setItem(40, back);
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
        int slot = event.getRawSlot();
        switch (slot) {
            case 10 -> toggleType();
            case 12 -> jobService.requestInput(p, JobService.DraftInput.TITLE);
            case 14 -> jobService.requestInput(p, JobService.DraftInput.DESCRIPTION);
            case 16 -> jobService.requestInput(p, JobService.DraftInput.REWARD);
            case 20 -> jobService.requestInput(p, JobService.DraftInput.EXPIRY);
            case 22 -> {
                if (draft.getType() == JobType.LIEFERUNG) {
                    if (event.isRightClick()) {
                        jobService.applyItem(p);
                    } else {
                        jobService.requestInput(p, JobService.DraftInput.AMOUNT);
                    }
                }
            }
            case 24 -> {
                jobService.publish(p).ifPresentOrElse(p::sendMessage, () -> {
                    p.sendMessage(plugin.getConfigManager().message("job-created"));
                    plugin.getGuiManager().openGui(p, new JobsMainGui(plugin, jobService));
                });
            }
            case 40 -> plugin.getGuiManager().openGui(p, new JobsMainGui(plugin, jobService));
            default -> {
            }
        }
        this.draft = jobService.getDraft(p).orElse(draft);
        build();
    }

    private void toggleType() {
        draft.setType(draft.getType() == JobType.LIEFERUNG ? JobType.DIENSTLEISTUNG : JobType.LIEFERUNG);
        if (draft.getType() == JobType.DIENSTLEISTUNG) {
            draft.setItemType(null);
            draft.setItemAmount(0);
        }
    }
}
