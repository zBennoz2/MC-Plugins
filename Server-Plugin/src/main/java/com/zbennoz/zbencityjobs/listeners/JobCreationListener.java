package com.zbennoz.zbencityjobs.listeners;

import com.zbennoz.zbencityjobs.model.JobCreationSession;
import com.zbennoz.zbencityjobs.model.JobType;
import com.zbennoz.zbencityjobs.service.JobCreationManager;
import com.zbennoz.zbencityjobs.service.JobService;
import com.zbennoz.zbencityjobs.service.CoinService;
import com.zbennoz.zbencityjobs.util.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class JobCreationListener implements Listener {
    private final JobCreationManager manager;
    private final JobService jobService;
    private final MessageService messages;
    private final CoinService coinService;

    public JobCreationListener(JobCreationManager manager, JobService jobService, MessageService messages, CoinService coinService) {
        this.manager = manager;
        this.jobService = jobService;
        this.messages = messages;
        this.coinService = coinService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        manager.get(player).ifPresent(session -> {
            event.setCancelled(true);
            handle(player, session, event.getMessage());
        });
    }

    private void handle(Player player, JobCreationSession session, String message) {
        switch (session.getStage()) {
            case TYPE -> {
                JobType type = JobType.fromString(message);
                if (type == null) {
                    player.sendMessage(messages.get("info.wizard.type"));
                    return;
                }
                session.setType(type);
                session.setStage(JobCreationSession.Stage.DESCRIPTION);
                player.sendMessage(messages.get("info.wizard.description"));
            }
            case DESCRIPTION -> {
                session.setDescription(message);
                session.setStage(JobCreationSession.Stage.REWARD);
                player.sendMessage(messages.get("info.wizard.reward", Map.of(
                        "currency", coinService.getCurrencyName()
                )));
            }
            case REWARD -> {
                long reward = parseReward(message);
                if (reward <= 0) {
                    player.sendMessage(messages.get("errors.invalid-amount", Map.of(
                            "max", String.valueOf(coinService.getMaxAmount())
                    )));
                    return;
                }
                session.setReward(reward);
                if (session.getType() == JobType.DELIVERY) {
                    session.setStage(JobCreationSession.Stage.DELIVERY_ITEM);
                    player.sendMessage(messages.get("info.wizard.delivery-item"));
                } else {
                    finalizeJob(player, session);
                }
            }
            case DELIVERY_ITEM -> {
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()) {
                    player.sendMessage(messages.get("info.wizard.delivery-item"));
                    return;
                }
                session.setDeliveryItem(inHand.clone());
                finalizeJob(player, session);
            }
        }
    }

    private void finalizeJob(Player player, JobCreationSession session) {
        jobService.createJob(player, session).ifPresentOrElse(job -> {
            player.sendMessage(messages.get("info.job-created", Map.of("id", String.valueOf(job.getId()))));
        }, () -> player.sendMessage(messages.get("errors.escrow-required")));
        manager.clear(player);
    }

    private long parseReward(String input) {
        try {
            long value = Long.parseLong(input);
            if (value <= 0) return -1;
            return Math.min(value, coinService.getMaxAmount());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
