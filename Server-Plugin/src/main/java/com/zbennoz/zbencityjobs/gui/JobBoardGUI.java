package com.zbennoz.zbencityjobs.gui;

import com.zbennoz.zbencityjobs.model.Job;
import com.zbennoz.zbencityjobs.model.JobStatus;
import com.zbennoz.zbencityjobs.service.JobService;
import com.zbennoz.zbencityjobs.service.CoinService;
import com.zbennoz.zbencityjobs.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.*;

public class JobBoardGUI {
    private final JobService jobService;
    private final MessageService messages;
    private final CoinService coinService;
    private final int size;
    private final Map<UUID, Map<Integer, Integer>> openJobIndex = new HashMap<>();

    public JobBoardGUI(JobService jobService, MessageService messages, CoinService coinService, int size) {
        this.jobService = jobService;
        this.messages = messages;
        this.coinService = coinService;
        this.size = size;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, size, "Jobs");
        Map<Integer, Integer> slots = new HashMap<>();
        int slot = 0;
        for (Job job : jobService.getJobs()) {
            if (slot >= size) break;
            if (job.getStatus() == JobStatus.CANCELLED || job.getStatus() == JobStatus.COMPLETED) continue;
            ItemStack stack = new ItemStack(Material.BOOK);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName("§bJob #" + job.getId());
            List<String> lore = new ArrayList<>();
            lore.add("§7Typ: §f" + job.getType());
            lore.add("§7Lohn: §a" + coinService.formatAmount(job.getReward()));
            lore.add("§7Status: §f" + job.getStatus());
            lore.add("§7Beschreibung: §f" + job.getDescription());
            if (job.isEscrow()) lore.add("§aEscrow gesichert");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
            inventory.setItem(slot, stack);
            slots.put(slot, job.getId());
            slot++;
        }
        openJobIndex.put(player.getUniqueId(), slots);
        player.openInventory(inventory);
    }

    public Optional<Integer> resolveJob(Player player, int slot) {
        Map<Integer, Integer> slots = openJobIndex.get(player.getUniqueId());
        if (slots == null) return Optional.empty();
        return Optional.ofNullable(slots.get(slot));
    }
}
