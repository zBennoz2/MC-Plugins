package com.zbennoz.zbencityjobs.commands;

import com.zbennoz.zbencityjobs.gui.JobBoardGUI;
import com.zbennoz.zbencityjobs.model.Job;
import com.zbennoz.zbencityjobs.model.JobCreationSession;
import com.zbennoz.zbencityjobs.model.JobStatus;
import com.zbennoz.zbencityjobs.service.JobCreationManager;
import com.zbennoz.zbencityjobs.service.JobService;
import com.zbennoz.zbencityjobs.service.CoinService;
import com.zbennoz.zbencityjobs.util.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class JobsCommand implements CommandExecutor {
    private final JobBoardGUI jobBoardGUI;
    private final JobService jobService;
    private final JobCreationManager creationManager;
    private final MessageService messages;
    private final CoinService coinService;

    public JobsCommand(JobBoardGUI jobBoardGUI, JobService jobService, JobCreationManager creationManager, MessageService messages, CoinService coinService) {
        this.jobBoardGUI = jobBoardGUI;
        this.jobService = jobService;
        this.creationManager = creationManager;
        this.messages = messages;
        this.coinService = coinService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only");
            return true;
        }

        if (args.length == 0) {
            jobBoardGUI.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                creationManager.start(player);
                player.sendMessage(messages.get("info.wizard.type"));
            }
            case "list" -> jobService.getJobs().forEach(job -> player.sendMessage(format(job)));
            case "take" -> {
                if (args.length < 2) return false;
                int id = Integer.parseInt(args[1]);
                if (jobService.takeJob(id, player)) {
                    player.sendMessage(messages.get("info.job-taken", Map.of("id", String.valueOf(id))));
                } else {
                    player.sendMessage(messages.get("errors.job-taken", Map.of("id", String.valueOf(id))));
                }
            }
            case "submit" -> {
                if (args.length < 2) return false;
                int id = Integer.parseInt(args[1]);
                if (jobService.submitJob(id, player)) {
                    player.sendMessage(messages.get("info.job-submitted"));
                } else {
                    player.sendMessage(messages.get("errors.job-cancelled"));
                }
            }
            case "cancel" -> {
                if (args.length < 2) return false;
                int id = Integer.parseInt(args[1]);
                if (jobService.cancelJob(id, player)) {
                    player.sendMessage(messages.get("info.job-cancelled"));
                } else {
                    player.sendMessage(messages.get("errors.not-owner"));
                }
            }
            default -> jobBoardGUI.open(player);
        }
        return true;
    }

    private String format(Job job) {
        return "#" + job.getId() + " " + job.getType() + " " + job.getStatus() + " " + coinService.formatAmount(job.getReward());
    }
}
