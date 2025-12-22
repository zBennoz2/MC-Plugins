package com.zbennoz.zbencityjobs.commands;

import com.zbennoz.zbencityjobs.model.Company;
import com.zbennoz.zbencityjobs.model.CompanyRole;
import com.zbennoz.zbencityjobs.service.CompanyService;
import com.zbennoz.zbencityjobs.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class CompanyCommand implements CommandExecutor {
    private final CompanyService companyService;
    private final MessageService messages;

    public CompanyCommand(CompanyService companyService, MessageService messages) {
        this.companyService = companyService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
            String name = args[1];
            companyService.createCompany(player, name).ifPresentOrElse(company ->
                    player.sendMessage(messages.get("info.company-created", Map.of("name", company.getName()))),
                    () -> player.sendMessage(messages.get("errors.transaction-failed")));
            return true;
        }

        Optional<Company> owned = companyService.findOwnedCompany(player);
        if (owned.isEmpty()) {
            player.sendMessage(messages.get("errors.no-permission"));
            return true;
        }
        Company company = owned.get();

        if (args.length >= 2 && args[0].equalsIgnoreCase("invite")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (companyService.invite(company, player, target.getUniqueId())) {
                player.sendMessage(messages.get("info.invited", Map.of("player", target.getName())));
            }
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("kick")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (companyService.kick(company, player, target.getUniqueId())) {
                player.sendMessage(messages.get("info.kicked", Map.of("player", target.getName())));
            }
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("role")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            CompanyRole role = CompanyRole.fromString(args[2]);
            if (role == null) return false;
            if (companyService.setRole(company, player, target.getUniqueId(), role)) {
                player.sendMessage(messages.get("info.invited", Map.of("player", target.getName())));
            }
            return true;
        }

        return false;
    }
}
