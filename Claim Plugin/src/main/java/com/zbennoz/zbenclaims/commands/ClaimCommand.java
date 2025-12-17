package com.zbennoz.zbenclaims.commands;

import com.zbennoz.zbenclaims.ClaimResult;
import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class ClaimCommand extends BaseCommand implements CommandExecutor {

    public ClaimCommand(ZBenClaimsPlugin plugin) { super(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!ensurePlayer(sender)) return true;
        if (!ensurePerm(sender, "zbenclaims.use")) return true;

        Player p = (Player) sender;
        ClaimResult res = plugin.getClaimService().claimChunk(p.getLocation().getChunk(), p.getUniqueId());

        switch (res.type()) {
            case SUCCESS -> plugin.getMessages().send(p, "claimSuccess", Map.of(
                    "world", p.getWorld().getName(),
                    "x", String.valueOf(p.getLocation().getChunk().getX()),
                    "z", String.valueOf(p.getLocation().getChunk().getZ())
            ));
            case ALREADY_CLAIMED -> plugin.getMessages().send(p, "claimAlreadyClaimed", Map.of(
                    "owner", plugin.getClaimService().ownerName(res.claim().ownerUuid())
            ));
            case LIMIT_REACHED -> plugin.getMessages().send(p, "claimLimitReached", Map.of(
                    "limit", String.valueOf(res.limit())
            ));
            default -> plugin.getMessages().sendRaw(p, "&cKonnte nicht claimen.");
        }
        return true;
    }
}
