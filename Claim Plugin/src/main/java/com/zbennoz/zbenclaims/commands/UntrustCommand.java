package com.zbennoz.zbenclaims.commands;

import com.zbennoz.zbenclaims.TrustResult;
import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UntrustCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    public UntrustCommand(ZBenClaimsPlugin plugin) { super(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!ensurePlayer(sender)) return true;
        if (!ensurePerm(sender, "zbenclaims.use")) return true;
        if (args.length < 1) return false;

        Player p = (Player) sender;
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null || target.getUniqueId() == null) {
            plugin.getMessages().send(p, "unknownPlayer");
            return true;
        }

        TrustResult res = plugin.getClaimService().untrust(p.getLocation().getChunk(), p.getUniqueId(), target.getUniqueId());
        switch (res.type()) {
            case SUCCESS -> plugin.getMessages().send(p, "untrustSuccess", Map.of("player", target.getName() != null ? target.getName() : args[0]));
            case NOT_CLAIMED -> plugin.getMessages().send(p, "notClaimed");
            case NOT_OWNER -> plugin.getMessages().send(p, "unclaimNotOwner");
            case NOT_TRUSTED -> plugin.getMessages().send(p, "notTrusted", Map.of("player", args[0]));
            case CANNOT_TRUST_OWNER -> plugin.getMessages().send(p, "cannotTrustOwner");
            default -> plugin.getMessages().sendRaw(p, "&cKonnte nicht untrusten.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
