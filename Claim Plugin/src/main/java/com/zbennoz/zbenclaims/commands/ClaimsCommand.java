package com.zbennoz.zbenclaims.commands;

import com.zbennoz.zbenclaims.Claim;
import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class ClaimsCommand extends BaseCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 10;

    public ClaimsCommand(ZBenClaimsPlugin plugin) { super(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!ensurePlayer(sender)) return true;
        if (!ensurePerm(sender, "zbenclaims.use")) return true;

        Player p = (Player) sender;
        int page = 1;
        if (args.length >= 1) {
            try { page = Math.max(1, Integer.parseInt(args[0])); } catch (NumberFormatException ignored) {}
        }

        List<Claim> claims = plugin.getClaimService().listClaims(p.getUniqueId());
        int pages = Math.max(1, (int) Math.ceil(claims.size() / (double) PAGE_SIZE));
        page = Math.min(page, pages);

        plugin.getMessages().send(p, "listHeader", Map.of("page", String.valueOf(page), "pages", String.valueOf(pages)));

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(claims.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            Claim c = claims.get(i);
            plugin.getMessages().send(p, "listEntry", Map.of(
                    "world", c.worldName(),
                    "x", String.valueOf(c.chunkX()),
                    "z", String.valueOf(c.chunkZ())
            ));
        }
        return true;
    }
}
