package com.zbennoz.zbenclaims.commands;

import com.zbennoz.zbenclaims.ClaimResult;
import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnclaimCommand extends BaseCommand implements CommandExecutor {

    public UnclaimCommand(ZBenClaimsPlugin plugin) { super(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!ensurePlayer(sender)) return true;
        if (!ensurePerm(sender, "zbenclaims.use")) return true;

        Player p = (Player) sender;
        ClaimResult res = plugin.getClaimService().unclaimChunk(p.getLocation().getChunk(), p.getUniqueId());

        switch (res.type()) {
            case SUCCESS -> plugin.getMessages().send(p, "unclaimSuccess");
            case NOT_CLAIMED -> plugin.getMessages().send(p, "notClaimed");
            case NOT_OWNER -> plugin.getMessages().send(p, "unclaimNotOwner");
            default -> plugin.getMessages().sendRaw(p, "&cKonnte nicht unclaimen.");
        }
        return true;
    }
}
